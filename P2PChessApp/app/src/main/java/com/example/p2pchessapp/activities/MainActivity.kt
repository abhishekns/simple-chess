package com.example.p2pchessapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.p2pchessapp.R
import com.example.p2pchessapp.databinding.ActivityMainBinding
import com.example.p2pchessapp.network.WifiDirectBroadcastReceiver
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private lateinit var binding: ActivityMainBinding
    private val intentFilter = IntentFilter()
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    private var isWifiP2pEnabled = false
    private var isDiscovering = false
    var isHost = false // Made public for GameActivity to know (simplification)
    private var connectedPeerName: String? = null


    private val peers = mutableListOf<WifiP2pDevice>()
    private lateinit var peerListAdapter: ArrayAdapter<String> // For displaying peers in a dialog
    private val peerDeviceNames = mutableListOf<String>()

    // For data transfer
    private val executorService = Executors.newSingleThreadExecutor()
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val port = 8888

    private lateinit var messageHandlerThread: HandlerThread // For processing received messages
    private lateinit var messageHandler: Handler // Handler for the message processing thread


    companion object {
        private const val TAG = "MainActivityP2P"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val NEARBY_WIFI_DEVICES_PERMISSION_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the WeakReference to this MainActivity instance for GameActivity to use.
        // This is part of a simplified communication pattern used for this project's scope.
        // WARNING: For production applications or more complex scenarios, this direct static
        // (Weak)Reference pattern between Activities is discouraged due to potential lifecycle
        // complexities and tight coupling.
        // PREFERRED ALTERNATIVES:
        // 1. Bound Service: Manage network connection and data exchange in a Service.
        //    Activities bind to the service to communicate.
        // 2. LocalBroadcastManager or Event Bus (e.g., GreenRobot EventBus):
        //    Decouple components by sending and receiving events/messages.
        // This project uses WeakReference + Application class reference for simplicity here.
        GameActivity.mainActivityInstance = java.lang.ref.WeakReference(this)

        setupMessageHandler()
        setupPermissions()
        initializeWifiDirect()
        setupUI()
        registerWifiDirectReceiver()
    }

    private fun setupMessageHandler() {
        // Using a Handler on the main looper to process messages from the network thread.
        // This keeps the network listening thread free from complex processing and ensures
        // UI updates (like calling GameActivity methods) happen on the main thread.
        messageHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                val receivedMessage = msg.obj as String
                Log.d(TAG, "MessageHandler received: $receivedMessage")

                val gameActivity = (application as? ChessApplication)?.activeGameActivity
                if (gameActivity != null && !gameActivity.isFinishing) {
                    if (receivedMessage.startsWith("MOVE:")) {
                        gameActivity.handleOpponentMove(receivedMessage.substring(5))
                    } else if (receivedMessage == "RESIGN") {
                        gameActivity.handleOpponentResignation()
                    } else {
                        Log.w(TAG, "Unknown message type received by Handler: $receivedMessage")
                    }
                } else {
                    Log.w(TAG, "GameActivity not active or finishing, message received by Handler: $receivedMessage")
                    if (receivedMessage == "RESIGN") {
                        Toast.makeText(this@MainActivity, "${connectedPeerName ?: "Opponent"} resigned.", Toast.LENGTH_LONG).show()
                        handleDisconnection() // Clean up if game already ended or activity gone
                    }
                }
            }
        }
    }


    private fun setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), NEARBY_WIFI_DEVICES_PERMISSION_REQUEST_CODE)
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Both LOCATION_PERMISSION_REQUEST_CODE (for older Android versions)
        // and NEARBY_WIFI_DEVICES_PERMISSION_REQUEST_CODE (for Android 12+)
        // are critical for Wi-Fi Direct functionality.
        // Currently, their denial is handled identically: log, toast, and show a dialog
        // if "Don't ask again" was selected.
        // If future development requires distinct handling (e.g., different rationales or fallback mechanisms),
        // this when statement can be split into separate cases for each request code.
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE, NEARBY_WIFI_DEVICES_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Wi-Fi Direct related permission granted. Request code: $requestCode")
                } else {
                    Log.e(TAG, "Wi-Fi Direct related permission denied. Request code: $requestCode. Wi-Fi Direct features may not work.")
                    Toast.makeText(this, "Permission for Wi-Fi Direct denied. Features may not function.", Toast.LENGTH_LONG).show()
                     if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                        // User selected "Don't ask again" or policy disabled the permission.
                        // Guide them to app settings.
                        showPermissionDeniedDialog()
                    }
                }
            }
            // Potentially other request codes if added in the future
            // else -> {
            //     Log.d(TAG, "Unhandled onRequestPermissionsResult for request code: $requestCode")
            // }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs Location or Nearby Devices permission to discover and connect to peers. Please enable it in app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun initializeWifiDirect() {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        if (manager == null) {
            Toast.makeText(this, "Wi-Fi P2P service not available.", Toast.LENGTH_LONG).show(); finish(); return
        }
        channel = manager?.initialize(this, Looper.getMainLooper(), null)
        if (channel == null) {
            Toast.makeText(this, "Failed to initialize Wi-Fi P2P channel.", Toast.LENGTH_LONG).show(); finish(); return
        }
        receiver = WifiDirectBroadcastReceiver(manager, channel, this)
    }

    private fun registerWifiDirectReceiver() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
        registerReceiver(receiver, intentFilter)
    }

    private fun setupUI() {
        peerListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, peerDeviceNames)

        binding.buttonHostGame.setOnClickListener {
            isHost = true
            startDiscovery()
            updateStatus("Hosting game, waiting for client...")
            // Disable buttons during hosting/joining attempt
            binding.buttonHostGame.isEnabled = false
            binding.buttonJoinGame.isEnabled = false
        }

        binding.buttonJoinGame.setOnClickListener {
            isHost = false
            startDiscovery()
            updateStatus("Joining game, searching for hosts...")
            binding.buttonHostGame.isEnabled = false
            binding.buttonJoinGame.isEnabled = false
        }

        binding.buttonShareInvite.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Let's play P2P Chess! Open the app to connect.")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        if (!isWifiP2pEnabled) {
            Toast.makeText(this, "Enable Wi-Fi P2P first.", Toast.LENGTH_SHORT).show(); return
        }
        // Permission checks are crucial here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES), NEARBY_WIFI_DEVICES_PERMISSION_REQUEST_CODE); return
            }
        } else {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE); return
            }
        }

        if (isDiscovering) { Log.d(TAG, "Discovery already active."); return }

        peers.clear()
        peerDeviceNames.clear()
        peerListAdapter.notifyDataSetChanged()
        updateStatus("Discovering peers...")

        manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Peer discovery initiated.")
                Toast.makeText(MainActivity@this, "Peer discovery initiated", Toast.LENGTH_SHORT).show()
                isDiscovering = true
            }
            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Peer discovery failed. Reason: $reasonCode")
                Toast.makeText(MainActivity@this, "Peer discovery failed. Code: $reasonCode", Toast.LENGTH_SHORT).show()
                isDiscovering = false
                updateStatus("Discovery failed: ${errorReasonToString(reasonCode)}. Try again.")
                enableLobbyButtons()
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: WifiP2pDevice) {
        // Permission checks
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                 Toast.makeText(this, "Nearby Devices permission not granted.", Toast.LENGTH_SHORT).show(); enableLobbyButtons(); return
             }
        } else {
             if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                 Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show(); enableLobbyButtons(); return
             }
        }

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            // groupOwnerIntent default (0) makes this device a client if connecting to a GO.
            // If both are trying to connect, one will be chosen as GO.
            // For explicit hosting, the host should create a group first (not done in this simplified version)
            // or set groupOwnerIntent to 15 if it wants to be GO.
        }
        connectedPeerName = device.deviceName // Store for GameActivity

        manager?.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connection to ${device.deviceName} initiated.")
                Toast.makeText(MainActivity@this, "Connecting to ${device.deviceName}", Toast.LENGTH_SHORT).show()
                updateStatus("Connecting to ${device.deviceName}...")
            }
            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Connection to ${device.deviceName} failed. Reason: $reasonCode")
                Toast.makeText(MainActivity@this, "Connection failed. Code: $reasonCode", Toast.LENGTH_SHORT).show()
                updateStatus("Connection failed: ${errorReasonToString(reasonCode)}. Try again.")
                enableLobbyButtons()
                connectedPeerName = null
            }
        })
    }

    override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        val refreshedPeers = peerList.deviceList.filter { it.status == WifiP2pDevice.AVAILABLE || it.status == WifiP2pDevice.CONNECTED }

        peers.clear()
        peers.addAll(refreshedPeers)
        peerDeviceNames.clear()
        peers.forEach { peerDeviceNames.add(it.deviceName ?: "Unknown Device") }
        // No direct list view update here, using dialog instead

        Log.d(TAG, "Peers available: ${peers.size}")
        peers.forEach { Log.d(TAG, "Device: ${it.deviceName}, Status: ${getDeviceStatus(it.status)}") }

        if (!isHost && peers.isNotEmpty() && (clientSocket == null || !clientSocket!!.isConnected)) {
            // If we are a client, discovery found peers, and we are not already connected
            showPeerSelectionDialog()
        } else if (isHost && peers.isEmpty() && isDiscovering) {
             updateStatus("Hosting game, waiting for client... (No peers found yet)")
        } else if (!isHost && peers.isEmpty() && isDiscovering) {
            updateStatus("Searching for hosts... (No peers found yet)")
        }
    }

    private fun showPeerSelectionDialog() {
        if (peers.isEmpty()) {
            Toast.makeText(this, "No available peers found.", Toast.LENGTH_SHORT).show()
            updateStatus("No available peers found. Try having the host start first.")
            enableLobbyButtons()
            return
        }

        val deviceNames = peers.map { it.deviceName ?: "Unknown Device" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select a Host to Join")
            .setItems(deviceNames) { dialog, which ->
                if (which in peers.indices) {
                    connectToDevice(peers[which])
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                updateStatus("Peer selection cancelled.")
                enableLobbyButtons()
            }
            .setOnCancelListener {
                updateStatus("Peer selection cancelled.")
                enableLobbyButtons()
            }
            .show()
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        Log.d(TAG, "Connection info available. Group Formed: ${info.groupFormed}, Is GO: ${info.isGroupOwner}")

        if (info.groupFormed) {
            val hostAddress: InetAddress = info.groupOwnerAddress
            isHost = info.isGroupOwner // Update our role based on who became Group Owner

            if (isHost) {
                Log.d(TAG, "This device is HOST (Group Owner). Starting server socket.")
                updateStatus("Connected as Host. Waiting for client to establish socket...")
                executorService.execute {
                    try {
                        serverSocket = ServerSocket(port)
                        Log.d(TAG, "Server socket opened on port $port")
                        clientSocket = serverSocket!!.accept() // Blocking call
                        Log.d(TAG, "Client socket connection accepted.")
                        inputStream = clientSocket!!.getInputStream()
                        outputStream = clientSocket!!.getOutputStream()
                        runOnUiThread {
                            Toast.makeText(this, "Client connected! Starting game as Host (White).", Toast.LENGTH_LONG).show()
                            startGameActivity()
                        }
                        listenForMessages()
                    } catch (e: IOException) {
                        Log.e(TAG, "IOException in server thread: ${e.message}", e)
                        runOnUiThread{ Toast.makeText(this, "Host connection error: ${e.message}", Toast.LENGTH_LONG).show(); handleDisconnection() }
                    }
                }
            } else { // This device is CLIENT
                Log.d(TAG, "This device is CLIENT. Connecting to Host's server socket.")
                updateStatus("Connected as Client. Establishing socket to Host...")
                executorService.execute {
                    try {
                        clientSocket = Socket()
                        clientSocket!!.connect(InetSocketAddress(hostAddress, port), 5000) // 5 sec timeout
                        Log.d(TAG, "Connected to server socket.")
                        inputStream = clientSocket!!.getInputStream()
                        outputStream = clientSocket!!.getOutputStream()
                        runOnUiThread {
                             Toast.makeText(this, "Connected to Host! Starting game as Client (Black).", Toast.LENGTH_LONG).show()
                             startGameActivity()
                        }
                        listenForMessages()
                    } catch (e: IOException) {
                        Log.e(TAG, "IOException in client thread: ${e.message}", e)
                        runOnUiThread{Toast.makeText(this, "Client connection error: ${e.message}", Toast.LENGTH_LONG).show(); handleDisconnection()}
                    }
                }
            }
             // Stop discovery once connection is being established
            if (isDiscovering) {
                stopDiscovery()
            }
        } else {
            Log.d(TAG, "Group not formed, though connection callback was received.")
            updateStatus("Connection established, but group not formed. Retrying discovery might be needed.")
            enableLobbyButtons()
        }
    }

    private fun startGameActivity() {
        if (clientSocket == null || !clientSocket!!.isConnected) {
            Toast.makeText(this, "Socket not connected. Cannot start game.", Toast.LENGTH_LONG).show()
            handleDisconnection() // This will re-enable buttons
            return
        }
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_IS_HOST, isHost)
            putExtra(GameActivity.EXTRA_OPPONENT_NAME, connectedPeerName ?: "Opponent")
        }
        startActivity(intent)
        // Buttons will be re-enabled in onResume if game ends or in handleDisconnection
    }

    private fun listenForMessages() {
        executorService.execute {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (clientSocket?.isConnected == true && inputStream != null) {
                try {
                    bytes = inputStream!!.read(buffer)
                    if (bytes == -1) {
                        Log.d(TAG, "Peer closed socket connection.")
                        handleDisconnection() // This will also notify GameActivity if it's active
                        break
                    }
                    val receivedMessage = String(buffer, 0, bytes)
                    Log.d(TAG, "Raw message received by listener: $receivedMessage")

                    // Send message to the Handler on the main thread for processing
                    val message = messageHandler.obtainMessage()
                    message.obj = receivedMessage
                    messageHandler.sendMessage(message)

                } catch (e: IOException) {
                    Log.e(TAG, "Error reading message: ${e.message}", e)
                    handleDisconnection()
                    break
                }
            }
             Log.d(TAG, "Message listening loop ended.")
        }
    }

    fun sendMessage(message: String) {
        if (outputStream == null || clientSocket == null || !clientSocket!!.isConnected) {
            Log.e(TAG, "Cannot send message, outputStream is null or socket not connected.")
            Toast.makeText(this, "Not connected to a peer to send message.", Toast.LENGTH_SHORT).show()
            // If GameActivity is active and this happens, it's a problem.
            (application as? ChessApplication)?.activeGameActivity?.let {
                it.runOnUiThread {
                    Toast.makeText(it, "Connection lost. Cannot send move.", Toast.LENGTH_LONG).show()
                    // Potentially end game or show error in GameActivity
                }
            }
            handleDisconnection()
            return
        }
        executorService.execute {
            try {
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
                Log.d(TAG, "Sent message: $message")
            } catch (e: IOException) {
                Log.e(TAG, "Error sending message: ${e.message}", e)
                 runOnUiThread { Toast.makeText(this, "Error sending message.", Toast.LENGTH_SHORT).show() }
                 handleDisconnection() // Critical error, likely connection lost
            }
        }
    }

    fun updateWifiP2pStatus(isEnabled: Boolean) {
        this.isWifiP2pEnabled = isEnabled
        Log.d(TAG, "Wi-Fi P2P Enabled: $isWifiP2pEnabled")
        Toast.makeText(this, "Wi-Fi P2P: ${if (isEnabled) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        if (!isEnabled) {
            updateStatus("Wi-Fi P2P Disabled. Please enable Wi-Fi.")
            peers.clear()
            peerDeviceNames.clear()
            peerListAdapter.notifyDataSetChanged()
            handleDisconnection()
        } else {
            updateStatus("Wi-Fi P2P Enabled. Host or Join a game.")
        }
        enableLobbyButtons() // Ensure buttons reflect current state
    }

    fun updateDiscoveryStatus(isDiscoveringNow: Boolean) {
        this.isDiscovering = isDiscoveringNow
        Log.d(TAG, "Discovery Active: $isDiscoveringNow")
        if (!isDiscoveringNow && (clientSocket == null || !clientSocket!!.isConnected)) {
            // If discovery stopped and we are not connected, re-enable buttons
            // But only if not already in a connection attempt phase
            if (binding.textViewStatus.text.toString().contains("failed") || binding.textViewStatus.text.toString().contains("cancelled") || binding.textViewStatus.text.toString().contains("Idle")) {
                 enableLobbyButtons()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun handleDisconnection() {
        Log.d(TAG, "Handling disconnection. Closing sockets and cleaning up.")

        // Notify GameActivity if it's active
        (application as? ChessApplication)?.activeGameActivity?.let { gameActivity ->
            gameActivity.runOnUiThread {
                if (!gameActivity.isFinishing && !gameActivity.isDestroyed) {
                     AlertDialog.Builder(gameActivity)
                        .setTitle("Connection Lost")
                        .setMessage("The connection to the opponent was lost.")
                        .setPositiveButton("OK") { _, _ -> gameActivity.finish() }
                        .setCancelable(false)
                        .show()
                }
            }
        }
        (application as? ChessApplication)?.activeGameActivity = null


        try {
            inputStream?.close()
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing sockets: ${e.message}", e)
        } finally {
            inputStream = null
            outputStream = null
            clientSocket = null
            serverSocket = null
            connectedPeerName = null
        }

        // Remove P2P group if this device was the owner or part of one
        if (manager != null && channel != null) {
            manager?.requestGroupInfo(channel) { group ->
                if (group != null) {
                    manager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() { Log.d(TAG, "P2P group removed on disconnect.") }
                        override fun onFailure(reason: Int) { Log.w(TAG, "Failed to remove P2P group on disconnect: $reason") }
                    })
                }
            }
        }

        runOnUiThread {
            Toast.makeText(this, "Disconnected from peer.", Toast.LENGTH_LONG).show()
            updateStatus("Disconnected. Host or Join again.")
            enableLobbyButtons()
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            binding.textViewStatus.text = status
            Log.d(TAG, "Status Update: $status")
        }
    }

    private fun enableLobbyButtons() {
        binding.buttonHostGame.isEnabled = true
        binding.buttonJoinGame.isEnabled = true
    }


    override fun onResume() {
        super.onResume()
        if (receiver == null) {
            receiver = WifiDirectBroadcastReceiver(manager, channel, this)
        }
        registerReceiver(receiver, intentFilter)
        // If returning from GameActivity or if sockets are null, ensure lobby is usable
        if (clientSocket == null || !clientSocket!!.isConnected) {
            updateStatus("Welcome! Host or Join a game.")
            enableLobbyButtons()
        }
        GameActivity.mainActivityInstance = this // Re-assign in case it was cleared
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiverSafe()
        // Do not clear mainActivityInstance here if GameActivity might still be using it.
        // GameActivity's onDestroy should clear it.
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiverSafe()
        disconnectAndCleanup() // More thorough cleanup
        executorService.shutdownNow()
        if (GameActivity.mainActivityInstance == this) {
             GameActivity.mainActivityInstance = null
        }
    }

    private fun unregisterReceiverSafe() {
        try {
            if (receiver != null) {
                unregisterReceiver(receiver)
                // receiver = null; // Optional: nullify if re-created in onResume always
            }
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver not registered or already unregistered: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopDiscovery() {
        if (isDiscovering && manager != null && channel != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) { /* Already checked */ return }
            } else {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { /* Already checked */ return }
            }
            manager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() { Log.d(TAG, "Stopped peer discovery."); isDiscovering = false; updateDiscoveryStatus(false) }
                override fun onFailure(reason: Int) { Log.w(TAG, "Failed to stop peer discovery: $reason") }
            })
        }
    }


    @SuppressLint("MissingPermission")
    private fun disconnectAndCleanup() {
        stopDiscovery()
        handleDisconnection() // This already includes removing group and closing sockets.
    }

    private fun getDeviceStatus(deviceStatus: Int): String {
        return when (deviceStatus) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.CONNECTED -> "Connected" // This means P2P connected, not necessarily socket connected
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
    }

    private fun errorReasonToString(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
            WifiP2pManager.ERROR -> "Error"
            WifiP2pManager.BUSY -> "Busy"
            else -> "Unknown Error ($reasonCode)"
        }
    }
}
