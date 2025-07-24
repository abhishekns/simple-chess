# Simple Chess

This repository contains a sample Android app for playing chess over Wi-Fi Direct. The actual application code lives in the `P2PChessApp` directory.

## Features
- **Peer-to-peer chess gameplay** over Wi-Fi Direct
- **Complete chess rule implementation** including special moves
- **Comprehensive unit test coverage** (72 tests with 100% pass rate)
- **Robust chess engine** with move validation and game state management

## Quick Start

### Prerequisites
- Java 11 or higher
- Git

### Setup and Testing
1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd simple-chess
   ```

2. **Run the automated setup script:**
   ```bash
   bash setup.sh
   ```
   This installs the Android SDK and configures the development environment.

3. **Execute the comprehensive test suite:**
   ```bash
   cd P2PChessApp
   ./gradlew testDebugUnitTest
   ```

### Test Coverage
The project includes **72 comprehensive unit tests** covering:
- ✅ Chess piece movement validation (all piece types)
- ✅ Game rule enforcement (castling, en passant, promotion)
- ✅ Check and checkmate detection
- ✅ Board state management and validation
- ✅ Move notation parsing and formatting
- ✅ Edge cases and error handling

**Current Status:** 72/72 tests passing (100% success rate)

## Development

### Build the App
```bash
cd P2PChessApp

# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease
```

### Run on Device/Emulator
```bash
# Install on connected device
./gradlew installDebug

# Or use Android Studio for full development experience
```

## Deployment

For complete deployment instructions including production builds, app store publishing, and distribution options, see:

📋 **[DEPLOYMENT.md](DEPLOYMENT.md)** - Comprehensive deployment guide

### Quick Deploy Commands
```bash
# Run tests
./gradlew testDebugUnitTest

# Build for Play Store
./gradlew bundleRelease

# Install on device
./gradlew installDebug
```

## Project Structure
```
simple-chess/
├── P2PChessApp/                 # Main Android application
│   ├── app/src/main/           # Application source code
│   ├── app/src/test/           # Unit tests (72 comprehensive tests)
│   └── app/build.gradle        # Build configuration
├── setup.sh                    # Automated environment setup
├── DEPLOYMENT.md               # Detailed deployment instructions
└── README.md                   # This file
```

## Architecture
- **Model-View-Controller** pattern for clean separation of concerns
- **Comprehensive chess engine** with full rule validation
- **Wi-Fi Direct integration** for peer-to-peer connectivity
- **Robust testing framework** ensuring code reliability

## Contributing
1. Fork the repository
2. Create a feature branch
3. Add/update tests for new functionality
4. Ensure all tests pass: `./gradlew testDebugUnitTest`
5. Submit a pull request

## License
This project is provided as a sample application for educational and development purposes.

---

**Need help?** Check the [deployment guide](DEPLOYMENT.md) or open an issue for support.
