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
    public class EditIngredients6
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
            string url = $"{BASE_URL}/nutrition/confirm/ingredients";        // confirm edits

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // === Step 2: Build edits & POST ===
            // Example: adjust quantities and swap ingredients per detected item
            // 'ingredientAmount' is in grams (or ml for liquids)
            var edits = new Dictionary<int, List<object>>
            {
                { 
                    1, new List<object> 
                    {
                        new { ingredientId = 1478, ingredientAmount = 85.0 }, // chicken (boneless) -> 85 g
                        new { ingredientId = 61,   ingredientAmount = 12.0 }, // bread crumbles -> 12 g
                        new { ingredientId = 562,  ingredientAmount = 12.0 }  // oil -> 12 ml
                    }
                },
                { 
                    2, new List<object> 
                    {
                        new { ingredientId = 204,  ingredientAmount = 220.0 }, // potato -> 220 g
                        new { ingredientId = 562,  ingredientAmount = 8.0 },   // oil -> 8 ml
                        new { ingredientId = 45,   ingredientAmount = 1.2 }    // salt -> 1.2 g
                    }
                }
            };

            var payload = new 
            { 
                imageId = IMAGE_ID, 
                ingredients_per_item = edits 
            };

            string jsonPayload = JsonConvert.SerializeObject(payload);
            var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

            var response = await client.PostAsync(url, content);

            // Raise for HTTP errors
            if (!response.IsSuccessStatusCode)
            {
                string errorBody = await response.Content.ReadAsStringAsync();
                Console.Error.WriteLine($"HTTP Error: {(int)response.StatusCode} - {errorBody}");
                return;
            }

            Console.WriteLine("Ingredients updated and confirmed successfully");
        }
    }
}