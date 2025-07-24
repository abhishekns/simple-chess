#!/usr/bin/env bash
set -e

# Directory where the Android SDK will be installed
ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-"$HOME/android-sdk"}
CMDLINE_VERSION=10406996

echo "=== Android SDK Setup Script ==="
echo "Target SDK directory: $ANDROID_SDK_ROOT"

# Detect environment and handle Java installation accordingly
if [ -n "$GITHUB_ACTIONS" ]; then
    echo "Running in GitHub Actions CI environment"
    echo "Using CI-provided Java (avoiding package conflicts)"
    # Only install minimal dependencies in CI
    sudo apt-get update -qq
    sudo apt-get install -y -qq wget unzip
elif [ -n "$DEVCONTAINER" ] || [ -f "/.dockerenv" ]; then
    echo "Running in devcontainer/Docker environment"
    echo "Using container-provided Java setup"
    # In devcontainer, Java and certificates should already be properly configured
    apt-get update -qq || sudo apt-get update -qq
    apt-get install -y -qq wget unzip || sudo apt-get install -y -qq wget unzip
else
    echo "Running in local/manual environment"
    echo "Installing Java and dependencies..."
    sudo apt-get update
    # Install Java and handle certificates properly in local environment
    sudo apt-get install -y wget unzip openjdk-17-jdk
    # Give certificates time to configure properly
    sleep 2
fi

# Configure Java environment
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME not set. Detecting Java installation..."
    if [ -n "$GITHUB_ACTIONS" ]; then
        # In GitHub Actions, Java should be pre-configured
        echo "Warning: JAVA_HOME not set in CI environment"
    fi
    # Fallback to system default if JAVA_HOME is not set
    export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    echo "Auto-detected JAVA_HOME: $JAVA_HOME"
else
    echo "Using provided JAVA_HOME: $JAVA_HOME"
fi
export PATH="$JAVA_HOME/bin:$PATH"

# Validate Java installation
echo "Validating Java installation..."
echo "JAVA_HOME: $JAVA_HOME"
if ! command -v java &> /dev/null; then
    echo "Error: Java not found in PATH"
    exit 1
fi
java -version
echo "Java validation successful"

echo "Creating Android SDK directory..."
mkdir -p "$ANDROID_SDK_ROOT"
cd "$ANDROID_SDK_ROOT"

# Check if SDK is already installed
if [ -d "cmdline-tools/latest" ] && [ -f "cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Android SDK command-line tools already installed, skipping download"
else
    echo "Downloading Android SDK command-line tools..."
    wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VERSION}_latest.zip" -O cmdline-tools.zip
    echo "Extracting command-line tools..."
    unzip -q cmdline-tools.zip -d cmdline-tools
    mv cmdline-tools/cmdline-tools cmdline-tools/latest
    rm cmdline-tools.zip
    echo "Command-line tools installation completed"
fi

export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"

echo "Accepting Android SDK licenses..."
# Accept licenses and install basic packages
yes | sdkmanager --licenses > /dev/null 2>&1

echo "Installing Android SDK components..."
sdkmanager --install \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

# Create local.properties for Gradle builds if inside repo
if [ -d "./P2PChessApp" ]; then
    echo "sdk.dir=$ANDROID_SDK_ROOT" > ./P2PChessApp/local.properties
fi

echo "Android SDK installed at $ANDROID_SDK_ROOT"
echo "JAVA_HOME: $JAVA_HOME"
echo "Using Java version: $(java -version 2>&1 | head -n 1)"
echo "Setup completed successfully!"
