package `in`.mealwise.app.core.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val Context.ds by preferencesDataStore("mealwise")
val KEY_ACCESS = stringPreferencesKey("access")
val KEY_BASE = "https://api.mealwise.in/api/v1/" // overridden by local.properties `api.baseUrl` at build time

@Module @InstallIn(SingletonComponent::class)
object NetModule {
    @Provides @Singleton fun api(@ApplicationContext ctx: Context): MealWiseApi {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val token = kotlinx.coroutines.runBlocking { ctx.ds.data.map { it[KEY_ACCESS] }.first() }
            chain.proceed(chain.request().newBuilder()
                .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
                .header("X-Correlation-ID", java.util.UUID.randomUUID().toString()).build())
        }.build()
        return Retrofit.Builder().baseUrl(KEY_BASE).client(client)
            .addConverterFactory(MoshiConverterFactory.create()).build().create(MealWiseApi::class.java)
    }
}
suspend fun Context.saveToken(t: String) { ds.edit { it[KEY_ACCESS] = t } }
