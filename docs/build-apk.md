# Building the VoicePlus APK

These are the steps to build a testable APK from this branch.

## Prerequisites

- JDK 21 for the project toolchain
- Android SDK installed locally
- Network access to:
  - Google Maven
  - Maven Central
  - Foojay (used by Gradle for the daemon JVM if `gradle/gradle-daemon-jvm.properties` is present)

## Recommended build steps

From the repository root:

```bash
cd /home/runner/work/JDB3Fork_VoicePlus/JDB3Fork_VoicePlus
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew :app:assembleLibreRelease
```

## Expected output

Look in:

```bash
app/build/outputs/apk/libre/release/
```

## Signing behavior

- If `/home/runner/work/JDB3Fork_VoicePlus/JDB3Fork_VoicePlus/signing/keystore.properties` exists, the release build will be signed with that keystore.
- If that file does not exist, Gradle will still build a release APK, but it will not be signed for distribution.

## If Gradle complains about Java 25 / Azul Zulu

This repository includes `gradle/gradle-daemon-jvm.properties`, which pins the Gradle daemon to Azul Zulu Java 25.

Use one of these options:

1. Let Gradle download that daemon JDK automatically via Foojay.
2. Install a local Azul Zulu JDK 25.
3. If you only need a local test build, temporarily remove `gradle/gradle-daemon-jvm.properties` and rerun the build with JDK 21 active.

## Useful follow-up commands

Run the critical unit-test lane:

```bash
./gradlew criticalTest
```

Build a debug APK instead:

```bash
./gradlew :app:assembleLibreDebug
```
