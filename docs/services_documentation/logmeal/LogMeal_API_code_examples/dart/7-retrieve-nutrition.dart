import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

/**
 * ####################################################################################
 * # CODE FOR LOCAL TESTING, DO NOT COPY THIS INTO THE README.COM RECIPE
 * * const String BASE_URL = "http://localhost:8080/v2";
 * const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";
 * const int IMAGE_ID = 2072702;
 * ####################################################################################
 */

// CODE TO BE COPIED TO README.COM

void main() async {
  // === Step 1: Configure inputs & endpoint ===
  const String BASE_URL = "https://api.logmeal.com/v2";
  const String API_USER_TOKEN = "<YOUR_APIUSER_TOKEN>";                  // APIUser token
  const int IMAGE_ID = 1981521;                                         // From segmentation
  const String url = "$BASE_URL/nutrition/recipe/nutritionalInfo";      // default latest model
  
  final Map<String, String> headers = {
    "Authorization": "Bearer $API_USER_TOKEN",
    "Content-Type": "application/json"
  };

  // === Step 2: Request nutritional info ===
  final Map<String, dynamic> payload = {"imageId": IMAGE_ID};
  
  final response = await http.post(
    Uri.parse(url),
    headers: headers,
    body: jsonEncode(payload),
  ).timeout(const Duration(seconds: 30));

  if (response.statusCode != 200) {
    print("Request failed with status: ${response.statusCode}.");
    print(response.body);
    exit(1);
  }

  final Map<String, dynamic> data = jsonDecode(response.body);

  // === Step 3: Parse & print summary (matches provided structure) ===
  print("imageId: ${data['imageId'] ?? IMAGE_ID}");
  if (data.containsKey('serving_size')) {
    print("meal_serving_size: ${data['serving_size']} g");
  }

  // Optional: overall Nutri-Score (if present)
  if (data['image_nutri_score'] is Map) {
    final ns = data['image_nutri_score'];
    print("nutri_score: ${ns['nutri_score_category']} (${ns['nutri_score_standardized']})");
  }

  // Meal-level totals
  final Map total = data['nutritional_info'] ?? {};
  if (total.isNotEmpty) {
    final cal = total['calories'];
    if (cal != null) {
      print("calories_total: ${cal.toStringAsFixed(2)} kcal");
    }

    // Daily Intake Reference (DIR) summary
    final Map dirf = total['dailyIntakeReference'] ?? {};
    if (dirf.isNotEmpty) {
      print("Daily Intake Reference (meal):");
      const List<String> codes = ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'];
      for (var k in codes) {
        final v = dirf[k];
        if (v is Map) {
          final label = v['label'] ?? k;
          final level = v['level'];
          final pct = v['percent'] != null ? double.parse(v['percent'].toString()).toStringAsFixed(1) : "0.0";
          print("  $label: $pct% ($level)");
        }
      }
    }

    // Key nutrients (totals)
    final Map tn = total['totalNutrients'] ?? {};
    if (tn.isNotEmpty) {
      String? fmt(dynamic n) {
        if (n is! Map) return null;
        final q = n['quantity'];
        final u = n['unit'];
        final lab = n['label'];
        if (q == null || u == null) return null;
        return "$lab: ${double.parse(q.toString()).toStringAsFixed(2)} $u";
      }
      
      const List<String> keyCodes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR'];
      final List<String> nutrients = keyCodes.map((c) => fmt(tn[c])).whereType<String>().toList();
      print("Nutrients (meal): ${nutrients.join('; ')}");
    }
  }

  // Per-item nutritional info
  final Map<int, String> nameMap = {};
  if (data['ids'] is List && data['foodName'] is List) {
    final ids = data['ids'] as List;
    final names = data['foodName'] as List;
    for (int i = 0; i < ids.length; i++) {
      nameMap[ids[i]] = names[i].toString();
    }
  }

  final List items = data['nutritional_info_per_item'] ?? [];
  for (var it in items) {
    final pos = it['food_item_position'];
    final iid = it['id'];
    final dname = nameMap[iid] ?? "Unknown";
    final ss = it['serving_size'];
    print("[#$pos] dish=$dname serving_size=$ss g");

    if (it['nutri_score'] is Map) {
      final ns = it['nutri_score'];
      print("  nutri_score: ${ns['nutri_score_category']} (${ns['nutri_score_standardized']})");
    }

    final Map ni = it['nutritional_info'] ?? {};
    final calI = ni['calories'];
    if (calI != null) {
      print("  calories: ${calI.toStringAsFixed(2)} kcal");
    }

    // Item nutrients and DIR
    final Map tnI = ni['totalNutrients'] ?? {};
    final Map dirI = ni['dailyIntakeReference'] ?? {};

    if (tnI.isNotEmpty) {
      String? fmtI(dynamic n) {
        if (n is! Map) return null;
        final q = n['quantity'];
        final u = n['unit'];
        final lab = n['label'];
        if (q == null || u == null) return null;
        return "$lab: ${double.parse(q.toString()).toStringAsFixed(2)} $u";
      }
      const List<String> keyCodes = ['PROCNT','FAT','CHOCDF','FIBTG','SUGAR'];
      final List<String> nutrientsI = keyCodes.map((c) => fmtI(tnI[c])).whereType<String>().toList();
      print("  nutrients: ${nutrientsI.join('; ')}");
    }

    if (dirI.isNotEmpty) {
      final List<String> out = [];
      const List<String> codes = ['ENERC_KCAL','PROCNT','FAT','CHOCDF','FIBTG','SUGAR','FASAT','NA'];
      for (var k in codes) {
        final v = dirI[k];
        if (v is Map && v['percent'] != null) {
          final pct = double.parse(v['percent'].toString()).toStringAsFixed(1);
          out.add("${v['label']}: $pct%");
        }
      }
      if (out.isNotEmpty) {
        print("  daily intake: ${out.join('; ')}");
      }
    }
  }
}