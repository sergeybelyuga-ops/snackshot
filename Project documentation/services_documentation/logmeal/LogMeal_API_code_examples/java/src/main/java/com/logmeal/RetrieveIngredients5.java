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
import java.util.List;

public class RetrieveIngredients5 {

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
        Object IMAGE_ID = "<imageId-from-previous-step>";                               // From segmentation
        String url = BASE_URL + "/nutrition/recipe/ingredients";         // default latest model

        HttpClient client = HttpClient.newBuilder().build();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // === Step 2: Request ingredients & quantities ===
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

        // Prefer per-item recipes if present
        JsonArray items = data.has("recipe_per_item") ? data.getAsJsonArray("recipe_per_item") : null;

        if (items != null && items.size() > 0) {
            for (JsonElement itemElement : items) {
                JsonObject it = itemElement.getAsJsonObject();
                String pos = it.has("food_item_position") ? it.get("food_item_position").getAsString() : "?";
                String dname = it.has("name") ? it.get("name").getAsString() : "unknown";
                String ss = it.has("serving_size") ? it.get("serving_size").getAsString() : "0";
                String unit = it.has("unit") ? it.get("unit").getAsString() : "g";
                System.out.println(String.format("[#%s] dish=%s serving_size=%s %s", pos, dname, ss, unit));

                // Ingredients for this dish
                if (it.has("recipe")) {
                    printRecipe(it.getAsJsonArray("recipe"));
                }
            }
        } else {
            // Fallback 1: show flat global recipe
            JsonArray ingrList = data.has("recipe") ? data.getAsJsonArray("recipe") : null;
            if (ingrList != null && ingrList.size() > 0) {
                System.out.println("Global recipe:");
                printRecipe(ingrList);
            } else {
                // Fallback 2: generic fields
                for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                    String name = entry.getKey();
                    if (!List.of("imageId", "recipe_per_item", "recipe", "dish_id", "foodName", "is_combo", "serving_size", "source").contains(name)) {
                        System.out.println(name + ": " + gson.toJson(entry.getValue()));
                    }
                }
            }
        }
    }

    /**
     * Helper method to print ingredients in the same format as Python
     */
    private static void printRecipe(JsonArray recipe) {
        for (JsonElement ingElement : recipe) {
            JsonObject ing = ingElement.getAsJsonObject();
            String iid = ing.has("id") ? ing.get("id").getAsString() : "0";
            String name = ing.has("name") ? ing.get("name").getAsString() : "unknown";
            double weight = ing.has("weight") ? ing.get("weight").getAsDouble() : 0.0;
            String unitI = ing.has("unit") && !ing.get("unit").isJsonNull() ? ing.get("unit").getAsString() : "g";

            JsonObject meas = ing.has("measure") ? ing.getAsJsonObject("measure") : new JsonObject();
            JsonObject metric = meas.has("metric") ? meas.getAsJsonObject("metric") : null;
            JsonObject preferred = meas.has("preferred") ? meas.getAsJsonObject("preferred") : null;

            // Metric (canonical) amount
            String mQty = (metric != null && metric.has("quantity")) ? metric.get("quantity").getAsString() : String.valueOf(weight);
            String mShort = (metric != null && metric.has("short")) ? metric.get("short").getAsString() : unitI;

            // Preferred (human-friendly) measure if available
            String pLine = "";
            if (preferred != null && preferred.has("quantity") && preferred.has("name")) {
                pLine = String.format(" (~%s %s)", preferred.get("quantity").getAsString(), preferred.get("name").getAsString());
            }

            System.out.println(String.format("  - %s (id %s) -> %s %s%s", name, iid, mQty, mShort, pLine));
        }
    }
}