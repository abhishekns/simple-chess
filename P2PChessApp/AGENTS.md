## Agent Instructions for P2PChessApp

This document provides guidance for AI agents working on the P2PChessApp Android project.

### Project Overview:
P2PChessApp is an Android application that allows two users to play chess directly with each other using Wi-Fi Direct (peer-to-peer) technology, without requiring a central server. Game moves and resignations are sent as simple string messages over sockets.

### Key Technologies:
- **Language:** Kotlin
- **Networking:** Wi-Fi Direct (Android P2P Framework), Sockets for data transfer.
- **UI:** Android XML layouts, Material Components, ViewBinding.
- **Chess Logic:** Custom implementation in `model/ChessModel.kt`.
- **Build System:** Gradle

### Development Guidelines:
1.  **Permissions:**
    *   All necessary permissions for Wi-Fi Direct are declared in `AndroidManifest.xml` and runtime checks/requests are handled in `MainActivity.kt` for `ACCESS_FINE_LOCATION` (pre-Android 12) and `NEARBY_WIFI_DEVICES` (Android 12+).
    *   Current permissions: `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `ACCESS_FINE_LOCATION`, `CHANGE_NETWORK_STATE`, `INTERNET`, `ACCESS_NETWORK_STATE`, `NEARBY_WIFI_DEVICES`.
2.  **Wi-Fi Direct Implementation (`MainActivity.kt`, `network/WifiDirectBroadcastReceiver.kt`):**
    *   Uses `WifiP2pManager` for discovery, connection, and group management.
    *   `WifiDirectBroadcastReceiver` handles P2P system broadcasts.
    *   `MainActivity` manages the P2P lifecycle, including starting discovery, connecting to peers (selected via an AlertDialog), and establishing a socket connection.
    *   The Group Owner acts as the `ServerSocket`, the client connects as a `Socket`.
    *   Data (chess moves as "MOVE:e2e4", resignations as "RESIGN") is sent as UTF-8 strings over the socket's `InputStream` and `OutputStream`.
3.  **Inter-Activity Communication (`MainActivity.kt` <-> `GameActivity.kt`):**
    *   **Current Method:** A simplified approach is used:
        *   `GameActivity` holds a static `WeakReference<MainActivity>` (`GameActivity.mainActivityInstance`).
        *   `MainActivity` uses a reference to `GameActivity` via the `ChessApplication` class (`(application as? ChessApplication)?.activeGameActivity`).
    *   **Known Limitation & Future Improvement:** This pattern is not ideal for complex applications and can be prone to lifecycle issues or subtle bugs. For future development, **consider refactoring to use a Bound Service to manage the network connection and data exchange, or use LocalBroadcastManager / an Event Bus library (like GreenRobot EventBus) for more decoupled communication.** Comments regarding this are present in the code.
4.  **Chess Logic (`model/ChessModel.kt`):**
    *   Self-contained chess engine. Handles board representation, piece movements (including pawn promotion, basic castling, en passant), move validation, check, checkmate, and stalemate detection.
    *   Moves are serialized to/from a simple algebraic notation (e.g., "e2e4", "e7e8q") via `ChessMove.toSimpleNotation()` and `ChessMove.fromSimpleNotation()`.
5.  **User Interface:**
    *   **`MainActivity`:** Lobby with "Host Game", "Join Game", and "Share Invite" buttons. Displays P2P status.
    *   **`GameActivity`:** Displays an 8x8 board using `GridLayout` of `ImageViews`. Pieces are represented by simple vector drawables. Users tap to select their pieces and tap a destination square to move. Valid moves for a selected piece are highlighted (basic). Pawn promotion is handled via an `AlertDialog`.
    *   ViewBinding is enabled and used in activities.
6.  **Concurrency and Threading:**
    *   P2P operations and socket communication in `MainActivity` are performed on a background thread using `Executors.newSingleThreadExecutor()`.
    *   UI updates are performed on the main thread using `runOnUiThread`.
7.  **Error Handling:**
    *   Basic error handling for P2P operations (Toasts, status messages).
    *   Socket `IOExceptions` lead to `handleDisconnection()`, which closes resources and attempts to notify `GameActivity` to show a dialog and finish.
    *   Permission denial is handled, prompting the user to go to settings if "Don't ask again" was selected.
8.  **Code Structure:**
    *   `activities`: `MainActivity`, `GameActivity`, `ChessApplication`.
    *   `model`: `ChessModel.kt` (containing `ChessBoard`, `ChessPiece`, `ChessMove`, etc.).
    *   `network`: `WifiDirectBroadcastReceiver.kt`.
    *   `res/drawable`: Vector drawables for chess pieces.
    *   `res/layout`: XML layouts for activities.
9.  **Sharing/Invitation:**
    *   `MainActivity` includes a "Share Invite" button that uses an `ACTION_SEND` Intent to share a predefined text message.
10. **Testing:**
    *   Primarily manual testing on two physical Android devices is required due to the nature of Wi-Fi Direct.
    *   Key areas: discovery, connection, move synchronization, game rules, disconnection, resignation, UI responsiveness.

### Future Agent Considerations:
*   **Refactor Communication:** Prioritize refactoring the `MainActivity` <-> `GameActivity` communication as noted in Guideline #3.
*   **UI Enhancements:** Consider adding display for captured pieces, more distinct "Check" indication, and improved piece graphics.
*   **Advanced Chess Rules:** Full implementation of threefold repetition, 50-move rule for draws.
*   **Robustness:** Further improve error handling and user feedback for various P2P and socket edge cases.
*   When modifying `build.gradle` files, ensure that plugin versions and dependency versions are compatible and up-to-date, but prioritize stability.
*   If new Android SDK features are used, ensure `compileSdk` and `targetSdk` are updated accordingly, and any necessary backward compatibility is handled.

This document is a living guide. Update it if significant architectural decisions or new conventions are established.
