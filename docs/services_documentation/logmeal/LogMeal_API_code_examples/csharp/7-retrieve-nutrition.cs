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
    public class RetrieveNutrition7
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
            string API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
            object IMAGE_ID = "<imageId-from-previous-step>";
            string url = $"{BASE_URL}/nutrition/recipe/nutritionalInfo";

            using var client = new HttpClient();
            client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", API_USER_TOKEN);

            // === Step 2: Request nutritional info ===
            var payload = new { imageId = IMAGE_ID };
            string jsonPayload = JsonConvert.SerializeObject(payload);
            var content = new StringContent(jsonPayload, Encoding.UTF8, "application/json");

            var response = await client.PostAsync(url, content);
            response.EnsureSuccessStatusCode();

            string responseBody = await response.Content.ReadAsStringAsync();
            JObject data = JObject.Parse(responseBody);

            // === Step 3: Parse & print summary ===
            Console.WriteLine($"imageId: {data["imageId"] ?? IMAGE_ID.ToString()}");
            if (data["serving_size"] != null)
                Console.WriteLine($"meal_serving_size: {data["serving_size"]} g");

            var nsGlobal = data["image_nutri_score"];
            if (nsGlobal is JObject)
                Console.WriteLine($"nutri_score: {nsGlobal["nutri_score_category"]} ({nsGlobal["nutri_score_standardized"]})");

            var total = data["nutritional_info"] as JObject;
            if (total != null)
            {
                double? cal = total["calories"]?.Value<double?>();
                if (cal.HasValue) Console.WriteLine($"calories_total: {Math.Round(cal.Value, 2)} kcal");

                var dirf = total["dailyIntakeReference"] as JObject;
                if (dirf != null)
                {
                    Console.WriteLine("Daily Intake Reference (meal):");
                    string[] keys = { "ENERC_KCAL", "PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR", "FASAT", "NA" };
                    foreach (var k in keys)
                    {
                        if (dirf[k] is JObject v)
                        {
                            string label = v["label"]?.ToString() ?? k;
                            string level = v["level"]?.ToString() ?? "N/A";
                            double? pct = v["percent"]?.Value<double?>();
                            if (pct.HasValue)
                                Console.WriteLine($"  {label}: {Math.Round(pct.Value, 1)}% ({level})");
                        }
                    }
                }

                var tn = total["totalNutrients"] as JObject;
                if (tn != null)
                {
                    string? FormatNutrient(JToken? n) => (n is JObject && n["quantity"] != null) 
                        ? $"{n["label"]}: {Math.Round(n["quantity"]?.Value<double>() ?? 0, 2)} {n["unit"]}" 
                        : null;

                    string[] codes = { "PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR" };
                    var nutrientsLine = codes.Select(c => FormatNutrient(tn[c])).Where(s => s != null);
                    Console.WriteLine("Nutrients (meal): " + string.Join("; ", nutrientsLine));
                }
            }

            var ids = data["ids"]?.ToObject<List<int>>() ?? new List<int>();
            var foodNames = data["foodName"]?.ToObject<List<string>>() ?? new List<string>();
            var nameMap = ids.Zip(foodNames, (id, name) => new { id, name }).ToDictionary(x => x.id, x => x.name);

            if (data["nutritional_info_per_item"] is JArray items)
            {
                foreach (var it in items)
                {
                    int pos = it["food_item_position"]?.Value<int>() ?? 0;
                    int iid = it["id"]?.Value<int>() ?? 0;
                    nameMap.TryGetValue(iid, out string? dname);
                    
                    Console.WriteLine($"[#{pos}] dish={dname} serving_size={it["serving_size"]} g");

                    if (it["nutri_score"] is JObject ns)
                        Console.WriteLine($"  nutri_score: {ns["nutri_score_category"]} ({ns["nutri_score_standardized"]})");

                    if (it["nutritional_info"] is JObject ni)
                    {
                        double? calI = ni["calories"]?.Value<double?>();
                        if (calI.HasValue) Console.WriteLine($"  calories: {Math.Round(calI.Value, 2)} kcal");

                        if (ni["totalNutrients"] is JObject tnI)
                        {
                            string[] codes = { "PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR" };
                            var itemNutrients = codes.Select(c => {
                                var n = tnI[c];
                                return (n is JObject && n["quantity"] != null) 
                                    ? $"{n["label"]}: {Math.Round(n["quantity"]?.Value<double>() ?? 0, 2)} {n["unit"]}" 
                                    : null;
                            }).Where(s => s != null);
                            Console.WriteLine("  nutrients: " + string.Join("; ", itemNutrients));
                        }

                        if (ni["dailyIntakeReference"] is JObject dirI)
                        {
                            string[] keys = { "ENERC_KCAL", "PROCNT", "FAT", "CHOCDF", "FIBTG", "SUGAR", "FASAT", "NA" };
                            var dirOut = keys.Select(k => {
                                var v = dirI[k];
                                return (v is JObject && v["percent"] != null) 
                                    ? $"{v["label"]}: {Math.Round(v["percent"]?.Value<double>() ?? 0, 1)}%" 
                                    : null;
                            }).Where(s => s != null);
                            Console.WriteLine("  daily intake: " + string.Join("; ", dirOut));
                        }
                    }
                }
            }
        }
    }
}