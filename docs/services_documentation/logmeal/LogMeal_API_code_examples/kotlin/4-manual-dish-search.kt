import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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

    // === Step 1: Configure inputs ===
    val baseUrl = "https://api.logmeal.com/v2"
    val apiUserToken = "<YOUR_APIUSER_TOKEN>"                  // APIUser token
    val imageId = 2072707                      // obtained from a previous segmentation/complete
    val userQuery = "miso soup"               // what the user typed
    val targetRegion = 2                      // region to correct (food_item_position)
    val language = "eng"                      // language of the dishes search

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    // Example mapping for the WHOLE image confirmation
    val confirmedClasses = mutableListOf<Any?>(110, null) 
    val foodItemPositions = listOf(1, 2)
    val sources = mutableListOf("logmeal", "logmeal")

    // === Step 2: Search the dish catalog (GET /dataset/dishes) ===
    val dsUrl = "$baseUrl/dataset/dishes".toHttpUrlOrNull()!!.newBuilder()
        .addQueryParameter("language", language)
        .build()

    val dsRequest = Request.Builder()
        .url(dsUrl)
        .addHeader("Authorization", "Bearer $apiUserToken")
        .get()
        .build()

    val dsResponse = client.newCall(dsRequest).execute()
    val dsDataRaw = dsResponse.body?.string() ?: ""
    if (!dsResponse.isSuccessful) {
        println("Error fetching catalog: $dsDataRaw")
        return
    }
    dsResponse.close()

    // Flatten dishes logic
    val catalog = JsonParser.parseString(dsDataRaw).asJsonObject
    val allDishes = mutableListOf<JsonObject>()
    
    for (entry in catalog.entrySet()) {
        if (entry.value.isJsonArray) {
            val items = entry.value.asJsonArray
            for (item in items) {
                val obj = item.asJsonObject
                if (obj.has("name") && obj.has("id")) {
                    allDishes.add(obj)
                }
            }
        }
    }

    // Simple case-insensitive substring match
    val query = userQuery.lowercase().trim()
    val hits = allDishes.filter { it.get("name").asString.lowercase().contains(query) }

    if (hits.isNotEmpty()) {
        val picked = hits[0]
        val dishId = picked.get("id").asInt
        
        // Find the index of our target region to update the class
        val idx = foodItemPositions.indexOf(targetRegion)
        if (idx != -1) {
            confirmedClasses[idx] = dishId
        }
    }

    // === Step 3: Confirm/correct dishes ===
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
        println("Dish confirmation Error: $responseData")
        response.close()
        return
    }
    response.close()

    println("Dish confirmation -> OK")
    val prettyJson = Gson().newBuilder().setPrettyPrinting().create()
    println(prettyJson.toJson(JsonParser.parseString(responseData)))
}