#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ADB_BIN="${ADB_BIN:-$HOME/Library/Android/sdk/platform-tools/adb}"
EMULATOR_BIN="${EMULATOR_BIN:-$HOME/Library/Android/sdk/emulator/emulator}"
DEVICE_SERIAL="${DEVICE_SERIAL:-emulator-5554}"
AVD_NAME="${AVD_NAME:-OpenClaw_TV_API34}"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"

cd "$ROOT_DIR"

"$ADB_BIN" start-server >/dev/null

device_state="$("$ADB_BIN" -s "$DEVICE_SERIAL" get-state 2>/dev/null || true)"
if [ "$device_state" != "device" ]; then
  echo "[deploy] device $DEVICE_SERIAL unavailable, starting AVD $AVD_NAME..."
  nohup "$EMULATOR_BIN" -avd "$AVD_NAME" -no-snapshot-load >/tmp/bilitv_emulator.log 2>&1 &

  for _ in {1..120}; do
    state="$("$ADB_BIN" -s "$DEVICE_SERIAL" get-state 2>/dev/null || true)"
    if [ "$state" = "device" ]; then
      booted="$("$ADB_BIN" -s "$DEVICE_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
      if [ "$booted" = "1" ]; then
        echo "[deploy] emulator boot completed"
        break
      fi
    fi
    sleep 2
  done
fi

./gradlew --console=plain --offline --no-daemon -Dorg.gradle.vfs.watch=false :app:assembleDebug

if ! unzip -t "$APK_PATH" >/tmp/bilitv_apk_verify.log 2>&1; then
  echo "[deploy] detected corrupted apk output, rebuilding from clean..."
  ./gradlew --console=plain --offline --no-daemon -Dorg.gradle.vfs.watch=false clean :app:assembleDebug
  unzip -t "$APK_PATH" >/tmp/bilitv_apk_verify.log 2>&1
fi

"$ADB_BIN" -s "$DEVICE_SERIAL" install -r "$APK_PATH"
"$ADB_BIN" -s "$DEVICE_SERIAL" shell am start -n com.openclaw.bilitv/.MainActivity
