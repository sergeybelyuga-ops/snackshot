Source: https://developer.android.com/health-and-fitness/health-connect/read-data

# Read raw data

The following example shows you how to read raw data as part of the common workflow.

## Read data

Health Connect allows apps to read data from the datastore when the app is in the foreground and background:

- Foreground reads: You can normally read data from Health Connect when your app is in the foreground. In these cases, you may consider using a foreground service to run this operation in case the user or system places your app in the background during a read operation.
- Background reads: By requesting an extra permission from the user, you can read data after the user or system places your app in the background.

The Steps data type in Health Connect captures the number of steps a user has taken between readings. Step counts represent a common measurement across health, fitness, and wellness platforms. Health Connect lets you read and write step count data.

To read records, create a ReadRecordsRequest and supply it when you call readRecords.

Note: For cumulative types like StepsRecord, use aggregate() instead of readRecords() to avoid double counting from multiple sources and improve accuracy.

```kotlin
val response = healthConnectClient.readRecords(
    ReadRecordsRequest(
        HeartRateRecord::class,
        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
    )
)
response.records.forEach { record ->
    /* Process records */
}
```

You can also read your data in an aggregated manner using aggregate.

```kotlin
suspend fun readStepsAggregate(startTime: Instant, endTime: Instant): Long {
    val response = healthConnectClient.aggregate(
        AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
    )
    return response[StepsRecord.COUNT_TOTAL] ?: 0L
}
```

Important: Health Connect can read data for up to 30 days prior to the time permission was granted. If you would like your app to read records beyond 30 days, use the PERMISSION_READ_HEALTH_DATA_HISTORY permission.

## Read mobile steps

### Attribution change for on-device steps

Starting with the June 2026 update, steps tracked natively by Health Connect are attributed to a Synthetic Package Name (SPN), such as com.android.healthconnect.phone.jd5bdd37e1a8d3667a05d0abebfc4a89e.

Previously, built-in steps were attributed to the package name android. Historical step data recorded before June 2026 retains the android package name.

SPNs are device-specific and scoped on a per-application basis to protect user privacy:

- Stable: The SPN for the current device is stable for your application.
- Application-Scoped: Different applications on the same device see different SPNs for on-device step data.

#### Query for on-device steps

Because SPNs are scoped and device-specific, you must not hardcode SPN values. Instead, use the getCurrentDeviceDataSource() API to retrieve the SPN for the current device.

The following example shows how to read aggregated on-device step count data by filtering for both android and the current device SPN:

```kotlin
import android.content.Context
import android.health.connect.HealthConnectManager
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

suspend fun readDeviceStepsByTimeRange(
    healthConnectClient: HealthConnectClient,
    context: Context,
    startTime: Instant,
    endTime: Instant
) {
    val isDataSourceApiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.U &&
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.U) >= 11

    try {
        val healthConnectManager = context.getSystemService(HealthConnectManager::class.java)
        val currentDeviceSpn = if (isDataSourceApiAvailable) {
            healthConnectManager?.getCurrentDeviceDataSource()?.deviceDataOrigin?.packageName
        } else {
            null
        }

        val dataOriginFilters = mutableSetOf(DataOrigin("android"))
        currentDeviceSpn?.let {
            dataOriginFilters.add(DataOrigin(it))
        }

        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                dataOriginFilter = dataOriginFilters
            )
        )

        val stepCount = response[StepsRecord.COUNT_TOTAL]
    } catch (e: Exception) {
        // Handle error
    }
}
```

### On-Device Step Counting

- Sensor Usage: Health Connect utilizes the TYPE_STEP_COUNTER sensor from SensorManager. This sensor is optimized for low power consumption, making it ideal for continuous background step tracking.
- Data Granularity: To conserve battery life, step data is typically batched and written to the Health Connect database no more frequently than once per minute.
- Attribution: Steps recorded by this feature before June 2026 are attributed to the android package name in the DataOrigin. After this date, they are attributed to a device-specific SPN.
- Activation: The on-device step counting mechanism is active only when at least one application on the device has been granted the READ_STEPS permission within Health Connect.

With Android 14 (API level 34) and SDK Extension version 20 or higher, Health Connect provides on-device step counting. If any app has been granted the READ_STEPS permission, Health Connect starts capturing steps from the Android-powered device, and users see steps data automatically added to Health Connect Steps entries.

To check if on-device step counting is available, verify that the device is running Android 14 (API level 34) and has at least SDK extension version 20:

```kotlin
val isStepTrackingAvailable =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 20
```

If your app reads aggregated step counts using aggregate and doesn't filter by DataOrigin, on-device steps are automatically included in the total, and no changes are required for the June 2026 update.

## Background read example

To read data in the background, declare the following permission in your manifest file:

```xml
<application>
  <uses-permission android:name="android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND" />
</application>
```

The following example shows how to read step count data in the background for a user within a certain time by using WorkManager:

```kotlin
class ScheduleWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val healthConnectClient = HealthConnectClient.getOrCreate(applicationContext)
        // Perform background read logic here
        return Result.success()
    }
}

fun enqueueBackgroundReadWorker(context: Context, healthConnectClient: HealthConnectClient) {
    if (healthConnectClient
            .features
            .getFeatureStatus(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
            ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    ) {

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ScheduleWorker>(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "read_health_connect",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}
```

Note: If the user doesn't grant all of the permissions that are required for background reads, your app should still run, and it should perform as many tasks as it can with the permissions that the user granted.

The ReadRecordsRequest parameter has a default pageSize value of 1000. If the number of records in a single readResponse exceeds the pageSize of the request, you need to iterate over all pages of the response to retrieve all records by using pageToken. However, be careful to avoid rate-limiting concerns.

## pageToken read example

It is recommended to use pageToken for reading records to retrieve all available data from the requested time period.

Note: Depending on the specific system or Health Connect APK version, ReadRecordsResponse.pageToken can return an empty string "" instead of null when pagination is complete. To reliably check whether pagination is complete, use isNullOrEmpty() rather than checking only for null.

```kotlin
val type = HeartRateRecord::class
val endTime = Instant.now()
val startTime = endTime.minus(Duration.ofDays(7))

try {
    var pageToken: String? = null
    do {
        val readResponse =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = type,
                    timeRangeFilter = TimeRangeFilter.between(
                        startTime,
                        endTime
                    ),
                    pageToken = pageToken
                )
            )
        val records = readResponse.records
        // Do something with records
        pageToken = readResponse.pageToken
    } while (pageToken != null)
} catch (quotaError: IllegalStateException) {
    // Backoff
}
```

## Read previously written data

If an app has written records to Health Connect before, it is possible for that app to read historical data. This is applicable for scenarios in which the app needs to resync with Health Connect after the user has reinstalled it.

Some read restrictions apply:

- For Android 14 and higher
  - No historical limit on an app reading its own data.
  - 30-day limit on an app reading other data.
- For Android 13 and lower
  - 30-day limit on app reading any data.

To read historical data, you need to indicate the package name as a DataOrigin object in the dataOriginFilter parameter of your ReadRecordsRequest.

```kotlin
try {
    val response = healthConnectClient.readRecords(
        ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
            dataOriginFilter = setOf(DataOrigin("com.my.package.name"))
        )
    )
    for (record in response.records) {
        // Process each record
    }
} catch (e: Exception) {
    // Run error handling here
}
```

## Read data older than 30 days

By default, all applications can read data from Health Connect for up to 30 days prior to when any permission was first granted.

If you need to extend read permissions beyond any of the default restrictions, request the PERMISSION_READ_HEALTH_DATA_HISTORY permission. Otherwise, without this permission, an attempt to read records older than 30 days results in an error.

### Permissions history for a deleted app

If a user deletes your app, all permissions, including the history permission, are revoked. If the user reinstalls your app and grants permission again, the same default restrictions apply, and your app can read data from Health Connect for up to 30 days prior to that new date.

## Handle exceptions

Health Connect throws standard exceptions for CRUD operations when an issue is encountered. Your app should catch and handle each of these exceptions as appropriate.

Each method on HealthConnectClient lists the exceptions that can be thrown. In general, your app should handle the following exceptions:

| Exception | Description | Recommended best practice |
| --- | --- | --- |
| IllegalStateException | One of the following scenarios has occurred: The Health Connect service isn't available. The request isn't a valid construction. | Handle possible issues with the inputs first before doing a request. |
| IOException | There are issues encountered upon reading and writing data from disk. | Back up user input and apply retries and backoff strategies. |
| RemoteException | Errors have occurred within, or in communicating with, the underlying service to which the SDK connects. | Perform regular syncs between your app's datastore and Health Connect. |
| SecurityException | There are issues encountered when the requests require permissions that aren't granted. | Make sure that you've declared use of Health Connect data types and permissions. |
