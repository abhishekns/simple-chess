# Simple Chess App - Deployment Instructions

## Overview
This document provides step-by-step instructions for deploying the Simple Chess Android application. The app features a peer-to-peer chess game with comprehensive unit test coverage.

## Prerequisites

### Development Environment Setup
1. **Android Studio** (latest stable version)
   - Download from: https://developer.android.com/studio
   - Install with Android SDK and emulator support

2. **Java Development Kit (JDK)**
   - JDK 17 or higher required (for Android SDK compatibility)
   - Verify installation: `java -version`

3. **Git** (for version control)
   - Download from: https://git-scm.com/
   - Verify installation: `git --version`

## Quick Setup (Automated)

### Using the Setup Script
```bash
# Clone the repository
git clone <repository-url>
cd simple-chess

# Run the automated setup script
bash setup.sh
```

The setup script will automatically:
- Install Android SDK to `~/android-sdk`
- Configure SDK path in `local.properties`
- Set up necessary Android SDK components
- Prepare the development environment

## Manual Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd simple-chess
```

### 2. Android SDK Configuration
```bash
# Create local.properties file
echo "sdk.dir=$HOME/android-sdk" > P2PChessApp/local.properties

# Or set your existing Android SDK path
echo "sdk.dir=/path/to/your/android-sdk" > P2PChessApp/local.properties
```

### 3. Install Dependencies
```bash
cd P2PChessApp
./gradlew build
```

## Testing

### Run Unit Tests
```bash
cd P2PChessApp

# Run all unit tests
./gradlew testDebugUnitTest

# Run with detailed output
./gradlew testDebugUnitTest --console=plain

# View test reports
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Test Coverage
- **72 comprehensive unit tests**
- **100% pass rate**
- Coverage includes:
  - Chess piece movement validation
  - Game rule enforcement
  - Special moves (castling, en passant, promotion)
  - Check/checkmate detection
  - Board state management

## Build Options

### Debug Build
```bash
# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
# Build release APK (requires signing configuration)
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### Android App Bundle (for Play Store)
```bash
# Build App Bundle
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

## Deployment Targets

### 1. Local Development/Testing

#### Using Android Emulator
```bash
# Start emulator (Android Studio)
# Then install and run:
./gradlew installDebug
adb shell am start -n com.example.p2pchessapp/.MainActivity
```

#### Using Physical Device
```bash
# Enable USB debugging on device
# Connect via USB
adb devices  # Verify device is connected
./gradlew installDebug
```

### 2. Internal Testing

#### Firebase App Distribution
```bash
# Configure Firebase in your project
# Add google-services.json to app/ directory
# Deploy to testers:
./gradlew assembleDebug appDistributionUploadDebug
```

#### Google Play Console - Internal Testing
1. Build release APK/AAB
2. Upload to Play Console
3. Create internal testing track
4. Add test users via email

### 3. Production Deployment

#### Google Play Store
1. **Prepare Release Build**
   ```bash
   # Configure signing in app/build.gradle
   ./gradlew bundleRelease
   ```

2. **Upload to Play Console**
   - Create app listing
   - Upload app bundle (AAB file)
   - Complete store listing information
   - Set content rating and pricing

3. **Release Process**
   - Submit for review
   - Monitor for approval
   - Release to production

#### Alternative Distribution
- **Amazon Appstore**: Upload APK directly
- **Samsung Galaxy Store**: Use Samsung Developer Portal
- **Direct APK Distribution**: Host APK file for direct download

## Configuration

### Environment Variables
Create `app/src/main/res/values/config.xml`:
```xml
<resources>
    <string name="app_name">Simple Chess</string>
    <string name="version_name">1.0.0</string>
    <!-- Add other configuration values -->
</resources>
```

### Build Variants
Configure in `app/build.gradle`:
```gradle
android {
    buildTypes {
        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## Troubleshooting

### Common Issues

#### SDK Not Found
```bash
# Verify SDK path
cat P2PChessApp/local.properties
# Should show: sdk.dir=/path/to/android-sdk
```

#### Java Version Compatibility Error
If you see `UnsupportedClassVersionError` with class file version 61.0:
```bash
# Check current Java version
java -version

# Install Java 17 (Ubuntu/Debian)
sudo apt-get install openjdk-17-jdk

# Set JAVA_HOME (add to ~/.bashrc)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

# Verify correct version
java -version  # Should show Java 17
```

#### Build Failures
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

#### Test Failures
```bash
# Run specific test class
./gradlew testDebugUnitTest --tests "com.example.p2pchessapp.model.ChessBoardTest"

# Debug test output
./gradlew testDebugUnitTest --debug
```

### Performance Optimization
- Enable R8 code shrinking for release builds
- Optimize images and resources
- Use vector drawables where possible
- Profile memory usage with Android Studio

## Monitoring and Analytics

### Crash Reporting
- Integrate Firebase Crashlytics
- Monitor crash-free users percentage
- Set up automated alerts

### Performance Monitoring
- Use Firebase Performance Monitoring
- Track app startup time
- Monitor network requests

## Security Considerations

### Code Obfuscation
- Enable ProGuard/R8 for release builds
- Protect sensitive game logic
- Validate all user inputs

### Network Security
- Use HTTPS for all network communications
- Implement certificate pinning if applicable
- Validate all data from network sources

## Maintenance

### Regular Updates
- Monitor Android version distribution
- Update target SDK annually
- Keep dependencies current
- Regular security patches

### Testing Strategy
- Run full test suite before each release
- Perform manual testing on various devices
- Test on different Android versions
- Validate accessibility features

## Support and Documentation

### User Support
- Create FAQ documentation
- Set up feedback collection
- Monitor app store reviews
- Provide contact information

### Developer Documentation
- Maintain API documentation
- Document architecture decisions
- Keep deployment guide updated
- Version control all configurations

---

## Quick Reference Commands

```bash
# Setup
bash setup.sh

# Test
./gradlew testDebugUnitTest

# Build Debug
./gradlew assembleDebug

# Build Release
./gradlew bundleRelease

# Install on Device
./gradlew installDebug

# Clean Build
./gradlew clean build
```

For additional support or questions, please refer to the project documentation or contact the development team.