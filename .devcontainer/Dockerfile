# syntax=docker/dockerfile:1

###############################
# Android SDK dev‑container   #
###############################
# ‑ Base image with JDK 17 (works for cmdline‑tools ≥ 9)
FROM eclipse-temurin:17-jammy AS base

# ---------- build‑time arguments (override from devcontainer.json) ----------
ARG CMDLINE_TOOLS_VERSION=10406996    # July‑2025 release – bump when you need newer
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=${USER_UID}

# ---------- proxy / TLS inspection: add corporate root CA ----------
COPY ./certs/ /usr/local/share/ca-certificates/

# ---------- system packages ----------
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        wget unzip git \
        ca-certificates ca-certificates-java \
    && update-ca-certificates -f \
    # import corporate CA into Temurin JDK cacerts (idempotent)                   \
    && keytool -importcert -noprompt -trustcacerts -alias umbrella \
      -file /usr/local/share/ca-certificates/umbrella-root.crt \
      -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit || true \
    && rm -rf /var/lib/apt/lists/*


# ---------- environment ----------
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:/opt/gradle/bin

# ---------- download command‑line tools ----------
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools \
 && wget -q https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip -O /tmp/cmdline-tools.zip \
 && unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools \
 && mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest \
 && rm /tmp/cmdline-tools.zip

# ---------- install Gradle 8.7 ----------
ARG GRADLE_VERSION=8.7
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle \
    && rm /tmp/gradle.zip

# ---------- accept licences & install SDK components ----------
RUN yes | sdkmanager --licenses > /dev/null \
 && sdkmanager --install \
        "platform-tools" \
        "platforms;android-33" \
        "build-tools;33.0.2" \
        "emulator" \
        "system-images;android-33;google_apis;x86_64"   # uncomment if you need an emulator image

# ---------- create non‑root user (works even if it already exists) ----------
RUN set -eux; \
    if ! getent group "$USER_GID" >/dev/null; then \
        groupadd -g "$USER_GID" "$USERNAME"; \
    fi; \
    if ! id -u "$USERNAME" >/dev/null 2>&1; then \
        useradd -u "$USER_UID" -g "$USER_GID" -m "$USERNAME"; \
    fi; \
    mkdir -p /workspaces /opt/android-sdk; \
    chown -R "$USER_UID:$USER_GID" /workspaces /opt/android-sdk


USER $USERNAME
WORKDIR /workspaces

# ---------- create Gradle cache folder (feature scripts rely on it) ----------
RUN mkdir -p /workspaces/.gradle

# ---------- default command ----------
CMD ["bash"]
