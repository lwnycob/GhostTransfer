#!/bin/bash
set -e

ROOT="app/src/main"
BUILD="build"
ANDROID_JAR="/usr/lib/android-sdk/platforms/android-23/android.jar"
PKG="com/nyukob/ghosttransfer"

echo "=== Ghost Transfer APK Builder ==="

rm -rf "$BUILD"
mkdir -p "$BUILD/gen/$PKG" "$BUILD/classes" "$BUILD/apk"

echo "[1/6] Компилирую ресурсы (aapt)..."
aapt package -f -m \
    -J "$BUILD/gen" \
    -M "$ROOT/AndroidManifest.xml" \
    -S "$ROOT/res" \
    -I "$ANDROID_JAR"
echo "      ✓ R.java"

echo "[2/6] Компилирую Java → .class..."
find "$ROOT/java" -name "*.java" > /tmp/sources.txt
echo "$BUILD/gen/$PKG/R.java" >> /tmp/sources.txt
javac --release 8 -cp "$ANDROID_JAR" -d "$BUILD/classes" @/tmp/sources.txt
echo "      ✓ .class"

echo "[3/6] .class → .dex (dalvik-exchange)..."
dalvik-exchange --dex --output="$BUILD/apk/classes.dex" "$BUILD/classes/"
echo "      ✓ classes.dex"

echo "[4/6] Упаковываю APK..."
aapt package -f \
    -M "$ROOT/AndroidManifest.xml" \
    -S "$ROOT/res" \
    -I "$ANDROID_JAR" \
    -F "$BUILD/apk/ghost_transfer_unsigned.apk"
cd "$BUILD/apk" && zip -u ghost_transfer_unsigned.apk classes.dex && cd ../..
echo "      ✓ APK"

echo "[5/6] Создаю keystore..."
keytool -genkey -v -keystore "$BUILD/debug.jks" -alias debugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android \
    -dname "CN=GhostTransfer,O=Nyukob,C=RU" 2>&1 | tail -2

echo "[6/6] Подписываю APK..."
zipalign -v 4 "$BUILD/apk/ghost_transfer_unsigned.apk" "$BUILD/apk/ghost_transfer_aligned.apk" 2>&1 | tail -1
apksigner sign \
    --ks "$BUILD/debug.jks" --ks-pass pass:android --key-pass pass:android \
    --out "$BUILD/ghost_transfer.apk" \
    "$BUILD/apk/ghost_transfer_aligned.apk"

echo ""
echo "=== ГОТОВО ==="
ls -lh "$BUILD/ghost_transfer.apk"
