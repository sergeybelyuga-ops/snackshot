Source: https://developer.android.com/health-and-fitness/health-connect

# Health Connect: simplify connectivity between apps

Health and fitness apps record valuable data, including medical records. With permission from your users, you can unlock the full potential of this data by combining it to generate powerful insights.

## Health & Fitness data types

Health Connect stores and structures health and fitness data. It also accounts for how different data types are measured. These data types include heart rate measurements taken immediately, step count taken over time, and sleep data taken in sessions.

### Measure skin temperature

Health Connect provides a skin temperature data type to measure peripheral body temperature. This measurement is a particularly useful signal for detecting sleep quality, reproductive health, and the potential onset of illness.

### Add exercise routes

Exercise routes allow users to track a GPS route for associated exercise activities and share maps of their workouts with other apps.

### Track mindfulness

Health Connect provides a mindfulness data type to measure various aspects of mental health, such as stress and anxiety. Mindfulness is a data type that is part of overall wellness in Health Connect.

### Medical records

The Medical Records feature extends Health Connect to include basic medical data in the Fast Healthcare Interoperability Resources (FHIR®) format. It provides APIs for applications to read and write medical data, along with a user interface for browsing the data and managing permissions.

## Functionalities

### CRUD operations on record and data synchronization

The platform provides standard insert, update, and delete functions for recorded data. Health Connect also includes functionality that allows client apps to synchronize data out of Health Connect.

### Android compatibility

Health Connect is compatible with Android SDK version 28 (Pie) and higher. The SDK allows client apps to determine whether the Health Connect API is on the user's Android-powered device. If it isn't, Health Connect triggers an availability check to determine whether the user's device is compatible.

### Reading with Health Connect

Enable applications to continuously access a user's health data, even when the app is running in the background. This ensures uninterrupted access for analysis, synchronization, and utilization, offering a seamless health tracking experience.

## Videos

### Mo's Story: Starting Families

Learn how Health Connect can help users manage their reproductive health data.

### Introducing new APIs for health and fitness in Health Connect by Android

Learn about the new Health Connect APIs.

### Managing permissions in Health Connect

Learn how to manager permissions in Health Connect.

### Reading and writing in Health Connect

Learn how to read and write data in Health Connect.

### Tips for a great Health Connect integration

Need help integrating Health Connect with your app? Watch this video to learn some tips.

## News

### Health & Fitness Google Developer Newsletter May 2025

We shipped our first Health & Fitness Google Developer Newsletter! Learn about new features, updates, and community events in the newsletter.

### Unlock deeper health insights: Health Connect Jetpack SDK is now in beta and new feature updates

We're excited to announce significant Health Connect updates like the Jetpack SDK Beta, new datatypes and new permissions that will enable richer, more insightful app functionalities.

## Health Connect Jetpack Library 1.1.0 is now stable

We are excited to announce that the Health Connect Jetpack library has reached its 1.1.0 stable release. This milestone provides developers with the confidence and reliability needed to build production-ready health and fitness experiences at scale.

### Connect with us

Sign up for the Health & Fitness developer newsletter and get the latest news and updates straight to your inbox.
