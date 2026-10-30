package com.anmolmz.signalcheck

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

@Suppress("DEPRECATION")
class MainActivity : Activity() {
    private lateinit var wifi: WifiManager
    private lateinit var status: TextView
    private lateinit var connected: TextView
    private lateinit var networks: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val updateLoop = object : Runnable {
        override fun run() {
            refreshConnected()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(Color.rgb(15, 23, 42))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        fun heading(text: String, size: Float = 22f): TextView = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
            root.addView(this)
        }
        heading("SignalCheck", 30f)
        heading("Wi-Fi laboratory • Version 0.1", 16f)
        status = heading("Checking permission…", 14f)
        heading("Connected network", 21f)
        connected = heading("Loading…", 17f)
        val scanButton = Button(this).apply {
            text = "SCAN NEARBY WI-FI"
            setOnClickListener { scanNetworks() }
        }
        root.addView(scanButton)
        val settingsButton = Button(this).apply {
            text = "OPEN LOCATION SETTINGS"
            setOnClickListener { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        }
        root.addView(settingsButton)
        heading("Nearby networks", 21f)
        networks = heading("Tap Scan to begin.", 15f)
        heading("Tip: Move between rooms and watch the dBm value. A value closer to 0 is stronger (for example, -45 dBm is better than -85 dBm).", 14f)

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
        } else {
            status.text = "Permission granted • Ensure Location and Wi-Fi are enabled"
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateLoop)
    }

    override fun onPause() {
        handler.removeCallbacks(updateLoop)
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            status.text = if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
                "Permission granted • Turn on Location to view network names" else
                "Location permission denied • Wi-Fi names and scans may be unavailable"
        }
    }

    private fun refreshConnected() {
        if (!wifi.isWifiEnabled) {
            connected.text = "Wi-Fi is disabled"
            return
        }
        try {
            val info = wifi.connectionInfo
            if (info.networkId == -1 || info.rssi == -127) {
                connected.text = "Not connected to Wi-Fi"
                return
            }
            val name = info.ssid.removeSurrounding("\"")
            val strength = WifiManager.calculateSignalLevel(info.rssi, 5)
            connected.text = "Name: $name\nSignal: ${info.rssi} dBm\nStrength: ${strength + 1}/5\nFrequency: ${info.frequency} MHz\nLink speed: ${info.linkSpeed} Mbps"
        } catch (e: SecurityException) {
            connected.text = "Permission required: ${e.message}"
        }
    }

    private fun scanNetworks() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status.text = "Grant Location permission in Android settings first."
            return
        }
        try {
            val started = wifi.startScan()
            val results = wifi.scanResults.sortedByDescending { it.level }
            networks.text = if (results.isEmpty()) {
                "No scan results. Enable device Location and Wi-Fi, then try again."
            } else {
                results.take(30).joinToString("\n\n") { item ->
                    val name = item.SSID.ifBlank { "(hidden SSID)" }
                    "$name\n${item.level} dBm  •  ${item.frequency} MHz"
                }
            }
            status.text = if (started) "Scan requested (results may be cached; tap again shortly)" else
                "Scan throttled or unavailable; showing last cached results."
        } catch (e: SecurityException) {
            status.text = "Scan denied: check Location permission and device Location switch"
        }
    }
}
