# Call Whitelist

Android call-screening app with a local, flexible allowlist for incoming calls.

[Русская версия](README.ru.md)

## Features

- Allow calls from contacts and explicitly approved numbers.
- Configure default actions for unknown and hidden numbers.
- Enable schedules with time windows, including overnight intervals.
- Add temporary approved numbers for 1 hour, 24 hours, or 7 days.
- Temporarily disable all filtering with one switch.
- Review processed, allowed, and blocked calls in a local journal.
- Choose the overview period: today, 7 days, 30 days, or all time.
- Receive optional local notifications for blocked calls.
- Russian and English UI.
- Material 3 UI with standard Material icons.

## Privacy by design

The app is designed to work locally on the device:

- no account is required;
- no network service or analytics SDK is used;
- filtering rules, settings, and the app journal are stored locally;
- contact access is optional and is used only to recognize calls from saved contacts;
- the app does not request `READ_CALL_LOG` or `WRITE_CALL_LOG`.

The privacy policy and release licensing documents will be added before public distribution.

## Requirements

- Android Studio with the bundled JDK.
- Android SDK configured for the project.
- Minimum Android version: API 26.
- Kotlin, Jetpack Compose, Material 3, Room, and DataStore.

The app must be assigned as the system call-screening handler before it can make decisions about incoming calls. This is configured from the Policies screen.

## Build

From the project root on Windows:

```powershell
$env:JAVA_HOME = "C:\Users\User\AppData\Local\Programs\Android Studio\jbr"
.\gradlew.bat clean :app:assembleDebug
```

Run the domain tests:

```powershell
.\gradlew.bat :domain:test
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

```text
app/                  Compose UI, navigation, system integration
callfilter/           CallScreeningService and the call decision path
core/model/           Kotlin domain models
core/database/        Room entities, DAOs, and database provider
core/preferences/     DataStore preferences
core/designsystem/    Material 3 theme
data/                 Local data and snapshot stores
domain/               Pure filtering rules and unit tests
```

The project uses a Kotlin-first modular structure. Screens are separated into files, while the project avoids creating a separate Gradle module for every screen.

## Project status

The current development baseline includes filtering, schedules, local journal, notifications, temporary rules, and modularized Kotlin sources. Google Play publication preparation is tracked separately and is not part of the current implementation stage.

## License

An open-source license will be selected and added before the first public release.
