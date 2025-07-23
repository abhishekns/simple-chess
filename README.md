# Simple Chess

This repository contains a sample Android app for playing chess over Wi-Fi Direct. The actual application code lives in the `P2PChessApp` directory.

## Running Tests Locally
1. Ensure you have Java 11 installed.
2. Run the provided `setup.sh` script to install the Android SDK and accept the required licenses:
   ```bash
   bash setup.sh
   ```
3. Execute the unit tests:
   ```bash
   cd P2PChessApp
   ./gradlew testDebugUnitTest
   ```

The setup script creates a `local.properties` file pointing Gradle to the installed SDK so the build can run without license errors.
