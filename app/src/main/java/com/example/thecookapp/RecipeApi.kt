package com.example.thecookapp

import android.os.Parcelable
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import kotlinx.parcelize.Parcelize

// Data class for Recipe
@Parcelize
data class Recipe(
    val user_id: String,
    val post_id: Int, // Incremented based on the number of posts
    val title: String,
    val description: String,
    val ingredients: Map<String, String>,  // Ingredient as key-value pair (ingredient, amount)
    val instructions: List<String>,
    val image_url: String,
    val difficulty: String,
    val servings: String,
    val time_to_do: String, // Timestamp when the recipe was created
    val created_at: String
    )

// Retrofit API Interface
interface RecipeApi {
    @POST("/add_recipe")
    fun addRecipe(@Body recipe: Recipe): Call<Map<String, Any>>

    @GET("get_post/{user_id}")
    fun get_post(@Path("user_id") userId: String): Call<List<Recipe>>

    @GET("/get_post_count/{user_id}")
    fun getPostCount(@Path("user_id") userId: String): Call<Int>

    @GET("/health")
    suspend fun healthCheck(): Response<ResponseMessage>

    @Multipart
    @POST("/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<JsonObject>
}

data class ResponseMessage(
    val success: Boolean,
    val message: String
)
