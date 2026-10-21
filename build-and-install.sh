#!/usr/bin/env bash
# Build Jadwal Sholat TV APK without Gradle using Android SDK bare tools + OpenJDK 17.
set -euo pipefail
cd "$(dirname "$0")"

[ -d /opt/homebrew/opt/openjdk@17 ] && export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export PATH="$JAVA_HOME/bin:$PATH"

SDK="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
[ -d /opt/homebrew/share/android-commandlinetools/build-tools/34.0.0 ] \
  && SDK=/opt/homebrew/share/android-commandlinetools

BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
B=build

echo "Compiling resources and manifest..."
rm -rf "$B" && mkdir -p "$B/gen" "$B/classes" "$B/dex"
"$BT/aapt2" compile --dir res -o "$B/res.zip"
"$BT/aapt2" link -o "$B/base.apk" -I "$JAR" --manifest AndroidManifest.xml --java "$B/gen" \
  -A assets --min-sdk-version 24 --target-sdk-version 34 "$B/res.zip"

echo "Compiling Java sources..."
javac -nowarn -Xlint:-options --release 11 -classpath "$JAR" -d "$B/classes" \
  $(find src "$B/gen" -name '*.java')

echo "Converting bytecode to DEX..."
"$BT/d8" --min-api 24 --lib "$JAR" --output "$B/dex" $(find "$B/classes" -name '*.class')

cp "$B/base.apk" "$B/unsigned.apk"
(cd "$B/dex" && zip -q -u ../unsigned.apk classes.dex)

echo "Aligning & signing APK..."
"$BT/zipalign" -f 4 "$B/unsigned.apk" "$B/aligned.apk"

KEYSTORE=debug.keystore
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass android -keypass android \
      -alias sholattv -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=sholattv" >/dev/null 2>&1
fi

"$BT/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:android --out "$B/jadwal-sholat-tv.apk" "$B/aligned.apk"
echo "✓ Berhasil build $B/jadwal-sholat-tv.apk ($(wc -c < "$B/jadwal-sholat-tv.apk") bytes)"

if [ "${1:-}" = "--build" ]; then
    exit 0
fi

# Install section if TV is reachable
TV_IP="${TV_IP:-192.168.3.11}"
if command -v adb >/dev/null 2>&1; then
    echo "Connecting to TV: $TV_IP:5555 ..."
    adb connect "$TV_IP:5555" || true
    echo "Installing to TV..."
    adb -s "$TV_IP:5555" install -r "$B/jadwal-sholat-tv.apk"
    echo "Launching app..."
    adb -s "$TV_IP:5555" shell "monkey -p com.ardith.sholattv -c android.intent.category.LAUNCHER 1"
    echo "✓ Selesai terpasang dan dijalankan di TV!"
fi
