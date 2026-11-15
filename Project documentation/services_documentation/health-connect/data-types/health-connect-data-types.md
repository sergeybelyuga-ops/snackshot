> Source: https://developer.android.com/health-and-fitness/health-connect/data-types

# Health Connect data types

Health Connect stores and structures both health and fitness data and medical records data. It's important to understand first what data types and permissions Health Connect offers so that you can plan your app's requirements.

After development, when you prepare to publish your app to the Play Store, you must declare your app's data use as well as declare access to the Health Connect data types that your app uses. Otherwise, users might be prompted with an error message where your app can't access the Health Connect data types because they require special approval.

See [Complete the health apps declaration in the Play Console](https://developer.android.com/health-and-fitness/guides/health-connect/publish/declare-access) for more information.

## Data type categories

Health Connect supports data types that are used across most health and fitness apps to provide as much variety as possible. Health Connect aims to offer a comprehensive view and storage of health and fitness data. These data types fall into the following categories:

| Category | Description |
| --- | --- |
| Activity | This captures any activity that a user does. It can include health and fitness activities like running and swimming. |
| Body Measurement | This captures common data related to the body, such as a user's weight and their basal metabolic rate. |
| Cycle Tracking | This captures menstrual cycles and related data points, such as the binary result of an ovulation test. |
| Nutrition | This captures hydration and nutrition data types. The former represents how much water a user consume in a single drink. The latter includes optional fields such as calories, sugar, and magnesium. |
| Sleep | This captures interval data related to a user's length and type of sleep. |
| Vitals | This captures essential information about the user's general health. It includes data such as body temperature, blood glucose, blood pressure, and blood oxygen saturation. |
| Wellness | This captures data related to a user's mental health and well-being. |

## Developer experience guides

Health Connect provides developer experience guides that focus on specific health and fitness use cases. These guides show you how to use relevant data types together to build features for:

- [Sleep](https://developer.android.com/health-and-fitness/health-connect/experiences/sleep)
- [Vitals](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)
- [Workouts](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)

The following data type table also indicates which experience guides are associated with each data type.

## Data type fields

The Health Connect data types table includes several field columns to help you understand the structure and requirements of each data type. These columns are as follows:

| Column | Description |
| --- | --- |
| Record Type | Indicates whether the data is recorded at an instant in time or over an interval. The possible values are: Interval, Instantaneous, Series. |
| Unit | The unit of measurement for the data type (for example, Calories, Meters, Count). |
| Mandatory Fields | These are the essential fields that must be included when writing data for this data type to help maintain data integrity and usefulness. |

## Health and fitness data types

Before requesting any permissions, your app must declare them in the manifest. See the [Declare permissions](https://developer.android.com/health-and-fitness/guides/health-connect/develop/get-started#declare-permissions) section of the Quick start guide for more information.

To read data while your app is in the background, or to read historical data, an additional set of read permissions must be declared separately from data type permissions:

| Permission | Value |
| --- | --- |
| Read data in background ([Background read example guide](https://developer.android.com/health-and-fitness/health-connect/guides/read-data)) | `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` |
| Read historical data ([Read data older than 30 days guide](https://developer.android.com/health-and-fitness/health-connect/guides/read-data)) | `android.permission.health.READ_HEALTH_DATA_HISTORY` |

The following table contains the full list of data types, each with category, feature flags and guides, and permission declarations.

This table is for the Health Connect SDK version 1.0.0-alpha10 and higher. If you are using an older version of the SDK, see [Health Connect data types for SDK 1.0.0-alpha09 and lower](https://developer.android.com/health-and-fitness/health-connect/data-types-legacy).

40 results:

| Data type | Category | Record class / Permissions / Aggregate data types |
| --- | --- | --- |
| Active calories burned ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Interval. Unit: Energy. Mandatory Fields. | Activity | `ActiveCaloriesBurnedRecord`; `android.permission.health.READ_ACTIVE_CALORIES_BURNED`; `android.permission.health.WRITE_ACTIVE_CALORIES_BURNED`; Aggregate data types: `ACTIVE_CALORIES_TOTAL` |
| Activity intensity ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Interval. Mandatory Fields. | Activity | `ActivityIntensityRecord`; `android.permission.health.READ_ACTIVITY_INTENSITY`; `android.permission.health.WRITE_ACTIVITY_INTENSITY`; `FEATURE_ACTIVITY_INTENSITY`; Aggregate data types |
| Basal body temperature. Record Type: Instantaneous. Unit: Temperature. Mandatory Fields. | Cycle Tracking | `BasalBodyTemperatureRecord`; `android.permission.health.READ_BASAL_BODY_TEMPERATURE`; `android.permission.health.WRITE_BASAL_BODY_TEMPERATURE` |
| Basal metabolic rate. Record Type: Instantaneous. Unit: Power. Mandatory Fields. | Body Measurement | `BasalMetabolicRateRecord`; `android.permission.health.READ_BASAL_METABOLIC_RATE`; `android.permission.health.WRITE_BASAL_METABOLIC_RATE`; Aggregate data types: `BASAL_CALORIES_TOTAL` |
| Blood glucose ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Instantaneous. Mandatory Fields. | Vitals | `BloodGlucoseRecord`; `android.permission.health.READ_BLOOD_GLUCOSE`; `android.permission.health.WRITE_BLOOD_GLUCOSE` |
| Blood pressure ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Instantaneous. Unit: Pressure. Mandatory Fields. | Vitals | `BloodPressureRecord`; `android.permission.health.READ_BLOOD_PRESSURE`; `android.permission.health.WRITE_BLOOD_PRESSURE`; Aggregate data types |
| Body fat. Record Type: Instantaneous. Mandatory Fields. | Body Measurement | `BodyFatRecord`; `android.permission.health.READ_BODY_FAT`; `android.permission.health.WRITE_BODY_FAT` |
| Body temperature ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Instantaneous. Unit: Temperature. Mandatory Fields. | Vitals | `BodyTemperatureRecord`; `android.permission.health.READ_BODY_TEMPERATURE`; `android.permission.health.WRITE_BODY_TEMPERATURE` |
| Body water mass. Record Type: Instantaneous. Unit: Mass. Mandatory Fields. | Body Measurement | `BodyWaterMassRecord`; `android.permission.health.READ_BODY_WATER_MASS`; `android.permission.health.WRITE_BODY_WATER_MASS` |
| Bone mass. Record Type: Instantaneous. Unit: Mass. Mandatory Fields. | Body Measurement | `BoneMassRecord`; `android.permission.health.READ_BONE_MASS`; `android.permission.health.WRITE_BONE_MASS` |
| Cervical mucus. Record Type: Instantaneous. Mandatory Fields. | Cycle Tracking | `CervicalMucusRecord`; `android.permission.health.READ_CERVICAL_MUCUS`; `android.permission.health.WRITE_CERVICAL_MUCUS` |
| Cycling pedaling cadence ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Series. Mandatory Fields. | Activity | `CyclingPedalingCadenceRecord`; `android.permission.health.READ_EXERCISE`; `android.permission.health.WRITE_EXERCISE`; Aggregate data types |
| Distance ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Interval. Unit: Length. Mandatory Fields. | Activity | `DistanceRecord`; `android.permission.health.READ_DISTANCE`; `android.permission.health.WRITE_DISTANCE`; Aggregate data types: `DISTANCE_TOTAL` |
| Elevation gained ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Interval. Unit: Length. Mandatory Fields. | Activity | `ElevationGainedRecord`; `android.permission.health.READ_ELEVATION_GAINED`; `android.permission.health.WRITE_ELEVATION_GAINED`; Aggregate data types: `ELEVATION_GAINED_TOTAL` |
| Exercise ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts), [Add exercise routes guide](https://developer.android.com/health-and-fitness/health-connect/guides/exercise-routes)). Record Type: Interval. Mandatory Fields. | Activity | `ExerciseSessionRecord`; `android.permission.health.READ_EXERCISE`; `android.permission.health.READ_EXERCISE_ROUTE`; `android.permission.health.WRITE_EXERCISE`; `android.permission.health.WRITE_EXERCISE_ROUTE`; Exercise types; Aggregate data types: `EXERCISE_DURATION_TOTAL` |
| Floors climbed ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Interval. Mandatory Fields. | Activity | `FloorsClimbedRecord`; `android.permission.health.READ_FLOORS_CLIMBED`; `android.permission.health.WRITE_FLOORS_CLIMBED`; Aggregate data types: `FLOORS_CLIMBED_TOTAL` |
| Heart rate ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Series. Mandatory Fields. | Vitals | `HeartRateRecord`; `android.permission.health.READ_HEART_RATE`; `android.permission.health.WRITE_HEART_RATE`; Aggregate data types |
| Heart rate variability ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Instantaneous. Mandatory Fields. | Vitals | `HeartRateVariabilityRmssdRecord`; `android.permission.health.READ_HEART_RATE_VARIABILITY`; `android.permission.health.WRITE_HEART_RATE_VARIABILITY` |
| Height. Record Type: Instantaneous. Unit: Length. Mandatory Fields. | Body Measurement | `HeightRecord`; `android.permission.health.READ_HEIGHT`; `android.permission.health.WRITE_HEIGHT`; Aggregate data types |
| Hydration. Record Type: Interval. Unit: Volume. Mandatory Fields. | Nutrition | `HydrationRecord`; `android.permission.health.READ_HYDRATION`; `android.permission.health.WRITE_HYDRATION`; Aggregate data types: `VOLUME_TOTAL` |
| Intermenstrual bleeding. Record Type: Instantaneous. Mandatory Fields. | Cycle Tracking | `IntermenstrualBleedingRecord`; `android.permission.health.READ_INTERMENSTRUAL_BLEEDING`; `android.permission.health.WRITE_INTERMENSTRUAL_BLEEDING` |
| Lean body mass. Record Type: Instantaneous. Unit: Mass. Mandatory Fields. | Body Measurement | `LeanBodyMassRecord`; `android.permission.health.READ_LEAN_BODY_MASS`; `android.permission.health.WRITE_LEAN_BODY_MASS` |
| Menstruation. For `MenstruationFlowRecord`: Record Type: Instantaneous. Mandatory Fields. For `MenstruationPeriodRecord`: Record Type: Interval. Mandatory Fields. | Cycle Tracking | `MenstruationFlowRecord`; `MenstruationPeriodRecord`; `android.permission.health.READ_MENSTRUATION`; `android.permission.health.WRITE_MENSTRUATION` |
| Mindfulness ([Track mindfulness guide](https://developer.android.com/health-and-fitness/health-connect/guides/mindfulness)). Record Type: Interval. Mandatory Fields. | Wellness | `MindfulnessSessionRecord`; `android.permission.health.READ_MINDFULNESS`; `android.permission.health.WRITE_MINDFULNESS`; `FEATURE_MINDFULNESS_SESSION`; Mindfulness session types; Aggregate data types: `MINDFULNESS_DURATION_TOTAL` |
| Nutrition. Record Type: Interval. Unit: Mass. Mandatory Fields. | Nutrition | `NutritionRecord`; `android.permission.health.READ_NUTRITION`; `android.permission.health.WRITE_NUTRITION`; Aggregate data types |
| Ovulation test. Record Type: Instantaneous. Mandatory Fields. | Cycle Tracking | `OvulationTestRecord`; `android.permission.health.READ_OVULATION_TEST`; `android.permission.health.WRITE_OVULATION_TEST` |
| Oxygen saturation ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Instantaneous. Mandatory Fields. | Vitals | `OxygenSaturationRecord`; `android.permission.health.READ_OXYGEN_SATURATION`; `android.permission.health.WRITE_OXYGEN_SATURATION` |
| Planned exercise ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts), [Training plans guide](https://developer.android.com/health-and-fitness/health-connect/guides/training-plans)). Record Type: Interval. Mandatory Fields. | Activity | `PlannedExerciseSessionRecord`; `android.permission.health.READ_PLANNED_EXERCISE`; `android.permission.health.WRITE_PLANNED_EXERCISE`; `FEATURE_PLANNED_EXERCISE` |
| Power ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Series. Mandatory Fields. | Activity | `PowerRecord`; `android.permission.health.READ_POWER`; `android.permission.health.WRITE_POWER`; Aggregate data types |
| Respiratory rate ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Instantaneous. Mandatory Fields. | Vitals | `RespiratoryRateRecord`; `android.permission.health.READ_RESPIRATORY_RATE`; `android.permission.health.WRITE_RESPIRATORY_RATE` |
| Resting heart rate ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals)). Record Type: Instantaneous. Mandatory Fields. | Vitals | `RestingHeartRateRecord`; `android.permission.health.READ_RESTING_HEART_RATE`; `android.permission.health.WRITE_RESTING_HEART_RATE`; Aggregate data types |
| Sexual activity. Record Type: Instantaneous. Mandatory Fields. | Cycle Tracking | `SexualActivityRecord`; `android.permission.health.READ_SEXUAL_ACTIVITY`; `android.permission.health.WRITE_SEXUAL_ACTIVITY` |
| Skin temperature ([Vitals guide](https://developer.android.com/health-and-fitness/health-connect/experiences/vitals), [Measure skin temperature guide](https://developer.android.com/health-and-fitness/health-connect/guides/skin-temperature)). Record Type: Series. Unit: Temperature. Mandatory Fields. | Vitals | `SkinTemperatureRecord`; `android.permission.health.READ_SKIN_TEMPERATURE`; `android.permission.health.WRITE_SKIN_TEMPERATURE`; `FEATURE_SKIN_TEMPERATURE`; Aggregate data types |
| Sleep session ([Sleep guide](https://developer.android.com/health-and-fitness/health-connect/experiences/sleep), [Track sleep sessions guide](https://developer.android.com/health-and-fitness/health-connect/guides/sleep)). Record Type: Interval. Mandatory Fields. | Sleep | `SleepSessionRecord`; `android.permission.health.READ_SLEEP`; `android.permission.health.WRITE_SLEEP`; Aggregate data types: `SLEEP_DURATION_TOTAL` |
| Speed ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Series. Mandatory Fields. | Activity | `SpeedRecord`; `android.permission.health.READ_SPEED`; `android.permission.health.WRITE_SPEED`; Aggregate data types |
| Steps ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). For `StepsRecord`: Record Type: Interval. Mandatory Fields. For `StepsCadenceRecord`: Record Type: Series. Mandatory Fields. | Activity | `StepsRecord`; `StepsCadenceRecord`; `android.permission.health.READ_STEPS`; `android.permission.health.WRITE_STEPS`; Aggregate data types for `StepsRecord`: `COUNT_TOTAL`; Aggregate data types for `StepsCadenceRecord` |
| Total calories burned ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Interval. Unit: Energy. Mandatory Fields. | Activity | `TotalCaloriesBurnedRecord`; `android.permission.health.READ_TOTAL_CALORIES_BURNED`; `android.permission.health.WRITE_TOTAL_CALORIES_BURNED`; Aggregate data types: `ENERGY_TOTAL` |
| VO2 max ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Instantaneous. Mandatory Fields. | Activity | `Vo2MaxRecord`; `android.permission.health.READ_VO2_MAX`; `android.permission.health.WRITE_VO2_MAX` |
| Weight. Record Type: Instantaneous. Mandatory Fields. | Body Measurement | `WeightRecord`; `android.permission.health.READ_WEIGHT`; `android.permission.health.WRITE_WEIGHT`; Aggregate data types |
| Wheelchair pushes ([Workouts guide](https://developer.android.com/health-and-fitness/health-connect/experiences/workouts)). Record Type: Interval. Mandatory Fields. | Activity | `WheelchairPushesRecord`; `android.permission.health.READ_WHEELCHAIR_PUSHES`; `android.permission.health.WRITE_WHEELCHAIR_PUSHES`; Aggregate data types: `COUNT_TOTAL` |
