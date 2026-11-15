import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import kotlin.math.round

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
    
    val url = "$baseUrl/nutrition/recipe/nutritionalInfo"
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    // === Step 2: Request nutritional info ===
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

    // Nutri-Score global
    if (data.has("image_nutri_score") && data.get("image_nutri_score").isJsonObject) {
        val ns = data.getAsJsonObject("image_nutri_score")
        println("nutri_score: ${ns.get("nutri_score_category")?.asString} (${ns.get("nutri_score_standardized")?.asInt})")
    }

    // Meal-level totals
    if (data.has("nutritional_info") && data.get("nutritional_info").isJsonObject) {
        val total = data.getAsJsonObject("nutritional_info")
        
        val cal = if (total.has("calories")) total.get("calories").asDouble else null
        if (cal != null) {
            println("calories_total: ${roundValue(cal)} kcal")
        }

        // Daily Intake Reference (DIR) summary
        if (total.has("dailyIntakeReference") && total.get("dailyIntakeReference").isJsonObject) {
            val dirf = total.getAsJsonObject("dailyIntakeReference")
            println("Daily Intake Reference (meal):")
            val keys = listOf("ENERC_KCAL", "PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR", "FASAT", "NA")
            for (k in keys) {
                if (dirf.has(k)) {
                    val v = dirf.getAsJsonObject(k)
                    val label = if (v.has("label")) v.get("label").asString else k
                    val level = v.get("level")?.asString
                    val pct = if (v.has("percent")) roundValue(v.get("percent").asDouble, 1) else null
                    println("  $label: $pct% ($level)")
                }
            }
        }

        // Key nutrients (totals)
        if (total.has("totalNutrients") && total.get("totalNutrients").isJsonObject) {
            val tn = total.getAsJsonObject("totalNutrients")
            val keyCodes = listOf("PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR")
            val formattedNutrients = keyCodes.mapNotNull { formatNutrient(tn.getAsJsonObject(it)) }
            println("Nutrients (meal): ${formattedNutrients.joinToString("; ")}")
        }
    }

    // Per-item nutritional info
    val nameMap = mutableMapOf<Int, String>()
    if (data.has("ids") && data.has("foodName")) {
        val idsArr = data.getAsJsonArray("ids")
        val namesArr = data.getAsJsonArray("foodName")
        for (i in 0 until idsArr.size()) {
            nameMap[idsArr[i].asInt] = namesArr[i].asString
        }
    }

    val items = if (data.has("nutritional_info_per_item")) data.getAsJsonArray("nutritional_info_per_item") else null
    items?.forEach { element ->
        val it = element.asJsonObject
        val pos = it.get("food_item_position")?.asInt
        val iid = it.get("id")?.asInt ?: -1
        val dname = nameMap[iid] ?: "Unknown"
        val ss = it.get("serving_size")?.asDouble
        println("[#$pos] dish=$dname serving_size=$ss g")

        if (it.has("nutri_score") && it.get("nutri_score").isJsonObject) {
            val ns = it.getAsJsonObject("nutri_score")
            println("  nutri_score: ${ns.get("nutri_score_category")?.asString} (${ns.get("nutri_score_standardized")?.asInt})")
        }

        val ni = it.getAsJsonObject("nutritional_info")
        val calI = if (ni.has("calories")) ni.get("calories").asDouble else null
        if (calI != null) println("  calories: ${roundValue(calI)} kcal")

        val tnI = ni.getAsJsonObject("totalNutrients")
        val keyCodes = listOf("PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR")
        val formattedI = keyCodes.mapNotNull { formatNutrient(tnI.getAsJsonObject(it)) }
        println("  nutrients: ${formattedI.joinToString("; ")}")
        
        val dirI = ni.getAsJsonObject("dailyIntakeReference")
        val outDir = mutableListOf<String>()
        val dirKeys = listOf("ENERC_KCAL", "PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR", "FASAT", "NA")
        for (k in dirKeys) {
            if (dirI.has(k)) {
                val v = dirI.getAsJsonObject(k)
                if (v.has("percent")) {
                    outDir.add("${v.get("label").asString}: ${roundValue(v.get("percent").asDouble, 1)}%")
                }
            }
        }
        println("  daily intake: ${outDir.joinToString("; ")}")
    }
}

fun formatNutrient(n: JsonObject?): String? {
    if (n == null || n.isJsonNull) return null
    val q = if (n.has("quantity")) n.get("quantity").asDouble else return null
    val u = if (n.has("unit")) n.get("unit").asString else return null
    val lab = if (n.has("label")) n.get("label").asString else return null
    return "$lab: ${roundValue(q)} $u"
}

fun roundValue(value: Double, decimals: Int = 2): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(value * multiplier) / multiplier
}