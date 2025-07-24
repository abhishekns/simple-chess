package com.example.p2pchessapp.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.p2pchessapp.activities.MainActivity

class WifiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val peerListListener: WifiP2pManager.PeerListListener,
    private val connectionInfoListener: WifiP2pManager.ConnectionInfoListener,
    private val activity: MainActivity // Add activity reference
) : BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiDirectBroadcastRecv"
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action: String? = intent.action
        Log.d(TAG, "Received action: $action")

        when (action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                when (state) {
                    WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
                        // Wifi P2P is enabled
                        Log.d(TAG, "Wi-Fi P2P is enabled")
                        activity.updateWifiP2pStatus(true)
                    }
                    else -> {
                        // Wi-Fi P2P is not enabled
                        Log.d(TAG, "Wi-Fi P2P is not enabled")
                        activity.updateWifiP2pStatus(false)
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                // This is often called when peer discovery has successfully found peers
                Log.d(TAG, "P2P peers changed")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                     if (ActivityCompat.checkSelfPermission( context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Missing NEARBY_WIFI_DEVICES permission for requestPeers")
                        // activity.requestNearbyWifiDevicesPermission() // Activity should handle this
                        return
                    }
                } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Missing ACCESS_FINE_LOCATION permission for requestPeers")
                    // activity.requestLocationPermission() // Activity should handle this
                    return
                }

                manager.requestPeers(channel, peerListListener)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Respond to new connection or disconnections
                Log.d(TAG, "P2P connection changed")

                val wifiP2pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO, android.net.wifi.p2p.WifiP2pInfo::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                }

                val wifiP2pGroup = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP, android.net.wifi.p2p.WifiP2pGroup::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP)
                }

                if (wifiP2pInfo?.groupFormed == true) {
                    Log.d(TAG, "Connected to a peer.")
                    manager.requestConnectionInfo(channel, connectionInfoListener)
                    wifiP2pGroup?.let {
                        Log.d(TAG, "Group formed. Group Owner: ${it.isGroupOwner}")
                        // You can also get group owner address from it.owner.deviceAddress
                    }
                } else {
                    Log.d(TAG, "Disconnected from peer.")
                    activity.handleDisconnection()
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
                Log.d(TAG, "This device details changed")
                val thisDevice: WifiP2pDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                }
                thisDevice?.let {
                    Log.d(TAG, "Device Name: ${it.deviceName}, Status: ${it.status}")
                    // activity.updateThisDevice(it) // If you need to update UI with this device's info
                }
            }
            WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                val discoveryState = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED)
                if (discoveryState == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                    Log.d(TAG, "P2P Discovery Started")
                    activity.updateDiscoveryStatus(true)
                } else {
                    Log.d(TAG, "P2P Discovery Stopped")
                    activity.updateDiscoveryStatus(false)
                }
            }
        }
    }
}
