package com.logmeal;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class ImageBasedFoodRecognition1 {

    public static void main(String[] args) throws IOException, InterruptedException {
        
        /**
         * ####################################################################################
         * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
         
        String TEST_BASE_URL = "http://localhost:8080/v2";
        String TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";  // APIUser token
        String TEST_IMAGE_PATH = "path/to/multiple_items_meal.jpg>";
        /**
         * ####################################################################################
         */

        // CODE TO BE COPIED TO README.COM

        // === Step 1: Configure inputs & endpoint ===
        String BASE_URL = "https://api.logmeal.com/v2";
        String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                 // APIUser token
        String IMAGE_PATH = "path/to/multiple_items_meal.jpg>";                         // Local JPEG path
        String url = BASE_URL + "/image/segmentation/complete";
        
        HttpClient client = HttpClient.newBuilder().build();
        String boundary = "Boundary-" + UUID.randomUUID().toString();

        // === Step 2: POST image & get JSON ===
        byte[] fileBytes = Files.readAllBytes(Paths.get(IMAGE_PATH));
        String fileName = Paths.get(IMAGE_PATH).getFileName().toString();

        // Build Multipart Body
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"image\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: image/jpeg\r\n\r\n");

        byte[] header = sb.toString().getBytes();
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes();
        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_USER_TOKEN)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            System.err.println("HTTP Error: " + resp.statusCode());
            System.err.println(resp.body());
            System.exit(1);
        }

        Gson gson = new Gson();
        JsonObject data = gson.fromJson(resp.body(), JsonObject.class);

        // === Step 3: Parse & print summary ===
        System.out.println("imageId: " + (data.has("imageId") ? data.get("imageId") : "null"));
        if (data.has("occasion")) {
            System.out.println("occasion: " + data.get("occasion").getAsString());
        }
        if (data.has("foodType")) {
            JsonObject ft = data.getAsJsonObject("foodType");
            System.out.println("foodType: " + ft.get("name").getAsString() + " (id " + ft.get("id") + ")");
        }

        JsonArray segResults = data.has("segmentation_results") ? data.getAsJsonArray("segmentation_results") : new JsonArray();
        for (JsonElement element : segResults) {
            JsonObject item = element.getAsJsonObject();
            int pos = item.get("food_item_position").getAsInt();
            JsonObject bb = item.getAsJsonObject("contained_bbox");
            JsonObject center = item.getAsJsonObject("center");
            JsonElement grams = item.get("serving_size");
            JsonArray recs = item.has("recognition_results") ? item.getAsJsonArray("recognition_results") : new JsonArray();
            JsonObject top = (recs != null && recs.size() > 0) ? recs.get(0).getAsJsonObject() : null;

            String line = String.format("[#%d] bbox={'x': %s, 'y': %s, 'w': %s, 'h': %s} center={'x': %s, 'y': %s}",
                    pos, bb.get("x"), bb.get("y"), bb.get("w"), bb.get("h"),
                    center.get("x"), center.get("y"));
            
            if (grams != null && !grams.isJsonNull()) {
                line += " grams=" + grams.getAsString();
            }
            System.out.println(line);

            if (top != null) {
                double prob = top.get("prob").getAsDouble();
                System.out.printf("  top dish: %s (id %s, prob %.3f)%n", 
                        top.get("name").getAsString(), top.get("id"), prob);

                List<String> cands = new ArrayList<>();
                for (int i = 0; i < Math.min(recs.size(), 5); i++) {
                    JsonObject r = recs.get(i).getAsJsonObject();
                    double p = r.get("prob").getAsDouble();
                    cands.add(String.format("%s (id %s, prob %.3f)", r.get("name").getAsString(), r.get("id"), p));
                }
                System.out.println("  top-5 candidates: " + String.join("; ", cands));
            }
        }
    }
}