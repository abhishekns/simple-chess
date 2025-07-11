# P2P Chess App for Android

## Project Overview

P2PChessApp is an Android application that allows two users to play chess directly with each other using Wi-Fi Direct (peer-to-peer) technology. This means no central server is required for gameplay; the devices connect directly to each other.

Game moves and other game-related communication (like resignations) are sent as simple string messages over TCP/IP sockets once a Wi-Fi Direct group is established between the two devices.

## Features

*   **Peer-to-Peer Gameplay:** Play chess with another user on the same local Wi-Fi network without needing an internet connection or a backend server.
*   **Wi-Fi Direct:** Utilizes Android's Wi-Fi P2P framework for device discovery and connection.
*   **Host/Join Game:** One player hosts the game (becoming discoverable), and the other player joins by selecting the host from a list of discovered peers.
*   **Basic Chess Rules:** Implements standard chess piece movements, captures, check detection, checkmate, and stalemate.
*   **Pawn Promotion:** Players are prompted to choose a piece (Queen, Rook, Bishop, or Knight) upon pawn promotion.
*   **Castling & En Passant:** Basic implementation of these special moves.
*   **Turn-Based Gameplay:** Clear indication of whose turn it is.
*   **Resignation:** Players can resign the game.
*   **Share Invitation:** A button allows users to send a text message via other apps (SMS, WhatsApp, etc.) to invite someone to play.

## Key Technologies Used

*   **Language:** Kotlin
*   **Networking:**
    *   Android Wi-Fi P2P (Wi-Fi Direct) for discovery and connection.
    *   Java Sockets (`ServerSocket`, `Socket`) for data transmission between connected peers.
*   **User Interface:**
    *   Android XML Layouts
    *   Material Components
    *   ViewBinding
    *   `GridLayout` for the chessboard display.
*   **Chess Logic:** Custom chess engine implemented in `app/src/main/java/com/example/p2pchessapp/model/ChessModel.kt`.
*   **Build System:** Gradle

## Project Structure

*   `app/src/main/java/com/example/p2pchessapp/`
    *   `activities/`: Contains `MainActivity.kt` (lobby, P2P management), `GameActivity.kt` (gameplay screen), and `ChessApplication.kt` (simple Application class for inter-activity communication aid).
    *   `model/`: Contains `ChessModel.kt`, which includes all chess logic, piece definitions, board state, move validation, etc.
    *   `network/`: Contains `WifiDirectBroadcastReceiver.kt` for handling Wi-Fi P2P system events.
*   `app/src/main/res/`
    *   `layout/`: XML layout files for `MainActivity` and `GameActivity`.
    *   `drawable/`: Vector drawables for chess pieces (e.g., `ic_king_white.xml`).
    *   `values/`: String resources, colors, themes.
*   `app/src/main/AndroidManifest.xml`: Declares activities, permissions, and application settings.
*   `AGENTS.md`: Instructions and guidelines for AI agents working on this codebase.

## How to Build and Run

1.  **Prerequisites:**
    *   Android Studio (latest stable version recommended).
    *   Android SDK installed (target SDK is 33, min SDK is 21).
    *   Two Android devices with Wi-Fi Direct capability (Android 5.0+). Emulators may have limited or no support for Wi-Fi Direct testing.

2.  **Clone the Repository:**
    ```bash
    git clone <repository-url>
    cd P2PChessApp
    ```

3.  **Open in Android Studio:**
    *   Open Android Studio.
    *   Select "Open an existing Android Studio project".
    *   Navigate to the cloned `P2PChessApp` directory and select it.
    *   *Note: If the project was created in a nested `P2PChessApp` directory within the repository, ensure you open this inner directory in Android Studio.*

4.  **Build the Project:**
    *   Allow Android Studio to sync Gradle files and download any necessary dependencies.
    *   The project is configured to use JDK 11. Ensure Android Studio is using a compatible JDK (usually managed by Android Studio itself).
    *   Click on `Build > Make Project` or use the shortcut (often Ctrl+F9 or Cmd+F9).
    *   Alternatively, you can build from the command line (from the `P2PChessApp` project root where `gradlew` is located):
        ```bash
        ./gradlew assembleDebug
        ```

5.  **Run the App on Two Devices:**
    *   Connect two physical Android devices to your computer via USB.
    *   Ensure USB Debugging is enabled on both devices.
    *   In Android Studio, select one device from the device dropdown and click "Run 'app'".
    *   Once the app is running on the first device, select the second device from the dropdown and click "Run 'app'" again.
    *   Alternatively, install the APK located at `app/build/outputs/apk/debug/app-debug.apk` manually onto both devices.

## Automated Testing

A GitHub Actions CI pipeline is configured in `.github/workflows/android-ci.yml`. This pipeline:
*   Triggers on pushes and pull requests to main branches.
*   Checks out the code.
*   Sets up JDK 11.
*   Runs unit tests (`./gradlew testDebugUnitTest` located in `app/src/test/`).
*   Builds a debug APK.
*   Runs Android Lint for static code analysis.

Unit tests primarily cover the core chess logic in `ChessModel.kt`. Basic Espresso UI tests are included in `app/src/androidTest/` to verify activity launch and UI element presence.

To run tests locally:
*   **Unit Tests:** `./gradlew testDebugUnitTest` (from the `P2PChessApp` project root)
*   **Instrumentation Tests:** `./gradlew connectedDebugAndroidTest` (requires a connected device or emulator)

## Development with VS Code Dev Containers

This project includes support for VS Code Dev Containers, providing a consistent and pre-configured development environment.

### Prerequisites for Dev Containers:

*   **Visual Studio Code:** Latest version installed.
*   **Docker Desktop:** Installed and running on your system (Windows, macOS, or Linux).
*   **VS Code Remote - Containers extension:** Install this extension from the VS Code Marketplace (`ms-vscode-remote.remote-containers`).

### Opening in Dev Container:

1.  **Clone the Repository:** If you haven't already:
    ```bash
    git clone <repository-url>
    cd <repository-name> # Navigate to the root of the repository
    ```
2.  **Open in VS Code:**
    *   Open Visual Studio Code.
    *   Go to `File > Open Folder...` and select the cloned repository root directory (the one containing `.devcontainer` and `P2PChessApp` folders).
3.  **Reopen in Container:**
    *   VS Code should detect the `.devcontainer/devcontainer.json` file and show a notification at the bottom right: "Folder contains a Dev Container configuration file. Reopen in Container."
    *   Click "**Reopen in Container**".
    *   VS Code will build the Docker image as defined in `.devcontainer/Dockerfile` (this might take some time on the first run) and then start the Dev Container.
    *   Your VS Code window will reload, and you'll be working inside the containerized environment. The terminal in VS Code will now be a terminal within the container.

### Working in the Dev Container:

*   **Project Directory:** Your project files (including the `P2PChessApp` Android project) will be available at `/workspaces/<repository-name>/P2PChessApp` inside the container.
*   **Building the App:** Open a terminal in VS Code (Ctrl+` or Cmd+`) and navigate to the Android project directory:
    ```bash
    cd P2PChessApp
    ./gradlew assembleDebug
    ```
*   **Running Unit Tests:**
    ```bash
    cd P2PChessApp
    ./gradlew testDebugUnitTest
    ```
*   **Running Instrumentation Tests:**
    *   Running instrumentation tests (`./gradlew connectedDebugAndroidTest`) typically requires an Android Emulator.
    *   Setting up and running an emulator *inside* a Dev Container can be complex (requiring KVM access for Docker, X11 forwarding, etc.) and is not fully configured by default in this setup.
    *   A common approach is to run the Android Emulator on your **host machine** and configure ADB within the Dev Container to connect to the host's ADB server. This often involves network configuration (e.g., using host networking for the container or specific port forwarding for ADB). This advanced setup is not covered by the default `devcontainer.json`.
    *   For now, unit tests are the primary automated tests runnable directly and easily within this Dev Container setup.

## How to Play

1.  **Enable Wi-Fi:** Ensure Wi-Fi is enabled on both devices. Wi-Fi Direct uses Wi-Fi hardware. (Location services may also need to be enabled on some Android versions for Wi-Fi scanning to work effectively).
2.  **Permissions:** Grant the necessary permissions when prompted (Location and/or Nearby Devices, depending on Android version). These are required for Wi-Fi Direct peer discovery.
3.  **Player 1 (Host):**
    *   On one device, tap the "**Host Game**" button.
    *   The status will update to "Hosting game, waiting for client...".
4.  **Player 2 (Joiner):**
    *   On the second device, tap the "**Join Game**" button.
    *   The status will update to "Joining game, searching for hosts...".
    *   An alert dialog should appear listing available hosts (Player 1's device name should be visible).
    *   Tap on Player 1's device name in the dialog to initiate a connection.
5.  **Connection & Game Start:**
    *   Both devices will show connection progress.
    *   Once successfully connected, the `GameActivity` will launch on both devices.
    *   The Host device will play as White, and the Joiner device will play as Black.
6.  **Gameplay:**
    *   The player whose turn it is can tap one of their pieces to select it. Valid moves for that piece will be highlighted (basic highlighting).
    *   Tap a highlighted square to make the move.
    *   If a pawn reaches the opponent's back rank, a dialog will appear to choose a piece for promotion (Queen, Rook, Bishop, or Knight).
    *   The game continues until checkmate, stalemate, or resignation.
7.  **Resigning:**
    *   Either player can tap the "**Resign**" button at the bottom of the game screen. A confirmation dialog will appear.
8.  **Disconnection:**
    *   If the Wi-Fi Direct connection is lost (e.g., a player turns off Wi-Fi or moves out of range), the game will typically end, and a "Connection Lost" message should appear. Players will be returned to the main screen.

## Share Invite

*   From the main screen (`MainActivity`), tap the "**Share Invite**" button.
*   This will open the Android share sheet, allowing you to send a predefined text message (e.g., "Let's play P2P Chess! Open the app to connect.") via SMS, WhatsApp, email, or other messaging apps. The recipient would then need to have the app installed to host or join a game.

## Known Limitations & Future Improvements

*   **Inter-Activity Communication:** The communication between `MainActivity` (handling network) and `GameActivity` (handling UI) uses a simplified static `WeakReference` and Application class pattern. This could be made more robust using Android Bound Services or an event bus library.
*   **UI/UX:**
    *   Piece graphics are basic vector drawables.
    *   No display for captured pieces.
    *   "Check" indication is via a Toast message and game state text; visual highlighting of the king in check could be added.
*   **Advanced Chess Rules:** Full implementation of draw conditions like threefold repetition or the 50-move rule is not present.
*   **Wi-Fi Direct Reliability:** Wi-Fi Direct connections can sometimes be finicky depending on device hardware, Android version, and environmental factors.
*   **No Reconnection Logic:** If a connection drops, the game ends. There's no mechanism to automatically reconnect or resume a game.
*   **Error Handling:** While basic error handling is in place, it could be made more comprehensive and user-friendly for various P2P edge cases.

Contributions and suggestions for improvement are welcome!
