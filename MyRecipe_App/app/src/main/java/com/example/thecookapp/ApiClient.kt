import android.util.Log
import com.example.thecookapp.RecipeApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://nick99.pythonanywhere.com/"


    val recipeApi: RecipeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecipeApi::class.java)
    }

    // Function to check server connection
    suspend fun checkServerConnection(): Boolean {
        return try {
            val response = recipeApi.healthCheck()
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error connecting to Flask server: ${e.message}")
            false
        }
    }
}
