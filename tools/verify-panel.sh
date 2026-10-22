#!/usr/bin/env bash
# Verifikasi widget multi-panel: set config -> restart app -> dump teks layar TV.
set -euo pipefail
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
DEV=192.168.3.11:5555
MODE="${1:-prayer}"

cd "$(dirname "$0")/.."
python3 tools/set-panel.py "$MODE"

"$ADB" -s "$DEV" shell am force-stop com.ardith.sholattv
sleep 2
"$ADB" -s "$DEV" shell "monkey -p com.ardith.sholattv -c android.intent.category.LAUNCHER 1" >/dev/null 2>&1
sleep 9
"$ADB" -s "$DEV" shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1
"$ADB" -s "$DEV" shell cat /sdcard/u.xml \
  | tr '>' '\n' \
  | grep -o 'text="[^"]*"' \
  | sed 's/text="//; s/"$//' \
  | grep -v '^$'
