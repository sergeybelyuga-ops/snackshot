import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.JsonObject
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
    
    val url = "$baseUrl/nutrition/confirm/quantity"
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    // === Step 2: Build edits & POST ===
    // Example: set/adjust per-item serving sizes (by region)
    // - Use food_item_positions returned by the previous recipe endpoint
    // - Values are quantities in grams (or ml for liquids)
    
    val quantityEdits = JsonObject().apply {
        addProperty("1", 50.0)   // steamed broccoli
        addProperty("2", 180.2)  // schnitzel
        addProperty("3", 30.0)   // mayonnaise
        addProperty("4", 120.0)  // baked potatoes
    }

    val payload = JsonObject().apply {
        addProperty("imageId", imageId)
        add("quantity", quantityEdits)
    }

    val mediaType = "application/json".toMediaTypeOrNull()
    val requestBody = payload.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $apiUserToken")
        .post(requestBody)
        .build()

    // === Step 3: Execute & Verify ===
    val response = client.newCall(request).execute()
    val responseData = response.body?.string() ?: ""
    
    if (!response.isSuccessful) {
        println("HTTP Error: ${response.code} - $responseData")
        response.close()
        return
    }
    response.close()

    println("Food quantities successfully updated for image $imageId")
    
    // Opcional: Mostrar respuesta formateada
    val prettyJson = Gson().newBuilder().setPrettyPrinting().create()
    println(prettyJson.toJson(JsonParser.parseString(responseData)))
}