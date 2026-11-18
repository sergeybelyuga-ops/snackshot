using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace LogMeal
{
    public class ManualDishSearch4
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
            string API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                   // APIUser token
            object IMAGE_ID = "<imageId-from-previous-step>";                 // From segmentation
            string USER_QUERY = "miso soup";                  // what the user typed
            int TARGET_REGION = 2;                            // region to correct (food_item_position)
            string LANGUAGE = "eng";                          // language of the dishes search

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // (Example) Current mapping you plan to CONFIRM for the WHOLE image.
            // IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
            // Use 'object?' to tell C# we intentionally allow null values in these lists
            var confirmedClasses = new List<object?> { 110, null };
            var foodItemPositions = new List<object> { 1, 2 }; // This one stays as object or int
            var sources = new List<string> { "logmeal", "logmeal" };

            // === Step 2: Search the dish catalog (GET /dataset/dishes) ===
            string dsUrl = $"{BASE_URL}/dataset/dishes?language={LANGUAGE}";
            var dsResponse = await client.GetAsync(dsUrl);
            dsResponse.EnsureSuccessStatusCode();

            string dsJson = await dsResponse.Content.ReadAsStringAsync();
            JToken catalog = JToken.Parse(dsJson);
            
            var allDishes = FlattenDishes(catalog);
            string q = USER_QUERY.ToLower().Trim();
            
            // LINQ search for hits
            var hits = allDishes.Where(d => d.Name.ToLower().Contains(q)).ToList();

            if (hits.Any())
            {
                var picked = hits.First();
                // Find index of TARGET_REGION to update it
                int idx = foodItemPositions.IndexOf(TARGET_REGION);
                if (idx != -1)
                {
                    confirmedClasses[idx] = picked.Id;
                }
            }

            // === Step 3: Confirm/correct dishes ===
            string dishUrl = $"{BASE_URL}/image/confirm/dish";
            var dishPayload = new
            {
                imageId = IMAGE_ID,
                confirmedClass = confirmedClasses,
                food_item_position = foodItemPositions,
                source = sources
            };

            string jsonPayload = JsonConvert.SerializeObject(dishPayload);
            var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

            var response = await client.PostAsync(dishUrl, content);
            response.EnsureSuccessStatusCode();

            string responseBody = await response.Content.ReadAsStringAsync();
            JObject dishData = JObject.Parse(responseBody);

            Console.WriteLine("Dish confirmation -> OK");
            Console.WriteLine(dishData.ToString(Formatting.Indented));
        }

        private static List<(int Id, string Name)> FlattenDishes(JToken payload)
        {
            var flat = new List<(int Id, string Name)>();

            if (payload is JObject obj)
            {
                foreach (var property in obj.Properties())
                {
                    if (property.Value is JArray items)
                    {
                        foreach (var it in items)
                        {
                            int? id = it["id"]?.Value<int>() ?? it["dishId"]?.Value<int>() ?? it["class_id"]?.Value<int>();
                            // Use (string?) and ?.Value<string>() to handle the conversion safely
                            string? name = it["name"]?.Value<string>();
                            if (id.HasValue && !string.IsNullOrEmpty(name))
                                flat.Add((id.Value, name));
                        }
                    }
                }
            }
            return flat;
        }
    }
}