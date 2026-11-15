package com.logmeal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.HashMap;

public class ConfirmFoodQuantity8 {

    public static void main(String[] args) throws IOException, InterruptedException {
        
        /**
         * ####################################################################################
         * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
        String BASE_URL = "http://localhost:8080/v2";
        String TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
        int TEST_IMAGE_ID = <imageId-from-previous-step>;
        /**
         * ####################################################################################
         */

        // CODE TO BE COPIED TO README.COM

        // === Step 1: Configure inputs & endpoint ===
        String BASE_URL = "https://api.logmeal.com/v2";
        String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                   // APIUser token
        Object IMAGE_ID = "<imageId-from-previous-step>";                 // From segmentation
        String url = BASE_URL + "/nutrition/confirm/quantity";           // confirm food quantities

        // For local testing, ensure IMAGE_ID is the correct type
        if (IMAGE_ID.toString().contains("previous-step")) { IMAGE_ID = 2072703; }

        HttpClient client = HttpClient.newBuilder().build();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // === Step 2: Build edits & POST ===
        // Example: set/adjust per-item serving sizes (by region)
        // - Use food_item_positions returned by the previous recipe endpoint
        // - Values are quantities in grams (or ml for liquids)
        Map<String, Double> edits = new HashMap<>();
        edits.put("1", 50.0);   // steamed broccoli
        edits.put("2", 180.2);  // schnitzel
        edits.put("3", 30.0);   // mayonnaise
        edits.put("4", 120.0);  // baked potatoes

        Map<String, Object> payload = Map.of(
            "imageId", IMAGE_ID,
            "quantity", edits
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_USER_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
        if (resp.statusCode() != 200) {
            System.err.println("HTTP Error: " + resp.statusCode());
            System.err.println(resp.body());
            System.exit(1);
        }

        System.out.println("Food quantities updated successfully");
    }
}