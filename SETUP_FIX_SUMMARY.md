# Android CI Setup Fix Summary

## Problem
The original setup encountered a Java certificate configuration issue during Android CI runs:
```
Exception in thread "main" java.io.FileNotFoundException: /etc/ssl/certs/java/cacerts (Is a directory)
dpkg: error processing package ca-certificates-java (--configure):
```

## Root Cause
The issue was caused by conflicting Java installations:
1. **GitHub Actions CI** was setting up **JDK 11** via `actions/setup-java@v3`
2. **setup.sh script** was installing **OpenJDK 17** via `apt-get install openjdk-17-jdk`

This created conflicts in the `ca-certificates-java` package configuration, as the system was trying to manage certificates for multiple Java versions simultaneously.

## Solution Applied

### 1. Updated CI Workflow (`.github/workflows/android-ci.yml`)
- Changed from **JDK 11** to **JDK 17** for consistency with modern Android development
- Added Java validation step to help with debugging
- Updated comments to reflect the changes

### 2. Enhanced setup.sh Script
- **Environment Detection**: The script now detects different environments:
  - **GitHub Actions CI**: Uses CI-provided Java, installs minimal dependencies
  - **Devcontainer/Docker**: Uses container-provided Java setup
  - **Local/Manual**: Installs Java and handles certificates properly

- **Improved Error Handling**: Added validation steps and better error messages
- **Idempotent Installation**: Script can be run multiple times safely
- **Better Logging**: Added detailed progress messages

### 3. Key Changes Made

#### CI Workflow Changes:
```yaml
# Before
- name: Set up JDK 11
  uses: actions/setup-java@v3
  with:
    java-version: '11'

# After  
- name: Set up JDK 17
  uses: actions/setup-java@v3
  with:
    java-version: '17'
```

#### Setup Script Changes:
```bash
# Before: Always installed Java
sudo apt-get install -y openjdk-17-jdk

# After: Environment-aware installation
if [ -n "$GITHUB_ACTIONS" ]; then
    # CI: Use provided Java, minimal deps only
    sudo apt-get install -y -qq wget unzip
elif [ -n "$DEVCONTAINER" ] || [ -f "/.dockerenv" ]; then
    # Container: Use container Java
    apt-get install -y -qq wget unzip
else
    # Local: Install Java properly
    sudo apt-get install -y wget unzip openjdk-17-jdk
fi
```

## Benefits
1. **No More Certificate Conflicts**: Single Java version prevents ca-certificates-java issues
2. **Environment Flexibility**: Works in CI, containers, and local environments
3. **Better Debugging**: Clear logging helps identify issues
4. **Idempotent**: Safe to run multiple times
5. **Modern Compatibility**: JDK 17 supports latest Android Gradle Plugin versions

## Testing
The fix has been validated to:
- ✅ Detect GitHub Actions environment correctly
- ✅ Use CI-provided Java without conflicts
- ✅ Maintain backward compatibility for local development
- ✅ Handle missing JAVA_HOME gracefully
- ✅ Provide clear error messages and progress indicators

## Usage
The setup script now automatically adapts to the environment:
```bash
# In CI (GitHub Actions)
bash setup.sh  # Uses CI-provided Java

# In local development
bash setup.sh  # Installs Java if needed

# In devcontainer
bash setup.sh  # Uses container Java
```