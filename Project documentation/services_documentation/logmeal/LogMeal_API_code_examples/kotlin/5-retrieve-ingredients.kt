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
    val apiUserToken = "<YOUR_APIUSER_TOKEN>"                  // APIUser token
    val imageId = "<imageId-from-previous-step>"                // From segmentation
    
    val url = "$baseUrl/nutrition/recipe/ingredients"
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    // === Step 2: Request ingredients & quantities ===
    val payload = JsonObject().apply {
        addProperty("imageId", imageId)
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

    val data = JsonParser.parseString(responseData).asJsonObject

    // === Step 3: Parse & print summary ===
    println("imageId: ${if (data.has("imageId")) data.get("imageId").asString else imageId}")
    
    if (data.has("serving_size") && !data.get("serving_size").isJsonNull) {
        println("meal_serving_size: ${data.get("serving_size").asDouble} g")
    }

    // Prefer per-item recipes if present
    val items = if (data.has("recipe_per_item")) data.getAsJsonArray("recipe_per_item") else JsonArray()

    if (items.size() > 0) {
        for (element in items) {
            val it = element.asJsonObject
            val pos = it.get("food_item_position")?.asInt
            val dname = it.get("name")?.asString
            val ss = it.get("serving_size")?.asDouble
            val unit = it.get("unit")?.asString ?: "g"
            
            println("[#$pos] dish=$dname serving_size=$ss $unit")

            // Ingredients for this dish
            val recipe = if (it.has("recipe")) it.getAsJsonArray("recipe") else JsonArray()
            for (ingElement in recipe) {
                val ing = ingElement.asJsonObject
                printIngredient(ing)
            }
        }
    } else {
        // Fallback 1: show flat global recipe
        val globalRecipe = if (data.has("recipe")) data.getAsJsonArray("recipe") else JsonArray()
        if (globalRecipe.size() > 0) {
            println("Global recipe:")
            for (ingElement in globalRecipe) {
                val ing = ingElement.asJsonObject
                printIngredient(ing)
            }
        } else {
            // Fallback 2: generic fields
            val skipKeys = listOf("imageId", "recipe_per_item", "recipe", "dish_id", "foodName", "is_combo", "serving_size", "source")
            for (entry in data.entrySet()) {
                if (entry.key !in skipKeys) {
                    println("${entry.key}: ${entry.value}")
                }
            }
        }
    }
}

/**
 * Helper function to parse and print ingredient details in a consistent format
 */
fun printIngredient(ing: JsonObject) {
    val iid = ing.get("id")?.asInt
    val name = ing.get("name")?.asString
    val weight = ing.get("weight")?.asDouble
    val unitI = if (ing.has("unit") && !ing.get("unit").isJsonNull) ing.get("unit").asString else "g"
    
    val meas = if (ing.has("measure") && ing.get("measure").isJsonObject) ing.getAsJsonObject("measure") else JsonObject()
    val metric = if (meas.has("metric") && meas.get("metric").isJsonObject) meas.getAsJsonObject("metric") else null
    val preferred = if (meas.has("preferred") && meas.get("preferred").isJsonObject) meas.getAsJsonObject("preferred") else null

    // Metric (canonical) amount
    val mQty = metric?.get("quantity")?.asDouble ?: weight
    val mShort = metric?.get("short")?.asString ?: unitI

    var line = "  - $name (id $iid) -> $mQty $mShort"

    // Preferred (human-friendly) measure if available
    if (preferred != null && preferred.has("quantity") && !preferred.get("quantity").isJsonNull) {
        val pQty = preferred.get("quantity").asDouble
        val pName = preferred.get("name")?.asString ?: ""
        line += " (~$pQty $pName)"
    }
    println(line)
}