#!/usr/bin/env bash

set -e
set -x

# Run local tests, should be more stable if no emulator is running
./gradlew build assembleAndroidTest test

# Run emulator tests only on master. We just have a few, so this should be fine
# [[ "${TRAVIS_BRANCH}" != "master" ]] && exit 0

# Fire up the emulator
emulator -avd test -no-audio -no-window &
./android-wait-for-emulator.sh
adb shell input keyevent 82 &
# This will check that local tests where executed and execute
# instrumentation tests with coverage report
./gradlew jacocoTestReport