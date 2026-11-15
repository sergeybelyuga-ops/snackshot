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
    // ####################################################################################

    // CODE TO BE COPIED TO README.COM

    // -----------------------------
    // Step 1 - Prepare configuration & inputs
    // -----------------------------
    val baseUrl = "https://api.logmeal.com/v2"
    val apiUserToken = "<YOUR_APIUSER_TOKEN>"                  // APIUser token
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    // Define preferences (ISO country code + ISO language code)
    val preferences = JsonObject().apply {
        addProperty("country", "ES")    // Spain
        addProperty("language", "spa")   // Spanish
    }

    // -----------------------------
    // Step 2 - Send profile personalization request
    // -----------------------------
    val url = "$baseUrl/profile/modifyUserProfileInfo"
    val mediaType = "application/json".toMediaTypeOrNull()
    val requestBody = preferences.toString().toRequestBody(mediaType)

    val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $apiUserToken")
        .post(requestBody)
        .build()

    // -----------------------------
    // Step 3 - Confirm personalization response
    // -----------------------------
    val response = client.newCall(request).execute()
    val responseData = response.body?.string() ?: ""

    if (!response.isSuccessful) {
        println("Failed to personalize profile (${response.code}): $responseData")
        response.close()
        return
    }
    response.close()

    val profileInfo = JsonParser.parseString(responseData).asJsonObject
    val prettyJson = Gson().newBuilder().setPrettyPrinting().create()
    
    println("Profile updated: ${prettyJson.toJson(profileInfo)}")

    // -----------------------------
    // Step 4 - Note on usage
    // -----------------------------
    // After this call, all recognition endpoints will return labels 
    // in the chosen language and prioritized for the selected country.
}