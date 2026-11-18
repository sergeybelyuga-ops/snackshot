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
    public class FoodItemsConfirmation2
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

            // === Step 1: Configure inputs & intended corrections ===
            string BASE_URL = "https://api.logmeal.com/v2";
            string API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";   // APIUser token (Bearer)
            int IMAGE_ID = 2072704;                           // obtained from a previous food intake insertion

            // Example mapping from food intake insertion results you want to confirm/correct:
            // IMPORTANT: /image/confirm/dish OVERWRITES the image; any region NOT SENT here gets REMOVED.
            // - Region #1 was recognized as "donuts" (id 110) and that's correct.
            // - Region #2 should be corrected to "mushroom cream" (id 1474).
            // - Add an extra dish not in any segment: free text "grandma soup".
            var confirmedClasses = new List<object> { 110, 1474, "grandma soup" };
            var foodItemPositions = new List<object> { 1, 2, "extra_dish_1" };
            var sources = new List<string> { "logmeal", "logmeal", "other" };

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // === Step 2: Confirm/correct dishes (per segment or extra) ===
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

            // Raise for HTTP errors (e.g., 401 bad token, 429 rate limit)
            if (!response.IsSuccessStatusCode)
            {
                string errorBody = await response.Content.ReadAsStringAsync();
                Console.Error.WriteLine($"HTTP Error: {(int)response.StatusCode} - {errorBody}");
                return;
            }

            string responseBody = await response.Content.ReadAsStringAsync();
            JObject dishData = JObject.Parse(responseBody);

            Console.WriteLine("Dish confirmation -> OK");
            Console.WriteLine(dishData.ToString(Formatting.Indented));
        }
    }
}