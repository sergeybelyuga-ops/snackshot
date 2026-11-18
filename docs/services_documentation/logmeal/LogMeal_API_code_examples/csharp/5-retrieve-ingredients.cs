using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace LogMeal
{
    public class RetrieveIngredients5
    {
        public static async Task Main(string[] args)
        {
            /**
             * ####################################################################################
             * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
            string TEST_BASE_URL = "http://localhost:8080/v2";
            string TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
            int TEST_IMAGE_ID = 2072704;
             * ####################################################################################
             */

            // CODE TO BE COPIED TO README.COM

            // === Step 1: Configure inputs & endpoint ===
            string BASE_URL = "https://api.logmeal.com/v2";
            string API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                  // APIUser token
            object IMAGE_ID = "<imageId-from-previous-step>";                // From segmentation
            string url = $"{BASE_URL}/nutrition/recipe/ingredients";         // default latest model

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // === Step 2: Request ingredients & quantities ===
            var payload = new { imageId = IMAGE_ID };
            string jsonPayload = JsonConvert.SerializeObject(payload);
            var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

            var response = await client.PostAsync(url, content);

            if (!response.IsSuccessStatusCode)
            {
                string errorBody = await response.Content.ReadAsStringAsync();
                Console.Error.WriteLine($"HTTP Error: {(int)response.StatusCode} - {errorBody}");
                return;
            }

            string responseBody = await response.Content.ReadAsStringAsync();
            JObject data = JObject.Parse(responseBody);

            // === Step 3: Parse & print summary ===
            Console.WriteLine($"imageId: {data["imageId"] ?? IMAGE_ID.ToString()}");
            if (data["serving_size"] != null)
            {
                Console.WriteLine($"meal_serving_size: {data["serving_size"]} g");
            }

            // Prefer per-item recipes if present
            var items = data["recipe_per_item"] as JArray;

            if (items != null && items.Count > 0)
            {
                foreach (var it in items)
                {
                    var pos = it["food_item_position"];
                    var dname = it["name"];
                    var ss = it["serving_size"];
                    var unit = it["unit"];
                    Console.WriteLine($"[#{pos}] dish={dname} serving_size={ss} {unit}");

                    // Ingredients for this dish
                    var recipe = it["recipe"] as JArray ?? new JArray();
                    foreach (var ing in recipe)
                    {
                        PrintIngredient(ing);
                    }
                }
            }
            else
            {
                // Fallback 1: show flat global recipe
                var ingrList = data["recipe"] as JArray;
                if (ingrList != null && ingrList.Count > 0)
                {
                    Console.WriteLine("Global recipe:");
                    foreach (var ing in ingrList)
                    {
                        PrintIngredient(ing);
                    }
                }
                else
                {
                    // Fallback 2: generic fields
                    foreach (var prop in data.Properties())
                    {
                        string[] ignored = { "imageId", "recipe_per_item", "recipe", "dish_id", "foodName", "is_combo", "serving_size", "source" };
                        if (!Array.Exists(ignored, element => element == prop.Name))
                        {
                            Console.WriteLine($"{prop.Name}: {prop.Value}");
                        }
                    }
                }
            }
        }

        private static void PrintIngredient(JToken ing)
        {
            var iid = ing["id"];
            var name = ing["name"];
            var weight = ing["weight"];
            var unitI = ing["unit"]?.ToString() ?? "g";
            var meas = ing["measure"] ?? new JObject();
            var metric = meas["metric"];
            var preferred = meas["preferred"];

            // Metric (canonical) amount
            var mQty = (metric != null && metric["quantity"] != null) ? metric["quantity"] : weight;
            var mShort = metric?["short"]?.ToString() ?? unitI;

            // Preferred (human-friendly) measure if available
            var pQty = preferred?["quantity"];
            var pName = preferred?["name"];

            string line = $"  - {name} (id {iid}) -> {mQty} {mShort}";
            if (pQty != null && pName != null)
            {
                line += $" (~{pQty} {pName})";
            }
            Console.WriteLine(line);
        }
    }
}