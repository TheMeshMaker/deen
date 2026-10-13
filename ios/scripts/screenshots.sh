#!/bin/bash
# Takes screenshots of Deen in the iOS Simulator (used by .github/workflows/ios.yml).
# Usage: screenshots.sh <simulator udid> <path to Deen.app> <output folder>
# The app's demo mode (-DeenDemo …) fills in a sample place (Makkah) and saves nothing.
set -euo pipefail
DEVICE="$1"
APP="$2"
OUT="$3"
BUNDLE="io.github.themeshmaker.dunya"
mkdir -p "$OUT"

xcrun simctl boot "$DEVICE" 2>/dev/null || true
xcrun simctl bootstatus "$DEVICE" -b
xcrun simctl status_bar "$DEVICE" override --time "9:41" --batteryState charged --batteryLevel 100 \
  --cellularMode active --cellularBars 4 --wifiBars 3 || true
xcrun simctl install "$DEVICE" "$APP"

shot() {
  local name="$1"
  shift
  xcrun simctl terminate "$DEVICE" "$BUNDLE" 2>/dev/null || true
  xcrun simctl launch "$DEVICE" "$BUNDLE" "$@" >/dev/null
  sleep 7
  xcrun simctl io "$DEVICE" screenshot "$OUT/$name.png" >/dev/null
  echo "captured $name"
}

xcrun simctl ui "$DEVICE" appearance light
shot 01-welcome -DeenDemo onboarding
shot 02-today -DeenDemo today
shot 03-today-andalus -DeenDemo today -DeenTheme andalus
shot 04-more -DeenDemo more
shot 04b-quran -DeenDemo quran
shot 04c-reader -DeenDemo reader
shot 04d-hadith -DeenDemo hadith
shot 04e-adhkar -DeenDemo adhkar
shot 04f-qibla -DeenDemo qibla
shot 04g-timetable -DeenDemo timetable
shot 04h-tracker -DeenDemo tracker
shot 04i-calendar -DeenDemo calendar
shot 05-settings -DeenDemo settings
shot 06-today-arabic -DeenDemo today -AppleLanguages "(ar)" -AppleLocale ar_SA
shot 07-welcome-urdu -DeenDemo onboarding -AppleLanguages "(ur)" -AppleLocale ur_PK
shot 08-today-french -DeenDemo today -DeenTheme rose -AppleLanguages "(fr)" -AppleLocale fr_FR

xcrun simctl ui "$DEVICE" appearance dark
shot 09-today-dark -DeenDemo today -DeenTheme midnight
shot 10-settings-dark -DeenDemo settings -DeenTheme kiswah
shot 11-reader-dark -DeenDemo reader -DeenTheme midnight
