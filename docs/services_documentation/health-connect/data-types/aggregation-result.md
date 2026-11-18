> Source: https://developer.android.com/reference/kotlin/androidx/health/connect/client/aggregate/AggregationResult

# AggregationResult

Artifact: `androidx.health.connect:connect-client`. Added in [1.1.0](https://developer.android.com/jetpack/androidx/releases/health-connect#1.1.0).

```kotlin
class AggregationResult
```

Contains results of requested `AggregateMetric`.

To retrieve aggregate metrics:

```kotlin
val result = healthConnectClient.aggregate(
    metrics = setOf(Steps.COUNT_TOTAL, Distance.DISTANCE_TOTAL)
)
val totalSteps = result[Steps.COUNT_TOTAL]
val totalDistance = result[Distance.DISTANCE_TOTAL]
```

## Summary

### Public functions

| Signature | Description |
| --- | --- |
| `operator fun contains(metric: AggregateMetric<*>): Boolean` | Checks whether the aggregation result contains a metric or not. |
| `open operator fun equals(other: Any?): Boolean` | |
| `operator fun <T : Any> get(metric: AggregateMetric<T>): T?` | Retrieves a metric with given metric identifier. |
| `open fun hashCode(): Int` | |
| `open fun toString(): String` | |

### Public properties

| Type | Property | Description |
| --- | --- | --- |
| Set\<DataOrigin\> | `dataOrigins` | Set of `DataOrigin`s that contributed to the aggregation result. |

## Details

### contains

```kotlin
operator fun contains(metric: AggregateMetric<*>): Boolean
```

Checks whether the aggregation result contains a metric or not. If there is no relevant record that contribute to requested metric, the metric will not be provided.

| Parameter | Description |
| --- | --- |
| `metric: AggregateMetric<*>` | an aggregate metric identifier. |

Returns: `Boolean` — whether given metric is set.

### get

```kotlin
operator fun <T : Any> get(metric: AggregateMetric<T>): T?
```

Retrieves a metric with given metric identifier.

If there are no relevant records contributing to the requested metric, the metric will not be provided.

Returns: `T?` — the value of the metric, or null if not set.

See also: `contains`

### dataOrigins

```kotlin
val dataOrigins: Set<DataOrigin>
```

Set of `DataOrigin`s that contributed to the aggregation result.
