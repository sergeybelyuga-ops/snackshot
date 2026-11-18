import java.io.File
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit

fun main() {
    /**
     * ####################################################################################
     * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
     
    val TEST_BASE_URL = "http://localhost:8080/v2"
    val TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"
    val TEST_IMAGE_PATH = "<path/to/multiple_items_meal.jpg"
    /**
     * ####################################################################################
     */

    // CODE TO BE COPIED TO README.COM

    // === Step 1: Configure inputs & endpoint ===
    var BASE_URL = "https://api.logmeal.com/v2"
    var API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>"                     // APIUser token
    var IMAGE_PATH = "<path/to/multiple_items_meal.jpg>"            // Local JPEG path

    // Local testing override
    if (IMAGE_PATH.contains("<path/to/") || IMAGE_PATH.contains("multiple_items_meal.jpg")) {
        BASE_URL = TEST_BASE_URL
        API_USER_TOKEN = TEST_API_USER_TOKEN
        IMAGE_PATH = TEST_IMAGE_PATH
    }

    val url = "$BASE_URL/image/segmentation/complete"
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // === Step 2: POST image & get JSON ===
    val file = File(IMAGE_PATH)
    if (!file.exists()) {
        println("Error: Image not found at ${file.absolutePath}")
        return
    }

    val mediaType = "image/jpeg".toMediaTypeOrNull()
    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("image", file.name, file.asRequestBody(mediaType))
        .build()

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $API_USER_TOKEN")
        .post(requestBody)
        .build()

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
        println("HTTP Error: ${response.code} - ${response.body?.string()}")
        response.close()
        return
    }

    val responseData = response.body?.string() ?: ""
    response.close()
    
    val data = JsonParser.parseString(responseData).asJsonObject

    // === Step 3: Parse & print summary ===
    println("imageId: ${data.get("imageId")}")
    if (data.has("occasion") && !data.get("occasion").isJsonNull) {
        println("occasion: ${data.get("occasion").asString}")
    }
    
    if (data.has("foodType") && !data.get("foodType").isJsonNull) {
        val ft = data.getAsJsonObject("foodType")
        println("foodType: ${ft.get("name").asString} (id ${ft.get("id").asInt})")
    }

    // Iterate each segmented food item / region
    val segments = if (data.has("segmentation_results")) data.getAsJsonArray("segmentation_results") else com.google.gson.JsonArray()
    for (element in segments) {
        val item = element.asJsonObject
        val pos = item.get("food_item_position").asInt
        val bb = item.getAsJsonObject("contained_bbox") ?: JsonObject()
        val center = item.getAsJsonObject("center") ?: JsonObject()
        val grams = item.get("serving_size")
        val recs = if (item.has("recognition_results")) item.getAsJsonArray("recognition_results") else com.google.gson.JsonArray()
        val top = if (recs.size() > 0) recs[0].asJsonObject else null

        var line = "[#$pos] bbox={'x': ${bb.get("x")}, 'y': ${bb.get("y")}, 'w': ${bb.get("w")}, 'h': ${bb.get("h")}} center={'x': ${center.get("x")}, 'y': ${center.get("y")}}"
        if (grams != null && !grams.isJsonNull) {
            line += " grams=${grams.asDouble}"
        }
        println(line)

        if (top != null) {
            val prob = Math.round(top.get("prob").asDouble * 1000.0) / 1000.0
            println("  top dish: ${top.get("name").asString} (id ${top.get("id").asInt}, prob $prob)")

            // extra: print top-5 candidates for this region
            val cands = mutableListOf<String>()
            val limit = if (recs.size() < 5) recs.size() else 5
            for (i in 0 until limit) {
                val r = recs[i].asJsonObject
                val p = Math.round(r.get("prob").asDouble * 1000.0) / 1000.0
                cands.add("${r.get("name").asString} (id ${r.get("id").asInt}, prob $p)")
            }
            println("  top-5 candidates: " + cands.joinToString("; "))
        }
    }
}