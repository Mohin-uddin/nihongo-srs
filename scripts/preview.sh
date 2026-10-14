#!/usr/bin/env bash
# Runs inside the emulator step of the GitHub workflow: installs the app, records a
# walkthrough video while PreviewTour visits every screen, then pulls the results.
set -x
OUT=preview
mkdir -p "$OUT"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Pre-grant permissions so no system dialog covers the screens.
adb shell pm grant com.nihongo.srs android.permission.POST_NOTIFICATIONS || true
adb shell pm grant com.nihongo.srs android.permission.RECORD_AUDIO || true
# The emulator's launcher sometimes shows "isn't responding" over the app; hide such dialogs.
adb shell settings put secure anr_show_background 0 || true
adb shell settings put global hide_error_dialogs 1 || true
adb shell pm disable-user --user 0 com.google.android.apps.nexuslauncher || true

adb shell screenrecord --size 540x1170 --bit-rate 1000000 --time-limit 170 /sdcard/walkthrough.mp4 &
REC_PID=$!
sleep 1
adb shell am instrument -w -e class com.nihongo.srs.PreviewTour \
  com.nihongo.srs.test/androidx.test.runner.AndroidJUnitRunner | tee "$OUT/tour.log"
sleep 2
adb shell pkill -INT screenrecord || true
wait $REC_PID || true
sleep 2

adb pull /sdcard/Pictures/nihongo-preview/. "$OUT/" || true
adb pull /sdcard/walkthrough.mp4 "$OUT/walkthrough.mp4" || true
ls -la "$OUT"
# Never fail the job: a partial preview is still useful.
exit 0
