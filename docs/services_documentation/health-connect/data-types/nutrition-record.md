> Source: https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/NutritionRecord

# NutritionRecord

Artifact: `androidx.health.connect:connect-client`. Added in [1.1.0](https://developer.android.com/jetpack/androidx/releases/health-connect#1.1.0).

```kotlin
class NutritionRecord : Record
```

Captures what nutrients were consumed as part of a meal or a food item.

## Summary

### Public companion properties

| Type | Property |
| --- | --- |
| AggregateMetric\<Mass\> | `BIOTIN_TOTAL` — Metric identifier to retrieve the total biotin from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `CAFFEINE_TOTAL` — Metric identifier to retrieve the total caffeine from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `CALCIUM_TOTAL` — Metric identifier to retrieve the total calcium from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `CHLORIDE_TOTAL` — Metric identifier to retrieve the total chloride from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `CHOLESTEROL_TOTAL` — Metric identifier to retrieve the total cholesterol from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `CHROMIUM_TOTAL` — Metric identifier to retrieve the total chromium from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `COPPER_TOTAL` — Metric identifier to retrieve the total copper from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `DIETARY_FIBER_TOTAL` — Metric identifier to retrieve the total dietary fiber from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Energy\> | `ENERGY_FROM_FAT_TOTAL` — Metric identifier to retrieve the total energy from fat from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Energy\> | `ENERGY_TOTAL` — Metric identifier to retrieve the total energy from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `FOLATE_TOTAL` — Metric identifier to retrieve the total folate from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `FOLIC_ACID_TOTAL` — Metric identifier to retrieve the total folic acid from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `IODINE_TOTAL` — Metric identifier to retrieve the total iodine from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `IRON_TOTAL` — Metric identifier to retrieve the total iron from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `MAGNESIUM_TOTAL` — Metric identifier to retrieve the total magnesium from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `MANGANESE_TOTAL` — Metric identifier to retrieve the total manganese from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `MOLYBDENUM_TOTAL` — Metric identifier to retrieve the total molybdenum from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `MONOUNSATURATED_FAT_TOTAL` — Metric identifier to retrieve the total monounsaturated fat from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `NIACIN_TOTAL` — Metric identifier to retrieve the total niacin from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `PANTOTHENIC_ACID_TOTAL` — Metric identifier to retrieve the total pantothenic acid from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `PHOSPHORUS_TOTAL` — Metric identifier to retrieve the total phosphorus from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `POLYUNSATURATED_FAT_TOTAL` — Metric identifier to retrieve the total polyunsaturated fat from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `POTASSIUM_TOTAL` — Metric identifier to retrieve the total potassium from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `PROTEIN_TOTAL` — Metric identifier to retrieve the total protein from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `RIBOFLAVIN_TOTAL` — Metric identifier to retrieve the total riboflavin from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `SATURATED_FAT_TOTAL` — Metric identifier to retrieve the total saturated fat from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `SELENIUM_TOTAL` — Metric identifier to retrieve the total selenium from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `SODIUM_TOTAL` — Metric identifier to retrieve the total sodium from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `SUGAR_TOTAL` — Metric identifier to retrieve the total sugar from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `THIAMIN_TOTAL` — Metric identifier to retrieve the total thiamin from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `TOTAL_CARBOHYDRATE_TOTAL` — Metric identifier to retrieve the total total carbohydrate from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `TOTAL_FAT_TOTAL` — Metric identifier to retrieve the total total fat from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `TRANS_FAT_TOTAL` — Metric identifier to retrieve the total trans fat from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `UNSATURATED_FAT_TOTAL` — Metric identifier to retrieve the total unsaturated fat from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `VITAMIN_A_TOTAL` — Metric identifier to retrieve the total vitamin a from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `VITAMIN_B12_TOTAL` — Metric identifier to retrieve the total vitamin b12 from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `VITAMIN_B6_TOTAL` — Metric identifier to retrieve the total vitamin b6 from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `VITAMIN_C_TOTAL` — Metric identifier to retrieve the total vitamin c from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `VITAMIN_D_TOTAL` — Metric identifier to retrieve the total vitamin d from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `VITAMIN_E_TOTAL` — Metric identifier to retrieve the total vitamin e from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `VITAMIN_K_TOTAL` — Metric identifier to retrieve the total vitamin k from `androidx.health.connect.client.aggregate.AggregationResult`. |
| AggregateMetric\<Mass\> | `ZINC_TOTAL` — Metric identifier to retrieve the total zinc from `androidx.health.connect.client.aggregate.AggregationResult`. |

### Public constructors

```kotlin
NutritionRecord(
    startTime: Instant,
    startZoneOffset: ZoneOffset?,
    endTime: Instant,
    endZoneOffset: ZoneOffset?,
    metadata: Metadata,
    biotin: Mass? = null,
    caffeine: Mass? = null,
    calcium: Mass? = null,
    energy: Energy? = null,
    energyFromFat: Energy? = null,
    chloride: Mass? = null,
    cholesterol: Mass? = null,
    chromium: Mass? = null,
    copper: Mass? = null,
    dietaryFiber: Mass? = null,
    folate: Mass? = null,
    folicAcid: Mass? = null,
    iodine: Mass? = null,
    iron: Mass? = null,
    // ...additional nutrient fields (see Public properties below)
)
```

### Public functions

| Signature | Description |
| --- | --- |
| `open operator fun equals(other: Any?): Boolean` | |
| `open fun hashCode(): Int` | |
| `open fun toString(): String` | |

### Public properties

| Type | Property | Description |
| --- | --- | --- |
| Mass? | `biotin` | Biotin in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `caffeine` | Caffeine in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `calcium` | Calcium in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `chloride` | Chloride in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `cholesterol` | Cholesterol in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `chromium` | Chromium in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `copper` | Copper in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `dietaryFiber` | Dietary fiber in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| open Instant | `endTime` | End time of the record. |
| open ZoneOffset? | `endZoneOffset` | User experienced zone offset at `endTime`, or null if unknown. |
| Energy? | `energy` | Energy in `Energy` unit. Optional field. Valid range: 0-100000 kcal. |
| Energy? | `energyFromFat` | Energy from fat in `Energy` unit. Optional field. Valid range: 0-100000 kcal. |
| Mass? | `folate` | Folate in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `folicAcid` | Folic acid in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `iodine` | Iodine in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `iron` | Iron in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `magnesium` | Magnesium in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `manganese` | Manganese in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Int | `mealType` | Type of meal related to the nutrients consumed. Optional, enum field. Allowed values: `MealType`. |
| open Metadata | `metadata` | Set of common metadata associated with the written record. |
| Mass? | `molybdenum` | Molybdenum in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `monounsaturatedFat` | Monounsaturated fat in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| String? | `name` | Name for food or drink, provided by the user. Optional field. |
| Mass? | `niacin` | Niacin in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `pantothenicAcid` | Pantothenic acid in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `phosphorus` | Phosphorus in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `polyunsaturatedFat` | Polyunsaturated fat in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `potassium` | Potassium in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `protein` | Protein in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `riboflavin` | Riboflavin in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `saturatedFat` | Saturated fat in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `selenium` | Selenium in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `sodium` | Sodium in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| open Instant | `startTime` | Start time of the record. |
| open ZoneOffset? | `startZoneOffset` | User experienced zone offset at `startTime`, or null if unknown. |
| Mass? | `sugar` | Sugar in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `thiamin` | Thiamin in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `totalCarbohydrate` | Total carbohydrate in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `totalFat` | Total fat in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `transFat` | Trans fat in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `unsaturatedFat` | Unsaturated fat in `Mass` unit. Optional field. Valid range: 0-100000 grams. |
| Mass? | `vitaminA` | Vitamin A in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `vitaminB12` | Vitamin B12 in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `vitaminB6` | Vitamin B6 in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `vitaminC` | Vitamin C in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `vitaminD` | Vitamin D in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `vitaminE` | Vitamin E in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `vitaminK` | Vitamin K in `Mass` unit. Optional field. Valid range: 0-100 grams. |
| Mass? | `zinc` | Zinc in `Mass` unit. Optional field. Valid range: 0-100 grams. |

Data notes: `endZoneOffset` and `startZoneOffset` — Providing these will help history aggregations results stay consistent should user travel. Queries with user experienced time filters will assume system current zone offset if the information is absent.
