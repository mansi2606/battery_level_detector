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

        forceBatteryRefresh()
        deviceList.clear()

        try {
            // Get ALL paired devices
            val paired: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

            if (paired.isEmpty()) {
                tvStatus.text = "No paired devices found"
            } else {
                paired.forEach { device ->
                    val name = try {
                        device.name ?: "Unknown Device"
                    } catch (e: SecurityException) {
                        "Unknown Device"
                    }

                    val battery = BatteryReceiver.batteryLevels[device.address]

                    deviceList.add(
                        DeviceInfo(
                            name = name,
                            address = device.address,
                            batteryLevel = battery
                        )
                    )
                }

                // Sort — low battery devices appear at TOP
                // devices with unknown battery go to bottom
                deviceList.sortBy { it.batteryLevel ?: 999 }

                val lowCount = deviceList.count {
                    it.batteryLevel != null && it.batteryLevel <= 20
                }

                tvStatus.text = if (lowCount > 0) {
                    "⚠️ $lowCount device(s) low on battery!"
                } else {
                    "✅ ${deviceList.size} device(s) monitored"
                }
            }

        } catch (e: SecurityException) {
            tvStatus.text = "Bluetooth permission denied"
            Toast.makeText(this, "Please grant Bluetooth permission", Toast.LENGTH_SHORT).show()
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