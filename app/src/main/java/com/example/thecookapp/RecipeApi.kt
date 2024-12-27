package com.example.thecookapp

import retrofit2.Call
import retrofit2.http.*

// Data class for Recipe
data class Recipe(
    val user_id: String,
    val post_id: Int, // Incremented based on the number of posts
    val title: String,
    val description: String,
    val ingredients: Map<String, String>,  // Ingredient as key-value pair (ingredient, amount)
    val instructions: List<String>, //
    val image_url: String,
    val difficulty: String,
    val servings: String,
    val time: String, // Timestamp when the recipe was created
    )

// Retrofit API Interface
interface RecipeApi {
    @POST("/add_recipe")
    fun addRecipe(@Body recipe: Recipe): Call<Map<String, Any>>

    @GET("get_recipes")
    fun getRecipes(): Call<List<Recipe>>

    @GET("/get_post_count/{user_id}")
    fun  getPostCount(@Path("user_id") userId: String): Call<Int>
}

data class ResponseMessage(
    val success: Boolean,
    val message: String
)