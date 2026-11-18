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
    public class ConfirmFoodQuantity8
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
            string url = $"{BASE_URL}/nutrition/confirm/quantity";           // confirm food quantities

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // === Step 2: Build edits & POST ===
            // Example: set/adjust per-item serving sizes (by region)
            // Values are quantities in grams (or ml for liquids)
            var edits = new Dictionary<int, double>
            {
                { 1, 50.0 },   // steamed broccoli
                { 2, 180.2 },  // schnitzel
                { 3, 30.0 },   // mayonnaise
                { 4, 120.0 }   // baked potatoes
            };

            var payload = new 
            { 
                imageId = IMAGE_ID, 
                quantity = edits 
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

            Console.WriteLine("Food quantities confirmed successfully");
        }
    }
}