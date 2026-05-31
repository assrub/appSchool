#!/bin/bash
# Auto-build script: bumps version, builds APK, updates backend
set -e

# 1. Bump version code
APP_GRADLE="app-android/app/build.gradle.kts"
BACKEND_MAIN="backend/main.py"

CURRENT=$(grep "versionCode" "$APP_GRADLE" | head -1 | grep -oP '\d+')
NEW=$((CURRENT + 1))
echo "Bumping version: $CURRENT → $NEW"

sed -i "s/versionCode = $CURRENT/versionCode = $NEW/" "$APP_GRADLE"
sed -i "s/\"versionCode\": $CURRENT/\"versionCode\": $NEW/" "$BACKEND_MAIN"

# 2. Build APK
export JAVA_HOME="$(pwd)/tools/jdk"
export ANDROID_SDK_ROOT="$(pwd)/tools/android-sdk"
cd app-android
./gradlew assembleRelease --no-daemon
cd ..

# 3. Copy APK
cp app-android/app/build/outputs/apk/release/app-release.apk app-release.apk
cp app-release.apk backend/uploads/app-release.apk

echo "Done! APK ready at app-release.apk (versionCode=$NEW)"
echo "Next: git add -A && git commit -m 'version $NEW' && git push"
