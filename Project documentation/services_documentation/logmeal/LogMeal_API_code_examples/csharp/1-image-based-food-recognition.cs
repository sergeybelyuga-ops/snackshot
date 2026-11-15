using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Threading.Tasks;
using System.Linq;
using Newtonsoft.Json.Linq;

namespace LogMeal
{
    public class ImageBasedFoodRecognition1
    {
        public static async Task Main(string[] args)
        {
            /**
             * ####################################################################################
             * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
            string TEST_BASE_URL = "http://localhost:8080/v2";
            string TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
            string TEST_IMAGE_PATH = "path/to/multiple_items_meal.jpg>";
             * ####################################################################################
             */

            // CODE TO BE COPIED TO README.COM

            // === Step 1: Configure inputs & endpoint ===
            string BASE_URL = "https://api.logmeal.com/v2";
            string API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                     // APIUser token
            string IMAGE_PATH = "<path/to/multiple_items_meal.jpg>";            // Local JPEG path
            string url = $"{BASE_URL}/image/segmentation/complete";

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // === Step 2: POST image & get JSON ===
            using var form = new MultipartFormDataContent();
            
            if (!File.Exists(IMAGE_PATH))
            {
                Console.Error.WriteLine($"Error: Image not found at {Path.GetFullPath(IMAGE_PATH)}");
                return;
            }

            using var fileStream = File.OpenRead(IMAGE_PATH);
            using var streamContent = new StreamContent(fileStream);
            streamContent.Headers.ContentType = new MediaTypeHeaderValue("image/jpeg");
            
            form.Add(streamContent, "image", Path.GetFileName(IMAGE_PATH));
            var response = await client.PostAsync(url, form);

            if (!response.IsSuccessStatusCode)
            {
                string errorBody = await response.Content.ReadAsStringAsync();
                Console.Error.WriteLine($"HTTP Error: {(int)response.StatusCode} - {errorBody}");
                return;
            }

            string jsonResponse = await response.Content.ReadAsStringAsync();
            JObject data = JObject.Parse(jsonResponse);

            // === Step 3: Parse & print summary ===
            Console.WriteLine($"imageId: {data["imageId"]}");
            
            if (data["occasion"] != null)
                Console.WriteLine($"occasion: {data["occasion"]}");

            var ft = data["foodType"];
            if (ft != null && ft.HasValues)
                Console.WriteLine($"foodType: {ft["name"]} (id {ft["id"]})");

            // Iterate each segmented food item / region
            var segments = data["segmentation_results"] as JArray ?? new JArray();
            foreach (var item in segments)
            {
                var pos = item["food_item_position"];
                var bb = item["contained_bbox"] ?? new JObject();
                var center = item["center"] ?? new JObject();
                var grams = item["serving_size"];
                var recs = item["recognition_results"] as JArray ?? new JArray();
                var top = recs.FirstOrDefault();

                string line = $"[#{pos}] bbox={{'x': {bb["x"]}, 'y': {bb["y"]}, 'w': {bb["w"]}, 'h': {bb["h"]}}} center={{'x': {center["x"]}, 'y': {center["y"]}}}";
                if (grams != null && grams.Type != JTokenType.Null) line += $" grams={grams}";
                Console.WriteLine(line);

                if (top != null)
                {
                    double prob = Math.Round(top["prob"]?.Value<double>() ?? 0, 3);
                    Console.WriteLine($"  top dish: {top["name"]} (id {top["id"]}, prob {prob})");

                    // extra: print top-5 candidates for this region
                    var candidates = recs.Take(5).Select(r => {
                        double p = Math.Round(r["prob"]?.Value<double>() ?? 0, 3);
                        return $"{r["name"]} (id {r["id"]}, prob {p})";
                    });
                    Console.WriteLine("  top-5 candidates: " + string.Join("; ", candidates));
                }
            }
        }
    }
}