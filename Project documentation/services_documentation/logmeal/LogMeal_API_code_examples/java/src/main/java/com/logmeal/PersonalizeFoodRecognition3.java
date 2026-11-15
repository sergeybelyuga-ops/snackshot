package com.logmeal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class PersonalizeFoodRecognition3 {

    public static void main(String[] args) throws IOException, InterruptedException {
        
        /**
         * ####################################################################################
         * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
        String BASE_URL = "http://localhost:8080/v2";
        String TEST_API_USER_TOKEN = "<REPLACE_WITH_APIUSER_TOKEN>";
        /**
         * ####################################################################################
         */

        // CODE TO BE COPIED TO README.COM

        // -----------------------------
        // Step 1 - Prepare configuration & inputs
        // -----------------------------
        String BASE_URL = "https://api.logmeal.com/v2";
        String API_USER_TOKEN = "<REPLACE_WITH_APIUSER_TOKEN>";  // APIUser token

        HttpClient client = HttpClient.newBuilder().build();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Define preferences (ISO country code + ISO language code)
        Map<String, String> preferences = Map.of(
            "country", "ES",   // Spain
            "language", "spa"   // Spanish
        );

        // -----------------------------
        // Step 2 - Send profile personalization request
        // -----------------------------
        String url = BASE_URL + "/profile/modifyUserProfileInfo";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_USER_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(preferences)))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        // -----------------------------
        // Step 3 - Confirm personalization response
        // -----------------------------
        if (resp.statusCode() != 200) {
            System.err.println("Failed to personalize profile (" + resp.statusCode() + "): " + resp.body());
            System.exit(1);
        }

        JsonObject profileInfo = gson.fromJson(resp.body(), JsonObject.class);
        System.out.println("Profile updated: " + gson.toJson(profileInfo));

        // -----------------------------
        // Step 4 - Use personalization in subsequent recognition requests
        // -----------------------------
        // After this call, all recognition endpoints (e.g., /image/segmentation/complete)
        // will return dish, ingredients and nutrient labels in the chosen language and
        // prioritized for the selected country.
    }
}