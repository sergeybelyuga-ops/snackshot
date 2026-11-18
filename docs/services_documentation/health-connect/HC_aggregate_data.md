Source: https://developer.android.com/health-and-fitness/health-connect/aggregate-data

# Read aggregated data

Aggregating data in Health Connect includes basic aggregations or aggregating data into buckets. The following workflows show you how to do both.

## Basic aggregation

To use basic aggregation on your data, use the aggregate function on your HealthConnectClient object. It accepts an AggregateRequest object where you add the metric types and the time range as its parameters. How basic aggregates are called depends on the metric types used.

### Cumulative aggregation

Cumulative aggregation computes the total value.

The following example shows you how to aggregate data for a data type:

```kotlin
suspend fun readDistanceAggregate(startTime: Instant, endTime: Instant): Number {
    val response = healthConnectClient.aggregate(
        AggregateRequest(
            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
    )
    return response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0L
}
```

### Filter by data origin

You can also filter aggregate data by its origin. For example, only including data written by a specific app.

The following example shows how to use dataOriginFilter and AggregateRequest to aggregate steps from a specific app:

```kotlin
suspend fun aggregateStepsFromSpecificApp(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant,
    appPackageName: String
) {
    try {
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                dataOriginFilter = setOf(DataOrigin(appPackageName))
            )
        )
        val totalSteps = response[StepsRecord.COUNT_TOTAL] ?: 0L
    } catch (e: Exception) {
        // Run error handling here
    }
}
```

### Statistical aggregation

Statistical aggregation computes the minimum, maximum, or average values of records with samples.

The following example shows how to use statistical aggregation:

```kotlin
suspend fun readHeartRateAggregate(startTime: Instant, endTime: Instant): Pair<Long, Long> {
    val response = healthConnectClient.aggregate(
        AggregateRequest(
            metrics = setOf(HeartRateRecord.BPM_MAX, HeartRateRecord.BPM_MIN),
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
    )
    val minimumHeartRate = response[HeartRateRecord.BPM_MIN] ?: 0L
    val maximumHeartRate = response[HeartRateRecord.BPM_MAX] ?: 0L

    return maximumHeartRate to minimumHeartRate
}
```

## Buckets

Health Connect can also let you aggregate data into buckets. The two types of buckets you can use include duration and period.

Once called, they return a list of buckets. Note that the list can be sparse, so a bucket is not included in the list if it doesn't contain any data.

### Duration

In this case, aggregated data is split into buckets within a fixed length of time, such as a minute or an hour. To aggregate data into buckets, use aggregateGroupByDuration. It accepts an AggregateGroupByDurationRequest object where you add the metric types, the time range, and the Duration as parameters.

```kotlin
suspend fun aggregateStepsIntoMinutes(
    healthConnectClient: HealthConnectClient,
    startTime: LocalDateTime,
    endTime: LocalDateTime
) {
    try {
        val response =
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    timeRangeSlicer = Duration.ofMinutes(1L)
                )
            )
        for (durationResult in response) {
            val totalSteps = durationResult.result[StepsRecord.COUNT_TOTAL] ?: 0L
        }
    } catch (e: Exception) {
        // Run error handling here
    }
}
```

### Period

In this case, aggregated data is split into buckets within a date-based amount of time, such as a week or a month. To aggregate data into buckets, use aggregateGroupByPeriod. It accepts an AggregateGroupByPeriodRequest object where you add the metric types, the time range, and the Period as parameters.

```kotlin
suspend fun aggregateStepsIntoMonths(
    healthConnectClient: HealthConnectClient,
    startTime: LocalDateTime,
    endTime: LocalDateTime
) {
    try {
        val response =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            )
        for (monthlyResult in response) {
            val totalSteps = monthlyResult.result[StepsRecord.COUNT_TOTAL] ?: 0L
        }
    } catch (e: Exception) {
        // Run error handling here
    }
}
```

Note: When running bucket aggregates per period, make sure that the start and end times set are not using the Instant class. Otherwise, the app returns an IllegalStateException.

## Read restrictions

By default, all applications can read data from Health Connect for up to 30 days prior to when any permission was first granted.

If you need to extend read permissions beyond any of the default restrictions, request the PERMISSION_READ_HEALTH_DATA_HISTORY permission. Otherwise, without this permission, an attempt to read records older than 30 days results in an error.

### Permissions history for a deleted app

If a user deletes your app, all permissions, including the history permission, are revoked. If the user reinstalls your app and grants permission again, the same default restrictions apply, and your app can read data from Health Connect for up to 30 days prior to that new date.

## Aggregate data affected by user-selected apps priorities

End users can set priority for the Sleep and Activity apps that they have integrated with Health Connect. Only end users can alter these priority lists. When you perform an aggregate read, the Aggregate API accounts for any duplicate data and keeps only the data from the app with the highest priority. Duplicate data could exist if the user has multiple apps writing the same kind of data at the same time.

Even though the Aggregate API calculates Activity and Sleep apps' data by deduping data according to how the user has set priorities, you can still build your own logic to calculate the data separately for each app writing that data.

Only the Activity and Sleep data types are deduped by Health Connect, and the data totals shown are the values after the dedupe has been performed by the Aggregate API.

## Background reads

You can request that your application run in the background and read data from Health Connect. If you request the Background Read permission, your user can grant your app access to read data in the background.

## Supported aggregate data types by record

This table lists all the supported aggregate data types by Health Connect record.

| Record | Aggregate metric(s) |
| --- | --- |
| ActiveCaloriesBurnedRecord | ACTIVE_CALORIES_TOTAL |
| ActivityIntensityRecord | DURATION_TOTAL, INTENSITY_MINUTES_TOTAL, MODERATE_DURATION_TOTAL, VIGOROUS_DURATION_TOTAL |
| BasalMetabolicRateRecord | BASAL_CALORIES_TOTAL |
| BloodPressureRecord | DIASTOLIC_AVG, DIASTOLIC_MAX, DIASTOLIC_MIN, SYSTOLIC_AVG, SYSTOLIC_MAX, SYSTOLIC_MIN |
| CyclingPedalingCadenceRecord | RPM_AVG, RPM_MAX, RPM_MIN |
| DistanceRecord | DISTANCE_TOTAL |
| ElevationGainedRecord | ELEVATION_GAINED_TOTAL |
| ExerciseSessionRecord | EXERCISE_DURATION_TOTAL |
| FloorsClimbedRecord | FLOORS_CLIMBED_TOTAL |
| HeartRateRecord | BPM_AVG, BPM_MAX, BPM_MIN, MEASUREMENTS_COUNT |
| HeightRecord | HEIGHT_AVG, HEIGHT_MAX, HEIGHT_MIN |
| HydrationRecord | VOLUME_TOTAL |
| MindfulnessSessionRecord | MINDFULNESS_DURATION_TOTAL |
| NutritionRecord | BIOTIN_TOTAL, CAFFEINE_TOTAL, CALCIUM_TOTAL, CHLORIDE_TOTAL, CHOLESTEROL_TOTAL, CHROMIUM_TOTAL, COPPER_TOTAL, DIETARY_FIBER_TOTAL, ENERGY_FROM_FAT_TOTAL, ENERGY_TOTAL, FOLATE_TOTAL, FOLIC_ACID_TOTAL, IODINE_TOTAL, IRON_TOTAL, MAGNESIUM_TOTAL, MANGANESE_TOTAL, MOLYBDENUM_TOTAL, MONOUNSATURATED_FAT_TOTAL, NIACIN_TOTAL, PANTOTHENIC_ACID_TOTAL, PHOSPHORUS_TOTAL, POLYUNSATURATED_FAT_TOTAL, POTASSIUM_TOTAL |
| PowerRecord | POWER_AVG, POWER_MAX, POWER_MIN |
| RestingHeartRateRecord | BPM_AVG, BPM_MAX, BPM_MIN |
| SkinTemperatureRecord | TEMPERATURE_DELTA_AVG, TEMPERATURE_DELTA_MAX, TEMPERATURE_DELTA_MIN |
| SleepSessionRecord | SLEEP_DURATION_TOTAL |
| SpeedRecord | SPEED_AVG, SPEED_MAX, SPEED_MIN |
| StepsRecord | COUNT_TOTAL |
| StepsCadenceRecord | RATE_AVG, RATE_MAX, RATE_MIN |
| TotalCaloriesBurnedRecord | ENERGY_TOTAL |
| WeightRecord | WEIGHT_AVG, WEIGHT_MAX, WEIGHT_MIN |
| WheelchairPushesRecord | COUNT_TOTAL |
