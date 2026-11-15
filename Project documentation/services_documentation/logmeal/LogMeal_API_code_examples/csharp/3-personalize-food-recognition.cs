using System;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace LogMeal
{
    public class PersonalizeFoodRecognition3
    {
        public static async Task Main(string[] args)
        {
            /**
             * ####################################################################################
             * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
            string TEST_BASE_URL = "http://localhost:8080/v2";
            string TEST_API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
             * ####################################################################################
             */

            // CODE TO BE COPIED TO README.COM

            // -----------------------------
            // Step 1 - Prepare configuration & inputs
            // -----------------------------
            string BASE_URL = "https://api.logmeal.com/v2";
            string API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";  // APIUser token

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // Define preferences (ISO country code + ISO language code)
            var preferences = new
            {
                country = "ES",    // Spain
                language = "spa"   // Spanish
            };

            // -----------------------------
            // Step 2 - Send profile personalization request
            // -----------------------------
            string url = $"{BASE_URL}/profile/modifyUserProfileInfo";
            string jsonPayload = JsonConvert.SerializeObject(preferences);
            var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

            var response = await client.PostAsync(url, content);

            // -----------------------------
            // Step 3 - Confirm personalization response
            // -----------------------------
            if (!response.IsSuccessStatusCode)
            {
                string errorBody = await response.Content.ReadAsStringAsync();
                Console.Error.WriteLine($"Failed to personalize profile ({(int)response.StatusCode}): {errorBody}");
                return;
            }

            string responseBody = await response.Content.ReadAsStringAsync();
            JObject profileInfo = JObject.Parse(responseBody);
            
            Console.WriteLine("Profile updated:");
            Console.WriteLine(profileInfo.ToString(Formatting.Indented));

            // -----------------------------
            // Step 4 - Use personalization in subsequent recognition requests
            // -----------------------------
            // After this call, all recognition endpoints (e.g., /image/segmentation/complete)
            // will return dish, ingredients and nutrient labels in the chosen language and
            // prioritized for the selected country.
        }
    }
}