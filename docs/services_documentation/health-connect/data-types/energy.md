> Source: https://developer.android.com/reference/kotlin/androidx/health/connect/client/units/Energy

# Energy

Artifact: `androidx.health.connect:connect-client`. Added in [1.1.0](https://developer.android.com/jetpack/androidx/releases/health-connect#1.1.0).

```kotlin
class Energy : Comparable
```

Represents a unit of energy. Supported units:

- calories — see `Energy.calories`, `Double.calories`
- kilocalories — see `Energy.kilocalories`, `Double.kilocalories`
- joules — see `Energy.joules`, `Double.joules`
- kilojoules — see `Energy.kilojoules`, `Double.kilojoules`

## Summary

### Public companion functions

| Signature | Description |
| --- | --- |
| `Energy calories(value: Double)` | Creates `Energy` with the specified value in calories. |
| `Energy joules(value: Double)` | Creates `Energy` with the specified value in joules. |
| `Energy kilocalories(value: Double)` | Creates `Energy` with the specified value in kilocalories. |
| `Energy kilojoules(value: Double)` | Creates `Energy` with the specified value in kilojoules. |

### Public functions

| Signature |
| --- |
| `open operator fun compareTo(other: Energy): Int` |
| `open operator fun equals(other: Any?): Boolean` |
| `open fun hashCode(): Int` |
| `open fun toString(): String` |

### Public properties

| Type | Property | Description |
| --- | --- | --- |
| Double | `inCalories` | Returns the energy in calories. |
| Double | `inJoules` | Returns the energy in joules. |
| Double | `inKilocalories` | Returns the energy in kilocalories. |
| Double | `inKilojoules` | Returns the energy in kilojoules. |
