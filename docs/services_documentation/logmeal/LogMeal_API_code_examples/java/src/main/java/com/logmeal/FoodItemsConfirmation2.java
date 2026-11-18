package com.logmeal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class FoodItemsConfirmation2 {

    public static void main(String[] args) throws IOException, InterruptedException {
        
        /**
         * ####################################################################################
         * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
        String BASE_URL = "http://localhost:8080/v2";
        String TEST_API_USER_TOKEN = "TEST_API_USER_TOKEN";   // APIUser token (Bearer)
        int TEST_IMAGE_ID = 2072703; // obtained from a previous food intake insertion
        /**
         * ####################################################################################
         */

        // CODE TO BE COPIED TO README.COM

        // === Step 1: Configure inputs & intended corrections ===
        String BASE_URL = "https://api.logmeal.com/v2";
        String API_USER_TOKEN = "TEST_API_USER_TOKEN";   // APIUser token (Bearer)
        int IMAGE_ID = "TEST_IMAGE_ID";                  // obtained from a previous food intake insertion

        // Example mapping from food intake insertion results you want to confirm/correct:
        // IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
        // - Region #1 was recognized as "donuts" (id 110) and that's correct.
        // - Region #2 should be corrected to "mushroom cream" (id 1474).
        // - Add an extra dish not in any segment: free text "grandma soup".
        List<Object> confirmed_classes = List.of(16, 1480, "grandma soup"); // ids or free-text
        List<Object> food_item_positions = List.of(1, 2, "extra_dish_1");    // int for segments, string for extra dish
        List<String> sources = List.of("logmeal", "logmeal", "other");       // one entry per confirmed class

        HttpClient client = HttpClient.newBuilder().build();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // === Step 2: Confirm/correct dishes (per segment or extra) ===
        String url = BASE_URL + "/image/confirm/dish";
        
        // Build payload map to be converted to JSON
        Map<String, Object> dish_payload = Map.of(
            "imageId", IMAGE_ID,
            "confirmedClass", confirmed_classes,
            "food_item_position", food_item_positions,
            "source", sources
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_USER_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(dish_payload)))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
        if (resp.statusCode() != 200) {
            System.err.println("HTTP Error: " + resp.statusCode());
            System.err.println(resp.body());
            System.exit(1);
        }

        JsonObject dish_data = gson.fromJson(resp.body(), JsonObject.class);
        System.out.println("Dish confirmation -> OK");
        System.out.println(gson.toJson(dish_data));
    }
}