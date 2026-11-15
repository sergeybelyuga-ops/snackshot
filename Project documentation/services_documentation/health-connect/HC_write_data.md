Source: https://developer.android.com/health-and-fitness/health-connect/write-data

# Write data

This guide is compatible with Health Connect version 1.1.0-alpha12.

This guide covers the process of writing or updating data in Health Connect.

## Handle zero values

Some data types like steps, distance, or calories might have a value of 0. Only write zero values when it reflects true inactivity while the user was wearing the device. Don't write zero values if the device wasn't worn, data is missing, or the battery died. In such cases, omit the record to avoid misleading data.

## Set up data structure

Before writing data, we need to set up the records first. For more than 50 data types, each have their respective structures. See the Jetpack reference for more details about the data types available.

### Basic records

The Steps data type in Health Connect captures the number of steps a user has taken between readings. Step counts represent a common measurement across health, fitness, and wellness platforms.

The following example shows how to set steps count data:

```kotlin
val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)
val stepsRecord = StepsRecord(
    count = 120,
    startTime = startTime,
    endTime = endTime,
    startZoneOffset = zoneOffset,
    endZoneOffset = zoneOffset,
    metadata = Metadata.autoRecorded(
        device = Device(type = Device.TYPE_WATCH)
    )
)
healthConnectClient.insertRecords(listOf(stepsRecord))
```

Note: Only write a zero value if the device was worn and no activity occurred. Don't write data if the device was off-body or the data is incomplete. For best practices on handling time zones, see the Time zone handling section.

### Records with units of measurement

Health Connect can store values along with their units of measurement to provide accuracy. One example is the Nutrition data type that is vast and comprehensive. It includes a wide variety of optional nutrient fields ranging from total carbohydrates to vitamins. Each data point represents the nutrients that were potentially consumed as part of a meal or food item.

In this data type, all of the nutrients are represented in units of Mass, while energy is represented in a unit of Energy.

The following example shows how to set nutrition data for a user who has eaten a banana:

```kotlin
val endTime = Instant.now()
val startTime = endTime.minus(Duration.ofMinutes(1))

val banana = NutritionRecord(
    name = "banana",
    energy = 105.0.kilocalories,
    dietaryFiber = 3.1.grams,
    potassium = 0.422.grams,
    totalCarbohydrate = 27.0.grams,
    totalFat = 0.4.grams,
    saturatedFat = 0.1.grams,
    sodium = 0.001.grams,
    sugar = 14.0.grams,
    vitaminB6 = 0.0005.grams,
    vitaminC = 0.0103.grams,
    startTime = startTime,
    endTime = endTime,
    startZoneOffset = ZoneOffset.UTC,
    endZoneOffset = ZoneOffset.UTC,
    metadata = Metadata.activelyRecorded(
        device = Device(type = Device.TYPE_PHONE)
    )
)
```

### Records with series data

Health Connect can store a list of series data. One example is the Heart Rate data type that captures a series of heartbeat samples detected between readings.

In this data type, the parameter samples is represented by a list of Heart Rate samples. Each sample contains a beatsPerMinute value and a time value.

The following example shows how to set heart rate series data:

```kotlin
val endTime = Instant.now()
val startTime = endTime.minus(Duration.ofMinutes(5))

val heartRateRecord = HeartRateRecord(
    startTime = startTime,
    startZoneOffset = ZoneOffset.UTC,
    endTime = endTime,
    endZoneOffset = ZoneOffset.UTC,
    samples = List(10) { index ->
        HeartRateRecord.Sample(
            time = startTime + Duration.ofSeconds(index.toLong()),
            beatsPerMinute = 100 + index.toLong(),
        )
    },
    metadata = Metadata.activelyRecorded(
        device = Device(type = Device.TYPE_WATCH)
    )
)
```

## Request permissions from the user

After creating a client instance, your app needs to request permissions from the user. Users must be allowed to grant or deny permissions at any time. To do so, create a set of permissions for the required data types. Make sure that the permissions in the set are declared in your Android manifest first.

```kotlin
val permissions =
    setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )
```

Use getGrantedPermissions() to see if your app already has the required permissions granted. If not, use createRequestPermissionResultContract() to request those permissions. This displays the Health Connect permissions screen.

```kotlin
val requestPermissionsLauncher = rememberLauncherForActivityResult(
    contract = PermissionController.createRequestPermissionResultContract()
) { grantedPermissions ->
    if (grantedPermissions.containsAll(permissions)) {
        coroutineScope.launch { snackbarHostState.showSnackbar("Permissions granted!") }
    } else {
        coroutineScope.launch { snackbarHostState.showSnackbar("Permissions denied.") }
    }
}
```

Because users can grant or revoke permissions at any time, your app needs to check for permissions every time before using them and handle scenarios where permission is lost.

## Write data

One of the common workflows in Health Connect is writing data. To add records, use insertRecords.

The following example shows how to write data inserting step counts:

```kotlin
val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime)
val stepsRecord = StepsRecord(
    count = 120,
    startTime = startTime,
    endTime = endTime,
    startZoneOffset = zoneOffset,
    endZoneOffset = zoneOffset,
    metadata = Metadata.autoRecorded(
        device = Device(type = Device.TYPE_WATCH)
    )
)
healthConnectClient.insertRecords(listOf(stepsRecord))
```

## Update data

If you need to change one or more records, especially when you need to sync your app datastore with data from Health Connect, you can update your data. There are two ways to update existing data which depends on the identifier used to find records.

### Metadata

It is worth examining the Metadata class first as this is necessary when updating data. On creation, each Record in Health Connect has a metadata field. The following properties are relevant to synchronization:

- id: Every Record in Health Connect has a unique id value. Health Connect automatically populates this when inserting a new record.
- lastModifiedTime: Every Record also keeps track of the last time the record was modified. Health Connect automatically populates this.
- clientRecordId: Each Record can have a unique ID associated with it to serve as reference in your app datastore. Your app supplies this value.
- clientRecordVersion: Where a record has clientRecordId, the clientRecordVersion can be used to allow data to stay in sync with the version in your app datastore. Your app supplies this value.

### Update after reading by time range

To update data, prepare the needed records first. Perform any changes to the records if necessary. Then, call updateRecords to make the changes.

The following example shows how to update data. For this purpose, each record has its zone offset values adjusted into PST.

```kotlin
suspend fun updateSteps(
    healthConnectClient: HealthConnectClient,
    prevRecordStartTime: Instant,
    prevRecordEndTime: Instant
) {
    try {
        val request = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(
                    prevRecordStartTime,
                    prevRecordEndTime
                )
            )
        )

        val newStepsRecords = arrayListOf<StepsRecord>()
        for (record in request.records) {
            val sr = StepsRecord(
                count = record.count,
                startTime = record.startTime,
                startZoneOffset = record.startTime.atZone(ZoneId.of("PST")).offset,
                endTime = record.endTime,
                endZoneOffset = record.endTime.atZone(ZoneId.of("PST")).offset,
                metadata = record.metadata
            )
            newStepsRecords.add(sr)
        }

        healthConnectClient.updateRecords(newStepsRecords)
    } catch (e: Exception) {
        // Run error handling here
    }
}
```

### Upsert through Client Record ID

If you are using the optional Client Record ID and Client Record Version values, we recommend using insertRecords instead of updateRecords.

The insertRecords function has the ability to upsert data. If the data exists in Health Connect based on the given set of Client Record IDs, it gets overwritten. Otherwise, it is written as new data. This scenario is useful whenever you need to sync data from your app datastore to Health Connect.

The following example shows how to perform an upsert on data pulled from the app datastore:

```kotlin
fun pullStepsFromDatastore(startTime: Instant, endTime: Instant) : ArrayList<StepsRecord> {
    val appStepsRecords = arrayListOf<StepsRecord>()
    // Pull data from app datastore
    // ...
    // Make changes to data if necessary
    // ...
    // Store data in appStepsRecords
    // ...
    var sr = StepsRecord(
        metadata = Metadata.activelyRecorded(
            clientRecordId = "Your client record ID",
            clientRecordVersion = 0L,
            device = Device(type = Device.TYPE_WATCH)
        ),
        startTime = startTime,
        startZoneOffset = startTime.atZone(ZoneId.of("PST")).offset,
        endTime = endTime,
        endZoneOffset = endTime.atZone(ZoneId.of("PST")).offset,
        count = 120
    )
    appStepsRecords.add(sr)
    return appStepsRecords
}

suspend fun upsertSteps(
    healthConnectClient: HealthConnectClient,
    newStepsRecords: ArrayList<StepsRecord>
) {
    try {
        healthConnectClient.insertRecords(newStepsRecords)
    } catch (e: Exception) {
        // Run error handling here
    }
}
```

#### Value check in Client Record Version

If your process of upserting data includes the Client Record Version, Health Connect performs comparison checks in the clientRecordVersion values. If the version from the inserted data is higher than the version from the existing data, the upsert happens. Otherwise, the process ignores the change and the value remains the same.

To include versioning in your data, you need to supply Metadata.clientRecordVersion with a Long value based on your versioning logic.

## General guidance

Your app should write all supported first-party data. Optionally, you can choose to have your app write data obtained from third-party sources. However, if your app has read data from Health Connect, that data shouldn't be written back into Health Connect.

When writing data that has been imported or derived from another source, you are expected to correctly attribute its origin and source device metadata. To do this, you must supply the following metadata for each written record:

- recordingMethod: For automatically or manually recorded data, we expect the recording method to be updated to reflect the type of activity that has been recorded.
- device.type: You are required to specify a device type from one of the supported Device types.
- device.manufacturer: The device's manufacturer, for example, "Fitbit".
- device.model: The device's model, for example, "Charge 3".

Setting metadata correctly is crucial for data transparency and helps users understand where their health information comes from. For complete details, refer to the Health Connect metadata guide.

### Time zone handling

When writing time-based records, avoid setting offsets to ZoneOffset.UTC by default because this can lead to inaccurate timestamps when users are in other zones. Instead, calculate the offset based on the device's actual location. You can retrieve the device's time zone using ZoneId.systemDefault().

```kotlin
val endTime = Instant.now()
val startTime = endTime.minus(Duration.ofDays(1))
val stepsRecords = mutableListOf<StepsRecord>()
var sampleTime = startTime
val minutesBetweenSamples = 15L
while (sampleTime < endTime) {
    val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(sampleTime)
    stepsRecords += StepsRecord(
        startTime = sampleTime.minus(Duration.ofMinutes(minutesBetweenSamples)),
        startZoneOffset = zoneOffset,
        endTime = sampleTime,
        endZoneOffset = zoneOffset,
        count = Random.nextLong(1, 100),
        metadata = Metadata.activelyRecorded(device = Device(type = Device.TYPE_WATCH)),
    )
    sampleTime = sampleTime.plus(Duration.ofMinutes(minutesBetweenSamples))
}
healthConnectClient.insertRecords(stepsRecords)
```

See the documentation for ZoneId for more details.

### Write frequency and granularity

When writing data to Health Connect, use appropriate resolution. Using the appropriate resolution helps reduce storage load, while still maintaining consistent and accurate data. Data resolution encompasses two things:

- Frequency of writes: How often your application writes any new data into Health Connect.
- Granularity of written data: How often the data was sampled.

Note: If your app relies on data from devices that sync less frequently than 15 minutes, adjust your writes to match the device's sync interval. This avoids empty writes to Health Connect.

### Additional guidelines

Follow these guidelines when writing data:

- On every sync, only write new data and updated data that was modified since the last sync.
- Chunk requests to at most 1000 records per write request.
- Restrict tasks to run only when the device is idle and is not low on battery.
- For background tasks, use WorkManager to schedule periodic tasks, with a maximum time period of 15 minutes.

### Active tracking

This includes apps that perform event-based tracking such as exercise and sleep, or manual user input such as nutrition. These records are created when the app is in the foreground, or in rare events where it is used a few times in a day.

Verify that your app doesn't keep Health Connect running for the entire duration of the event.

Data must be written to Health Connect in one of two ways:

- Sync data to Health Connect after the event is complete.
- Schedule a one-off task using WorkManager to sync data later.

## Best practices for granularity and frequency of writes

When writing data to Health Connect, use appropriate resolution. Using the appropriate resolution helps reduce storage load, while still maintaining consistent and accurate data. Data resolution encompasses 2 things:

1. Frequency of writes: how often your application pushes any new data into Health Connect. Write data as frequently as possible when new data is available, while being mindful of device performance. To avoid negatively impacting battery life and other performance aspects, the maximum interval between writes should be 15 minutes.
2. Granularity of written data: how often the data that is pushed in was sampled. For example, write heart rate samples every 5 seconds. Not every data type requires the same sample rate. There is little benefit to updating step count data every second, as opposed to a less frequent cadence such as every 60 seconds.

### Structure records for series data

For data types that use a series of samples, such as HeartRateRecord, it's important to structure your records correctly. Instead of creating a single, day-long record that is constantly updated, you should create multiple smaller records, each representing a specific time interval.

For example, for heart rate data, you should create a new HeartRateRecord for each minute. Each record would have a start time and end time spanning that minute, and would contain all the heart rate samples captured during that minute.

### Write data monitored throughout the day

For data collected on an ongoing basis, like steps, your application should write to Health Connect as frequently as possible when new data is available. To avoid negatively impacting battery life and other performance aspects, the maximum interval between writes should be 15 minutes.

## Handle exceptions

Health Connect throws standard exceptions for CRUD operations when an issue is encountered. Your app should catch and handle each of these exceptions as appropriate.

Each method on HealthConnectClient lists the exceptions that can be thrown. In general, your app should handle the following exceptions:

| Exception | Description | Recommended best practice |
| --- | --- | --- |
| IllegalStateException | One of the following scenarios has occurred: The Health Connect service isn't available. The request isn't a valid construction. | Handle possible issues with the inputs first before doing a request. |
| IOException | There are issues encountered upon reading and writing data from disk. | Back up user input and apply retries and backoff strategies. |
| RemoteException | Errors have occurred within, or in communicating with, the underlying service to which the SDK connects. | Perform regular syncs and apply retries and backoff strategies. |
| SecurityException | There are issues encountered when the requests require permissions that aren't granted. | Declare Health Connect permissions in the manifest and activity. |
