Source: https://developer.android.com/health-and-fitness/health-connect/data-types

# Health Connect data types

Health Connect stores and structures both health and fitness data and medical records data. It's important to understand first what data types and permissions Health Connect offers so that you can plan your app's requirements.

After development, when you prepare to publish your app to the Play Store, you must declare your app's data use as well as declare access to the Health Connect data types that your app uses. Otherwise, users might be prompted with an error message where your app can't access the Health Connect data types because they require special approval.

## Data type categories

Health Connect supports data types that are used across most health and fitness apps to provide as much variety as possible. Health Connect aims to offer a comprehensive view and storage of health and fitness data. These data types fall into the following categories:

- Activity
- Body Measurement
- Cycle Tracking
- Nutrition
- Sleep
- Vitals
- Wellness

## Developer experience guides

Health Connect provides developer experience guides that focus on specific health and fitness use cases. These guides show you how to use relevant data types together to build features for:

- Sleep
- Vitals
- Workouts

## Data type fields

The Health Connect data types table includes several field columns to help you understand the structure and requirements of each data type. These columns are as follows:

- Record Type: Indicates whether the data is recorded at an instant in time or over an interval. The possible values are: Interval, Instantaneous, Series.
- Unit: The unit of measurement for the data type (for example, Calories, Meters, Count).
- Mandatory Fields: These are the essential fields that must be included when writing data for this data type to help maintain data integrity and usefulness.

## Health and fitness data types

Before requesting any permissions, your app must declare them in the manifest. See the Declare permissions section of the Quick start guide for more information.

To read data while your app is in the background, or to read historical data, an additional set of read permissions must be declared separately from data type permissions:

- Read data in background
- Read historical data

This table is for the Health Connect SDK version 1.0.0-alpha10 and higher. If you are using an older version of the SDK, see Health Connect data types for SDK 1.0.0-alpha09 and lower.

The following data types are supported:

- Active calories burned
- Activity intensity
- Basal body temperature
- Basal metabolic rate
- Blood glucose
- Blood pressure
- Body fat
- Body temperature
- Body water mass
- Bone mass
- Cervical mucus
- Cycling pedaling cadence
- Distance
- Elevation gained
- Exercise
- Floors climbed
- Heart rate
- Heart rate variability
- Height
- Hydration
- Intermenstrual bleeding
- Lean body mass
- Menstruation
- Mindfulness
- Nutrition
- Ovulation test
- Oxygen saturation
- Planned exercise
- Power
- Respiratory rate
- Resting heart rate
- Sexual activity
- Skin temperature
- Sleep session
- Speed
- Steps
- Total calories burned
- VO2 max
- Weight
- Wheelchair pushes

Each record type has associated permissions and optional aggregate data types.
