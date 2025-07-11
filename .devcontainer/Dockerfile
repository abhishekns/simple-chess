# Base image with OpenJDK 11 (as specified in build.gradle and android-ci.yml)
FROM openjdk:11-jdk

# Set environment variables for Android SDK
ENV ANDROID_SDK_ROOT /opt/android-sdk
ENV PATH $PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Install necessary packages for Android SDK, including tools for downloading
# and managing SDK components, and for running Gradle.
# libncurses5 is sometimes needed for older sdkmanager/avdmanager UI tools if ever invoked directly.
# Use apt-get for Debian/Ubuntu based openjdk images.
USER root
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    git \
    libncurses5 \
    # Add any other system dependencies your project might need (e.g., for NDK)
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user 'vscode' for security and VS Code compatibility
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    # Give vscode user sudo privileges if needed for some operations (use with caution)
    # && apt-get update && apt-get install -y sudo \
    # && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    # && chmod 0440 /etc/sudoers.d/$USERNAME \
    && mkdir -p /opt/android-sdk \
    && chown -R $USERNAME:$USERNAME /opt/android-sdk

# Switch to the non-root user
USER $USERNAME

# Download and install Android SDK command-line tools
# Ensure the version of cmdline-tools is recent enough or matches your needs.
# Check official Android Studio download page for latest versions.
ARG CMDLINE_TOOLS_VERSION="9477386" # As of late 2023, check for newer versions
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools \
    && mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# Accept SDK licenses and install required SDK packages
# Adjust packages as per your project's build.gradle (compileSdk, buildToolsVersion)
# The `yes |` part is to automatically accept licenses.
RUN yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null
RUN $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager "platforms;android-33" \
    "build-tools;33.0.2" \
    "platform-tools" \
    # "emulator" # Uncomment if you want to install the emulator in the container
    # "system-images;android-33;google_apis;x86_64" # Uncomment for a system image

# Set up Gradle user home to be inside the workspace to persist dependencies
# This can speed up subsequent builds after the first one.
# However, this might not be ideal if the workspace is cleaned often.
# Alternatively, use a Docker volume for Gradle caches.
# ENV GRADLE_USER_HOME /workspaces/${localWorkspaceFolderBasename}/.gradle
# RUN mkdir -p $GRADLE_USER_HOME && chown -R $USERNAME:$USERNAME $GRADLE_USER_HOME
# For simplicity, default Gradle home (~/.gradle) will be used inside container.

# Verify installations (optional)
# RUN java -version
# RUN $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --list_installed

# Default command (optional, can be overridden by devcontainer.json or run commands)
# CMD ["/bin/bash"]

# Ensure gradlew in the workspace is executable after mounting (if needed)
# This is often better handled by a postCreateCommand or postAttachCommand in devcontainer.json
# if the workspace content isn't available during Docker build time for this step.
# WORKDIR /workspaces/${localWorkspaceFolderBasename}/P2PChessApp
# RUN chmod +x ./gradlew

# Final USER directive to ensure VS Code server runs as vscode
USER $USERNAME
WORKDIR /workspaces

# Note:
# - KVM for emulator acceleration is not configured here as it requires host Docker daemon config.
# - For running emulators inside the container, more setup (like X11 forwarding or VNC) might be needed.
#   It's often simpler to run the emulator on the host and connect via ADB if needed.
# - Ensure the CMDLINE_TOOLS_VERSION is up-to-date or suitable.
# - The specific build-tools version (33.0.2) and platform (android-33) should match your project.
# - This Dockerfile provides a basic CLI environment for Android builds and unit tests.
#   Full Android Studio-like experience in a dev container is more complex.
