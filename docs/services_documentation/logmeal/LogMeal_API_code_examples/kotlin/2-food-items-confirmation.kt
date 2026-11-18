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

    // === Step 1: Configure inputs & intended corrections ===
    var baseUrl = "https://api.logmeal.com/v2"
    var apiUserToken = "<YOUR_APIUSER_TOKEN>"
    val imageId = "<imageId-from-previous-step>"                // From segmentation

    // Local testing override
    if (imageId == 0) {
        baseUrl = TEST_BASE_URL
        apiUserToken = TEST_API_USER_TOKEN
        imageId = TEST_IMAGE_ID
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    val confirmedClasses = listOf(110, 1474, "grandma soup")
    val foodItemPositions = listOf(1, 2, "extra_dish_1")
    val sources = listOf("logmeal", "logmeal", "other")

    // === Step 2: Confirm/correct dishes (per segment or extra) ===
    val dishUrl = "$baseUrl/image/confirm/dish"
    
    val payload = JsonObject().apply {
        addProperty("imageId", imageId)
        add("confirmedClass", Gson().toJsonTree(confirmedClasses))
        add("food_item_position", Gson().toJsonTree(foodItemPositions))
        add("source", Gson().toJsonTree(sources))
    }

    val mediaType = "application/json".toMediaTypeOrNull()
    val requestBody = payload.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(dishUrl)
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

    println("Dish confirmation -> OK")
    val prettyJson = Gson().newBuilder().setPrettyPrinting().create()
    val jsonElement = JsonParser.parseString(responseData)
    println(prettyJson.toJson(jsonElement))
}