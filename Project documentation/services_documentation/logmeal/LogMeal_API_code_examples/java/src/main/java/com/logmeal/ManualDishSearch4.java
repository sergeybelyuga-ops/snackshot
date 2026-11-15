package com.logmeal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ManualDishSearch4 {

    public static void main(String[] args) throws IOException, InterruptedException {
        
        /**
         * ####################################################################################
         * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
        String BASE_URL = "http://localhost:8080/v2";
        String TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
        int TEST_IMAGE_ID = 2072703;
        /**
         * ####################################################################################
         */

        // CODE TO BE COPIED TO README.COM

        // === Step 1: Configure inputs ===
        String BASE_URL = "https://api.logmeal.com/v2";
        String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
        int IMAGE_ID = 1981521;                           // obtained from a previous segmentation/complete
        String USER_QUERY = "miso soup";                  // what the user typed
        int TARGET_REGION = 2;                            // region to correct (food_item_position)
        String LANGUAGE = "eng";                          // language of the dishes search

        HttpClient client = HttpClient.newBuilder().build();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // (Example) Current mapping you plan to CONFIRM for the WHOLE image.
        // IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
        List<Object> confirmed_classes = new ArrayList<>(List.of(110, 0)); // region #1 is donuts (id=110), #2 to be filled
        List<Object> food_item_positions = List.of(1, 2);                  // parallel to confirmed_classes
        List<String> sources = List.of("logmeal", "logmeal");              // 'logmeal' for catalog ids

        // === Step 2: Search the dish catalog (GET /dataset/dishes) ===
        String dsUrl = BASE_URL + "/dataset/dishes?language=" + LANGUAGE;
        HttpRequest dsRequest = HttpRequest.newBuilder()
                .uri(URI.create(dsUrl))
                .header("Authorization", "Bearer " + API_USER_TOKEN)
                .GET()
                .build();

        HttpResponse<String> dsResp = client.send(dsRequest, HttpResponse.BodyHandlers.ofString());
        if (dsResp.statusCode() != 200) {
            System.err.println("Failed to fetch catalog: " + dsResp.statusCode());
            System.exit(1);
        }

        JsonObject catalog = gson.fromJson(dsResp.body(), JsonObject.class);
        List<Map<String, Object>> allDishes = flattenDishes(catalog);
        
        String q = USER_QUERY.toLowerCase().trim();
        Map<String, Object> picked = null;

        for (Map<String, Object> dish : allDishes) {
            String name = (String) dish.get("name");
            if (name != null && name.toLowerCase().contains(q)) {
                picked = dish;
                break; // Found a hit
            }
        }

        // Map the user query to a dish id if found
        if (picked != null) {
            int dishId = ((Double) picked.get("id")).intValue();
            // Put the found id into the arrays at TARGET_REGION’s position
            int idx = food_item_positions.indexOf(TARGET_REGION);
            confirmed_classes.set(idx, dishId);
        }

        // === Step 3: Confirm/correct dishes ===
        String dishUrl = BASE_URL + "/image/confirm/dish";
        Map<String, Object> dishPayload = Map.of(
            "imageId", IMAGE_ID,
            "confirmedClass", confirmed_classes,
            "food_item_position", food_item_positions,
            "source", sources
        );

        HttpRequest confirmRequest = HttpRequest.newBuilder()
                .uri(URI.create(dishUrl))
                .header("Authorization", "Bearer " + API_USER_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(dishPayload)))
                .build();

        HttpResponse<String> confirmResp = client.send(confirmRequest, HttpResponse.BodyHandlers.ofString());
        if (confirmResp.statusCode() != 200) {
            System.err.println("Dish confirmation failed: " + confirmResp.body());
            System.exit(1);
        }

        JsonObject dishData = gson.fromJson(confirmResp.body(), JsonObject.class);
        System.out.println("Dish confirmation -> OK");
        System.out.println(gson.toJson(dishData));
    }

    /**
     * Helper to flatten the hierarchical catalog into a simple list
     */
    private static List<Map<String, Object>> flattenDishes(JsonObject payload) {
        List<Map<String, Object>> flat = new ArrayList<>();
        for (String key : payload.keySet()) {
            JsonElement element = payload.get(key);
            if (element.isJsonArray()) {
                for (JsonElement item : element.getAsJsonArray()) {
                    if (item.isJsonObject()) {
                        JsonObject obj = item.getAsJsonObject();
                        if (obj.has("name") && obj.has("id")) {
                            Map<String, Object> dish = new HashMap<>();
                            dish.put("id", obj.get("id").getAsDouble()); // GSON parses numbers as Double
                            dish.put("name", obj.get("name").getAsString());
                            flat.add(dish);
                        }
                    }
                }
            }
        }
        return flat;
    }
}