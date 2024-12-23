package com.example.thecookapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

// Data class for Recipe
data class Recipe(
    val user_id: Int,
    val post_id: Int, // Incremented based on the number of posts
    val title: String,
    val description: String,
    val ingredients: Map<String, String>,  // Ingredient as key-value pair (ingredient, amount)
    val instructions: List<String>, //
    val image_url: String,
    val difficulty: String,
    val servings: Int,
    val time: String, // Timestamp when the recipe was created
    )

// Retrofit API Interface
interface RecipeApi {
    @POST("/add_recipe")
    fun addRecipe(@Body recipe: Recipe): Call<Map<String, Any>>

    @GET("get_recipes")
    fun getRecipes(): Call<List<Recipe>>
}

data class ResponseMessage(
    val success: Boolean,
    val message: String
)