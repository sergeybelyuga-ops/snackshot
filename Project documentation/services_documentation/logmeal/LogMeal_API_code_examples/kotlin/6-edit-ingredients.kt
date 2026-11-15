import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit

fun main() {

    // ####################################################################################
    // # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
    // val TEST_BASE_URL = "http://localhost:8080/v2"
    // val TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
    // val IMAGE_ID = 2072707 
    // ####################################################################################

    // CODE TO BE COPIED TO README.COM

    // === Step 1: Configure inputs & endpoint ===
    val baseUrl = "https://api.logmeal.com/v2"
    val apiUserToken = "<YOUR_APIUSER_TOKEN>"                   // APIUser token
    val imageId = "<imageId-from-previous-step>"                 // From segmentation
    
    val url = "$baseUrl/nutrition/confirm/ingredients"
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    // === Step 2: Build edits & POST ===
    // Example: adjust quantities and swap ingredients per detected item
    // - Use food_item_positions returned by the previous recipe endpoint
    // - 'ingredientAmount' is in grams (or ml for liquids)
    
    val ingredientsPerItem = JsonObject()

    // Edits for food_item_position 1
    val item1Edits = JsonArray().apply {
        add(JsonObject().apply {
            addProperty("ingredientId", 1478)
            addProperty("ingredientAmount", 85.0)
        })
        add(JsonObject().apply {
            addProperty("ingredientId", 61)
            addProperty("ingredientAmount", 12.0)
        })
        add(JsonObject().apply {
            addProperty("ingredientId", 562)
            addProperty("ingredientAmount", 12.0)
        })
    }
    ingredientsPerItem.add("1", item1Edits)

    // Edits for food_item_position 2
    val item2Edits = JsonArray().apply {
        add(JsonObject().apply {
            addProperty("ingredientId", 204)
            addProperty("ingredientAmount", 220.0)
        })
        add(JsonObject().apply {
            addProperty("ingredientId", 562)
            addProperty("ingredientAmount", 8.0)
        })
        add(JsonObject().apply {
            addProperty("ingredientId", 45)
            addProperty("ingredientAmount", 1.2)
        })
    }
    ingredientsPerItem.add("2", item2Edits)

    val payload = JsonObject().apply {
        addProperty("imageId", imageId)
        add("ingredients_per_item", ingredientsPerItem)
    }

    val mediaType = "application/json".toMediaTypeOrNull()
    val requestBody = payload.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $apiUserToken")
        .post(requestBody)
        .build()

    val response = client.newCall(request).execute()
    val responseData = response.body?.string() ?: ""
    
    if (!response.isSuccessful) {
        println("HTTP Error: ${response.code} - $responseData")
        response.close()
        return
    }
    response.close()

    println("Ingredients successfully updated for image $imageId")
    // Pretty print the response
    val prettyJson = Gson().newBuilder().setPrettyPrinting().create()
    println(prettyJson.toJson(JsonParser.parseString(responseData)))
}