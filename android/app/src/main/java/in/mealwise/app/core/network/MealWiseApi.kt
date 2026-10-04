package `in`.mealwise.app.core.network

import com.squareup.moshi.JsonClass
import retrofit2.http.*

@JsonClass(generateAdapter = true) data class Page<T>(val content: List<T>, val page: Int, val totalPages: Int)
@JsonClass(generateAdapter = true) data class MealDto(val id: String, val name: String, val diet: String, val prepMinutes: Int, val cookMinutes: Int, val difficulty: String?, val imageUrl: String?)
@JsonClass(generateAdapter = true) data class Availability(val available: List<Map<String, Any?>>, val low: List<Map<String, Any?>>, val missing: List<Map<String, Any?>>)

interface MealWiseApi {
    @GET("meals/search") suspend fun search(@Query("q") q: String?, @Query("diet") diet: String?, @Query("maxMinutes") m: Int?, @Query("page") p: Int = 0): Page<MealDto>
    @GET("meals/{id}") suspend fun meal(@Path("id") id: String, @Query("servings") s: Int = 2): Map<String, Any?>
    @POST("meals/{id}/availability") suspend fun availability(@Path("id") id: String, @Body b: Map<String, Any?>): Availability
    @GET("users/me/favourites") suspend fun favourites(): List<Map<String, Any?>>
    @POST("users/me/favourites/{id}") suspend fun addFavourite(@Path("id") id: String): Map<String, Any?>
    @DELETE("users/me/favourites/{id}") suspend fun removeFavourite(@Path("id") id: String): Map<String, Any?>
    @POST("recommendations") suspend fun recommend(@Body b: Map<String, Any?>): Map<String, Any?>
    @GET("inventory") suspend fun inventory(): List<Map<String, Any?>>
    @POST("ai/chat") suspend fun chat(@Body b: Map<String, Any?>): Map<String, Any?>
    @GET("commerce/providers") suspend fun providers(): List<Map<String, Any?>>
    @POST("commerce/handoff") suspend fun handoff(@Body b: Map<String, Any?>): Map<String, Any?>
    @GET("subscriptions/entitlements") suspend fun entitlements(): Map<String, Any?>
    @POST("auth/login") suspend fun login(@Body b: Map<String, String>): Map<String, String>
}
