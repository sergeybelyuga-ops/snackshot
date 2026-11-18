> Source: https://developer.android.com/reference/kotlin/androidx/health/connect/client/units/Mass

# Mass

Artifact: `androidx.health.connect:connect-client`. Added in [1.1.0](https://developer.android.com/jetpack/androidx/releases/health-connect#1.1.0).

```kotlin
class Mass : Comparable
```

Represents a unit of mass. Supported units:

- grams — see `Mass.grams`, `Double.grams`
- kilograms — see `Mass.kilograms`, `Double.kilograms`
- milligrams — see `Mass.milligrams`, `Double.milligrams`
- micrograms — see `Mass.micrograms`, `Double.micrograms`
- ounces — see `Mass.ounces`, `Double.ounces`
- pounds — see `Mass.pounds`, `Double.pounds`

## Summary

### Public companion functions

| Signature | Description |
| --- | --- |
| `Mass grams(value: Double)` | Creates `Mass` with the specified value in grams. |
| `Mass kilograms(value: Double)` | Creates `Mass` with the specified value in kilograms. |
| `Mass micrograms(value: Double)` | Creates `Mass` with the specified value in micrograms. |
| `Mass milligrams(value: Double)` | Creates `Mass` with the specified value in milligrams. |
| `Mass ounces(value: Double)` | Creates `Mass` with the specified value in ounces. |
| `Mass pounds(value: Double)` | Creates `Mass` with the specified value in pounds. |

### Public functions

| Signature |
| --- |
| `open operator fun compareTo(other: Mass): Int` |
| `open operator fun equals(other: Any?): Boolean` |
| `open fun hashCode(): Int` |
| `open fun toString(): String` |

### Public properties

| Type | Property | Description |
| --- | --- | --- |
| Double | `inGrams` | Returns the mass in grams. |
| Double | `inKilograms` | Returns the mass in kilograms. |
| Double | `inMicrograms` | Returns the mass in micrograms. |
| Double | `inMilligrams` | Returns the mass in milligrams. |
| Double | `inOunces` | Returns the mass in ounces. |
| Double | `inPounds` | Returns the mass in pounds. |
