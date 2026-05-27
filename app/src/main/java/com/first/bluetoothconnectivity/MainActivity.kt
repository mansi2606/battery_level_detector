package com.first.bluetoothconnectivity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var listView: ListView
    private lateinit var btnRefresh: Button

    private val bluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private val deviceList = mutableListOf<DeviceInfo>()
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var batteryReceiver: BatteryReceiver

    private val basePermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    private val notificationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }

    private val allPermissions get() = basePermissions + bluetoothPermissions + notificationPermissions
    private val permissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        BatteryReceiver.loadBatteryLevels(this)  // Load previously saved battery levels from disk

        tvStatus = findViewById(R.id.tvStatus)
        listView = findViewById(R.id.listView)
        btnRefresh = findViewById(R.id.btnScan)

        // Use our custom adapter instead of ArrayAdapter
        deviceAdapter = DeviceAdapter(this, deviceList)
        listView.adapter = deviceAdapter

        // Register battery receiver
        batteryReceiver = BatteryReceiver()
        val filter = IntentFilter(BatteryReceiver.ACTION_BATTERY_LEVEL_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, filter)
        }

        btnRefresh.setOnClickListener {
            if (hasPermissions()) showConnectedDevices()
            else requestPermissionsFromUser()
        }

        if (!hasPermissions()) requestPermissionsFromUser()
        else showConnectedDevices()
    }

    @SuppressLint("SetTextI18n")
    private fun showConnectedDevices() {
        // Always load from disk first
        BatteryReceiver.loadBatteryLevels(this)

        // Force a live refresh if Bluetooth is on
        if (bluetoothAdapter.isEnabled) {
            forceBatteryRefresh()
        }

        deviceList.clear()

        // Get paired devices if Bluetooth is on
        val pairedDevices = mutableMapOf<String, String>()
        // key = MAC address, value = device name

        if (bluetoothAdapter.isEnabled) {
            try {
                bluetoothAdapter.bondedDevices.forEach { device ->
                    try {
                        val name = device.name ?: "Unknown Device"
                        val address = device.address
                        pairedDevices[address] = name

                        // Save name to disk every time we see it while BT is on
                        // This is our most reliable opportunity to capture names
                        if (name != "Unknown Device") {
                            BatteryReceiver.saveDeviceName(this, address, name)
                        }

                    } catch (e: SecurityException) {
                        pairedDevices[device.address] = "Unknown Device"
                    }
                }
            } catch (e: SecurityException) { }
        }

        // Also include any device we have saved battery data for
        // even if Bluetooth is off right now
        val allAddresses = (
                pairedDevices.keys +
                        BatteryReceiver.batteryLevels.keys
                ).toSet()
        // This is the key change — we combine:
        // 1. currently paired devices (if BT is on)
        // 2. any device we ever recorded battery for (from disk)

        if (allAddresses.isEmpty()) {
            tvStatus.text = "No devices found. Connect a Bluetooth device first."
        } else {
            allAddresses.forEach { address ->
                // Get name — from live paired list or from saved names
                val name = pairedDevices[address]
                    ?: BatteryReceiver.getSavedDeviceName(this, address)
                    ?: "Unknown Device"

                val battery = BatteryReceiver.batteryLevels[address]
                val lastSeen = BatteryReceiver.getLastSeen(this, address)

                deviceList.add(
                    DeviceInfo(
                        name = name,
                        address = address,
                        batteryLevel = battery,
                        lastSeen = lastSeen
                    )
                )
            }

            // Sort — low battery at top, unknown battery at bottom
            deviceList.sortBy { it.batteryLevel ?: 999 }

            val bluetoothStatus = if (bluetoothAdapter.isEnabled) "ON" else "OFF"
            val lowCount = deviceList.count {
                it.batteryLevel != null && it.batteryLevel <= 20
            }

            tvStatus.text = when {
                !bluetoothAdapter.isEnabled ->
                    "📴 Bluetooth off — showing last known data"
                lowCount > 0 ->
                    "⚠️ $lowCount device(s) low on battery!"
                else ->
                    "✅ ${deviceList.size} device(s) monitored • BT $bluetoothStatus"
            }
        }

        deviceAdapter.notifyDataSetChanged()
    }


    private fun forceBatteryRefresh() {
        try {
            val paired = bluetoothAdapter.bondedDevices
            paired.forEach { device ->
                try {
                    // Use reflection to call the hidden getBatteryLevel method
                    // This is the only way to request battery on demand
                    val method = device.javaClass.getMethod("getBatteryLevel")
                    val level = method.invoke(device) as Int
                    if (level >= 0) {
                        BatteryReceiver.batteryLevels[device.address] = level
                    }
                } catch (e: Exception) {
                    // Device doesn't support this method — wait for broadcast instead
                }
            }
        } catch (e: SecurityException) {
            // Permission issue — ignore
        }
    }
    private fun hasPermissions() = allPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsFromUser() {
        ActivityCompat.requestPermissions(this, allPermissions, permissionRequestCode)
    }

    @SuppressLint("SetTextI18n")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            showConnectedDevices()
        } else {
            tvStatus.text = "Some permissions were denied"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }
}