package com.logmeal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class RetrieveNutrition7 {

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
        String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                  // APIUser token
        Object IMAGE_ID = "<imageId-from-previous-step>";                // From segmentation
        String url = BASE_URL + "/nutrition/recipe/nutritionalInfo";     // default latest model

        // Ensure IMAGE_ID is the correct type for local testing
        if (IMAGE_ID.toString().contains("previous-step")) { IMAGE_ID = 2072703; }

        HttpClient client = HttpClient.newBuilder().build();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        headers(API_USER_TOKEN); // Utility for headers

        // === Step 2: Request nutritional info ===
        Map<String, Object> payload = Map.of("imageId", IMAGE_ID);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_USER_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Raise for HTTP errors
        if (resp.statusCode() != 200) {
            System.err.println("HTTP Error: " + resp.statusCode() + " - " + resp.body());
            System.exit(1);
        }

        JsonObject data = gson.fromJson(resp.body(), JsonObject.class);

        // === Step 3: Parse & print summary ===
        System.out.println("imageId: " + (data.has("imageId") ? data.get("imageId") : IMAGE_ID));
        if (data.has("serving_size") && !data.get("serving_size").isJsonNull()) {
            System.out.println("meal_serving_size: " + data.get("serving_size") + " g");
        }

        // Optional: overall Nutri-Score
        if (data.has("image_nutri_score") && data.get("image_nutri_score").isJsonObject()) {
            JsonObject ns = data.getAsJsonObject("image_nutri_score");
            System.out.println(String.format("nutri_score: %s (%s)", 
                ns.get("nutri_score_category").getAsString(), 
                ns.get("nutri_score_standardized").getAsString()));
        }

        // Meal-level totals
        JsonObject total = data.has("nutritional_info") ? data.getAsJsonObject("nutritional_info") : null;
        if (total != null) {
            if (total.has("calories") && !total.get("calories").isJsonNull()) {
                System.out.println(String.format("calories_total: %.2f kcal", total.get("calories").getAsDouble()));
            }

            // Daily Intake Reference (meal)
            JsonObject dirf = total.has("dailyIntakeReference") ? total.getAsJsonObject("dailyIntakeReference") : null;
            if (dirf != null) {
                System.out.println("Daily Intake Reference (meal):");
                for (String k : List.of("ENERC_KCAL","PROCNT","FAT","CHOCDF","FIBTG","SUGAR","FASAT","NA")) {
                    if (dirf.has(k) && dirf.get(k).isJsonObject()) {
                        JsonObject v = dirf.getAsJsonObject(k);
                        String label = v.has("label") ? v.get("label").getAsString() : k;
                        String level = v.has("level") ? v.get("level").getAsString() : "unknown";
                        if (v.has("percent") && !v.get("percent").isJsonNull()) {
                            System.out.println(String.format("  %s: %.1f%% (%s)", label, v.get("percent").getAsDouble(), level));
                        }
                    }
                }
            }

            // Key nutrients (totals)
            JsonObject tn = total.has("totalNutrients") ? total.getAsJsonObject("totalNutrients") : null;
            if (tn != null) {
                List<String> keyCodes = List.of("PROCNT","FAT","CHOCDF","FIBTG","SUGAR");
                String nutrientsLine = keyCodes.stream()
                    .map(c -> formatNutrient(tn.get(c)))
                    .filter(s -> s != null)
                    .collect(Collectors.joining("; "));
                if (!nutrientsLine.isEmpty()) {
                    System.out.println("Nutrients (meal): " + nutrientsLine);
                }
            }
        }

        // Per-item nutritional info
        Map<Integer, String> nameMap = new HashMap<>();
        if (data.has("ids") && data.has("foodName")) {
            JsonArray ids = data.getAsJsonArray("ids");
            JsonArray names = data.getAsJsonArray("foodName");
            for (int i = 0; i < ids.size(); i++) {
                nameMap.put(ids.get(i).getAsInt(), names.get(i).getAsString());
            }
        }

        JsonArray items = data.has("nutritional_info_per_item") ? data.getAsJsonArray("nutritional_info_per_item") : null;
        if (items != null) {
            for (JsonElement itEl : items) {
                JsonObject it = itEl.getAsJsonObject();
                int pos = it.get("food_item_position").getAsInt();
                int iid = it.get("id").getAsInt();
                String dname = nameMap.getOrDefault(iid, "unknown");
                double ss = it.has("serving_size") ? it.get("serving_size").getAsDouble() : 0.0;
                System.out.println(String.format("[#%d] dish=%s serving_size=%.1f g", pos, dname, ss));

                if (it.has("nutri_score") && it.get("nutri_score").isJsonObject()) {
                    JsonObject ns = it.getAsJsonObject("nutri_score");
                    System.out.println(String.format("  nutri_score: %s (%s)", 
                        ns.get("nutri_score_category").getAsString(), 
                        ns.get("nutri_score_standardized").getAsString()));
                }

                JsonObject ni = it.has("nutritional_info") ? it.getAsJsonObject("nutritional_info") : null;
                if (ni != null) {
                    if (ni.has("calories") && !ni.get("calories").isJsonNull()) {
                        System.out.println(String.format("  calories: %.2f kcal", ni.get("calories").getAsDouble()));
                    }

                    JsonObject tnI = ni.has("totalNutrients") ? ni.getAsJsonObject("totalNutrients") : null;
                    if (tnI != null) {
                        List<String> codes = List.of("PROCNT","FAT","CHOCDF","FIBTG","SUGAR");
                        String line = codes.stream().map(c -> formatNutrient(tnI.get(c))).filter(s -> s != null).collect(Collectors.joining("; "));
                        System.out.println("  nutrients: " + line);
                    }

                    JsonObject dirI = ni.has("dailyIntakeReference") ? ni.getAsJsonObject("dailyIntakeReference") : null;
                    if (dirI != null) {
                        List<String> out = new ArrayList<>();
                        for (String k : List.of("ENERC_KCAL","PROCNT","FAT","CHOCDF","FIBTG","SUGAR","FASAT","NA")) {
                            if (dirI.has(k) && dirI.get(k).isJsonObject()) {
                                JsonObject v = dirI.getAsJsonObject(k);
                                if (v.has("percent") && !v.get("percent").isJsonNull()) {
                                    out.add(String.format("%s: %.1f%%", v.get("label").getAsString(), v.get("percent").getAsDouble()));
                                }
                            }
                        }
                        System.out.println("  daily intake: " + String.join("; ", out));
                    }
                }
            }
        }
    }

    private static String formatNutrient(JsonElement n) {
        if (n == null || !n.isJsonObject()) return null;
        JsonObject obj = n.getAsJsonObject();
        if (!obj.has("quantity") || obj.get("quantity").isJsonNull()) return null;
        return String.format("%s: %.2f %s", 
            obj.get("label").getAsString(), 
            obj.get("quantity").getAsDouble(), 
            obj.get("unit").getAsString());
    }

    private static void headers(String token) {} // Placeholder for structure
}