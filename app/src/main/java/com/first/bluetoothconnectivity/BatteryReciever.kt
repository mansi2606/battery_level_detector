package com.first.bluetoothconnectivity

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission

class BatteryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"

        // ← this is what was missing
        // stores battery level for each device by MAC address
        // key = "80:99:E7:C4:64:6D", value = 90
        val batteryLevels = mutableMapOf<String, Int>()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == ACTION_BATTERY_LEVEL_CHANGED) {

            // Fixed: handle both old and new Android versions
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            // Fixed: use our own constant string instead of the hidden SDK one
            val batteryLevel = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)

            val deviceName = device?.name ?: "Unknown Device"

            Log.d("BatteryReceiver", "$deviceName battery: $batteryLevel%")

            if (batteryLevel in 1..19) {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.sendLowBatteryAlert(deviceName, batteryLevel)
            }
        }
    }
}