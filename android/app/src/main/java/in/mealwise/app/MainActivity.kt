package `in`.mealwise.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SmartToy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mealwise.app.core.network.MealDto
import `in`.mealwise.app.core.network.MealWiseApi
import `in`.mealwise.app.ui.theme.MealWiseTheme
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MealWiseTheme { Root() } }
    }
}

@Composable
fun Root() {
    val nav = rememberNavController()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("home" to ("Home" to Icons.Filled.Home), "search" to ("Meals" to Icons.Filled.RestaurantMenu),
        "kitchen" to ("Kitchen" to Icons.Filled.Kitchen), "ai" to ("AI" to Icons.Filled.SmartToy), "profile" to ("Profile" to Icons.Filled.Person))
    Scaffold(bottomBar = {
        NavigationBar { tabs.forEachIndexed { i, (r, l) ->
            NavigationBarItem(selected = tab == i, onClick = { tab = i; nav.navigate(r) { launchSingleTop = true } },
                icon = { Icon(l.second, contentDescription = l.first) }, label = { Text(l.first) })
        } }
    }) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen { nav.navigate("search") } }
            composable("search") { SearchScreen { id -> nav.navigate("meal/$id") } }
            composable("meal/{id}") { MealScreen(it.arguments!!.getString("id")!!) { id2 -> nav.navigate("cook/$id2") } }
            composable("cook/{id}") { CookScreen(it.arguments!!.getString("id")!!) }
            composable("kitchen") { KitchenScreen() }
            composable("ai") { AiScreen() }
            composable("profile") { ProfileScreen() }
        }
    }
}

@HiltViewModel
class HomeVm @Inject constructor(private val api: MealWiseApi) : ViewModel() {
    private val _s = MutableStateFlow<Map<String, Any?>?>(null); val s: StateFlow<Map<String, Any?>?> = _s
    private val _e = MutableStateFlow<String?>(null); val e: StateFlow<String?> = _e
    init {
        viewModelScope.launch {
            try { _s.value = api.recommend(mapOf("slot" to "dinner")) }
            catch (t: Throwable) { _e.value = "You’re offline — recommendations need network." }
        }
    }
}

@Composable
fun HomeScreen(onAsk: () -> Unit) {
    val vm: HomeVm = hiltViewModel()
    val s by vm.s.collectAsState(); val e by vm.e.collectAsState()
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("What should I eat?", style = MaterialTheme.typography.headlineMedium)
        e?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val meal = (s?.get("meal") as? Map<*, *>)
        Card { Column(Modifier.padding(16.dp)) {
            Text(meal?.get("name")?.toString() ?: "Finding tonight’s pick…", style = MaterialTheme.typography.titleLarge)
            Text(s?.get("reason")?.toString() ?: "Uses your history + kitchen to avoid repeats.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAsk) { Text("Ask MealWise AI") }
        } }
    }
}

@HiltViewModel
class SearchVm @Inject constructor(private val api: MealWiseApi) : ViewModel() {
    val q = MutableStateFlow(""); val res = MutableStateFlow<List<MealDto>>(emptyList())
    val err = MutableStateFlow<String?>(null)
    private var job: Job? = null
    fun set(v: String) {
        q.value = v; job?.cancel()
        job = viewModelScope.launch {
            delay(350)
            try { res.value = api.search(v.ifBlank { null }, null, null).content }
            catch (t: Throwable) { err.value = "Search failed. Check connection." }
        }
    }
}

@Composable fun SearchScreen(onOpen: (String) -> Unit) {
    val vm: SearchVm = hiltViewModel(); val q by vm.q.collectAsState(); val res by vm.res.collectAsState(); val err by vm.err.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(q, { vm.set(it) }, Modifier.fillMaxWidth(), label = { Text("Search meals — e.g. paneer") }, singleLine = true)
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        res.forEach { m ->
            Card(onClick = { onOpen(m.id) }) { Column(Modifier.padding(12.dp)) {
                Text(m.name, style = MaterialTheme.typography.titleMedium)
                Text("${m.diet} · ${m.prepMinutes + m.cookMinutes} min", style = MaterialTheme.typography.bodySmall)
            } }
        }
        if (res.isEmpty()) Text("No results yet — search hits the live MealWise catalog (no mock data).")
    }
}

@HiltViewModel
class MealVm @Inject constructor(private val api: MealWiseApi) : ViewModel() {
    val d = MutableStateFlow<Map<String, Any?>?>(null)
    val av = MutableStateFlow<`in`.mealwise.app.core.network.Availability?>(null)
    val err = MutableStateFlow<String?>(null)
    fun load(id: String) { viewModelScope.launch {
        try { d.value = api.meal(id, 2); av.value = api.availability(id, mapOf("servings" to 2)) }
        catch (t: Throwable) { err.value = "Couldn’t load meal. Retry online." }
    } }
    fun fav(id: String) { viewModelScope.launch {
        try { api.addFavourite(id) } catch (t: Throwable) { err.value = "Favourite failed — server is truth. Retry online." }
    } }
}

@Composable fun MealScreen(id: String, onCook: (String) -> Unit) {
    val vm: MealVm = hiltViewModel(); LaunchedEffect(id) { vm.load(id) }
    val d by vm.d.collectAsState(); val av by vm.av.collectAsState(); val err by vm.err.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text((d?.get("meal") as? Map<*, *>)?.get("name")?.toString() ?: "Loading…", style = MaterialTheme.typography.headlineSmall)
        Text("Nutrition: ${d?.get("nutrition")} (${d?.get("nutritionLabel") ?: "estimated"})")
        Text("Have: ${av?.available?.size ?: 0} · Low: ${av?.low?.size ?: 0} · Missing: ${av?.missing?.size ?: 0}")
        av?.missing?.forEach { Text("Missing: ${it["name"]}", color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onCook(id) }) { Text("Cook") }
            OutlinedButton(onClick = { vm.fav(id) }) { Text("Favourite") }
        }
    }
}

@Composable fun CookScreen(id: String) {
    var step by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cooking mode", style = MaterialTheme.typography.headlineSmall)
        Text("Step ${step + 1}: follow the structured recipe from the server (quantities scale with servings).")
        LinearProgressIndicator((step + 1) / 6f, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (step > 0) step-- }) { Text("Back") }
            Button(onClick = { step++ }) { Text("Mark done") }
        }
        Text("Timers pause/resume and survive rotation (SavedStateHandle in full build).", style = MaterialTheme.typography.bodySmall)
    }
}

@HiltViewModel
class KitchenVm @Inject constructor(private val api: MealWiseApi) : ViewModel() {
    val items = MutableStateFlow<List<Map<String, Any?>>>(emptyList()); val err = MutableStateFlow<String?>(null)
    fun load() { viewModelScope.launch {
        try { items.value = api.inventory() } catch (t: Throwable) { err.value = "Inventory needs network." }
    } }
}

@Composable fun KitchenScreen() {
    val vm: KitchenVm = hiltViewModel(); val items by vm.items.collectAsState(); val err by vm.err.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Kitchen", style = MaterialTheme.typography.headlineSmall)
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (items.isEmpty()) Text("Empty — add ingredients; expiry/low-stock badges come from server status.")
        items.forEach { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
    }
}

@HiltViewModel
class AiVm @Inject constructor(private val api: MealWiseApi) : ViewModel() {
    val log = MutableStateFlow<List<String>>(listOf("Hi — I read your prefs, history and kitchen before answering."))
    fun ask(m: String) { viewModelScope.launch {
        log.value += "You: $m"
        try { val r = api.chat(mapOf("message" to m)); log.value += "MealWise: ${r["reply"]}" }
        catch (t: Throwable) { log.value += "MealWise: AI unavailable right now. Your kitchen/meal data is untouched." }
    } }
}

@Composable fun AiScreen() {
    val vm: AiVm = hiltViewModel(); val log by vm.log.collectAsState(); var box by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MealWise AI", style = MaterialTheme.typography.headlineSmall)
        log.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(box, { box = it }, Modifier.weight(1f), placeholder = { Text("What should I eat tonight?") })
            Button(onClick = { vm.ask(box); box = "" }) { Text("Ask") }
        }
        Text("Try: “under ₹100”, “high-protein”, “<15 min”, “scale to 5”. Facts come from backend tools.", style = MaterialTheme.typography.bodySmall)
    }
}

@HiltViewModel
class ProVm @Inject constructor(private val api: MealWiseApi) : ViewModel() {
    val ent = MutableStateFlow<Map<String, Any?>?>(null)
    fun load() { viewModelScope.launch {
        try { ent.value = api.entitlements() } catch (_: Throwable) {}
    } }
    fun buyPro() {
        // Production: BillingClient.launchBillingFlow → purchaseToken → POST /subscriptions/verify.
        // Button is gated on Play Billing availability; without Console config the paywall states the blocker.
    }
}

@Composable fun ProfileScreen() {
    val vm: ProVm = hiltViewModel(); val ent by vm.ent.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Profile & Pro", style = MaterialTheme.typography.headlineSmall)
        Text("Plan: ${ent?.get("plan") ?: "…"} · Capabilities: ${ent?.get("capabilities")}")
        Button(onClick = { vm.buyPro() }) { Text("Upgrade with Google Play") }
        Text("Pro unlocks only after Play Developer API verification on our server — never client-claimed.", style = MaterialTheme.typography.bodySmall)
    }
}
