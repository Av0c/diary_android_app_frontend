
# WhereAreYou - Android GPS tracking app
This repository contains the source code for a project called "Diary", an Android forum-like app where users can register, post entries with option to attach media files. There's also a possibility to browse past entries.
The app is special in the aspect that each entry will include multiple metadata, such as IP addresses and locations, which could raise some concern over the privacy of whoever chose to use the application.
This project proposed by teacher Dr. Ghodrat Moghadampour and was developed in collaboration with Huy Pham, who provided the backend.

*Data is only recorded with user's consent.

## Application Features / Requirements:
Quoting the requirements:
> There will be two main activities in the Android app. The first activity's functionality is taking either a pictures, a video or a voice recording, or selecting an existing file, which can be of any type and submit it to the server with a text message; user can also submit the text message only. - The second activity allows user to search submitted posts based on multiple search conditions: text phrases in text messages / file name, time of submission, GPS coordinates and device IP address. The activity will contain for each search condition multiple input boxes or other input types (like a list of valid GPS coordinates) to make entering criteria and using the app faster. The result will be records that satisfies Boolean AND of all search conditions. A search condition will be excluded if its Input box is empty. Example if the user put file name "cat", time "26th Feb", the rest of boxes are empty, then the result would be all records on date 26th Feb that has "cat" as a sub-string in its file name.

Main features:
 - User authentication: login/logout, registering.
 - Submitting entries with message, and an optional attachment (picture, video, recording or any media file).
 - Display a past entries in cards in a scrollable view; Users can search past entries based on an advanced filtering system with multiple criteria.

## Installation

This project is developed in Android Studio.

 ### Running instructions
 - `git clone https://github.com/Av0c/diary_android_app_frontend.git`.
 - Open project in Android Studio: `File` > `Open` > Navigate to the folder above.
 - To run the app, make sure you have an Android device connected through USB (USB debug mode) or have setup an AVD (Android Virtual Device). (More details at official guide: [Run your app](https://developer.android.com/training/basics/firstapp/running-app))

It is also possible to build an .apk file to allow installation on another device.

 ### Required SDKs
This project targets Android API 29, with a minimum of API 26.
Make sure your Android Studio has the following SDKs installed (*Android Studio > Tools > SDK Manager*):
 - Android 8.0 Oreo (API 26) up to Android 10.0 Q (API 29).
 - Google Play Services (for locations).
 
 ## Architecture
The application contains 2 main activities:
 - LoginActivity - Activity to handle user login/registering.
 - MainActivity - Activity contains 2 fragments:
   - SubmitFragment - Fragment allowing user to submit new entry.
   - SearchFragment - Fragment where search results are displayed.
     - NewSearchFragment - Fragment handling inputs for search criteria.

A tabbed layout is implemented, user can switch between screens by swiping or pressing the desired tab in the tab selector.

More detailed information is available as code comments.
