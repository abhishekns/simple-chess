#!/usr/bin/env bash
set -e

# Directory where the Android SDK will be installed
ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-"$HOME/android-sdk"}
CMDLINE_VERSION=10406996

# Install prerequisites - Updated to Java 17 for Android SDK compatibility
sudo apt-get update
sudo apt-get install -y wget unzip openjdk-17-jdk

# Set JAVA_HOME to ensure the correct Java version is used
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p "$ANDROID_SDK_ROOT"
cd "$ANDROID_SDK_ROOT"

# Download command line tools
wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VERSION}_latest.zip" -O cmdline-tools.zip
unzip -q cmdline-tools.zip -d cmdline-tools
mv cmdline-tools/cmdline-tools cmdline-tools/latest
rm cmdline-tools.zip

export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"

# Accept licenses and install basic packages
yes | sdkmanager --licenses > /dev/null
sdkmanager --install \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

# Create local.properties for Gradle builds if inside repo
if [ -d "./P2PChessApp" ]; then
    echo "sdk.dir=$ANDROID_SDK_ROOT" > ./P2PChessApp/local.properties
fi

echo "Android SDK installed at $ANDROID_SDK_ROOT"
echo "Using Java version: $(java -version 2>&1 | head -n 1)"
