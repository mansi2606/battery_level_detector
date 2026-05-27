package com.first.bluetoothconnectivity

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.edit

class BatteryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL =
            "android.bluetooth.device.extra.BATTERY_LEVEL"

        // in-memory map — same as before
        val batteryLevels = mutableMapOf<String, Int>()

        // SharedPreferences file name — like naming your json file
        const val PREFS_NAME = "battery_prefs"

        // Save battery level to disk
        // context needed to access SharedPreferences
        fun saveBatteryLevel(context: Context, address: String, level: Int) {
            batteryLevels[address] = level  // update in-memory map

            // save to disk
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { putInt(address, level) }
            // .edit()  = open the file for writing (like opening a file in Python)
            // .putInt() = write a value (like dict[key] = value)
            // .apply()  = save and close (like file.close())
        }

        // Load all saved battery levels from disk into memory
        fun loadBatteryLevels(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val all = prefs.all
            // prefs.all returns everything saved — like reading entire json file

            batteryLevels.clear()
            all.forEach { (address, level) ->
                if (level is Int && level >= 0) {
                    batteryLevels[address] = level
                }
            }
        }

        // Save timestamp of when battery was last recorded
        // so we can show "last seen 2 hours ago"
        fun saveLastSeen(context: Context, address: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { putLong("lastSeen_$address", System.currentTimeMillis()) }
        }

        // Get last seen time for a device
        fun getLastSeen(context: Context, address: String): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong("lastSeen_$address", -1)
        }

        // Save device name alongside battery level
        fun saveDeviceName(context: Context, address: String, name: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { putString("name_$address", name) }
        }

        // Get saved device name
        fun getSavedDeviceName(context: Context, address: String): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString("name_$address", null)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BATTERY_LEVEL_CHANGED) {

            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            val batteryLevel = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
            val deviceAddress = device?.address ?: return
            val deviceName = device.name
                ?: getSavedDeviceName(context, deviceAddress)
                ?: "Unknown Device"


            Log.d("BatteryReceiver", "$deviceName battery: $batteryLevel%")

            if (batteryLevel >= 0) {
                // Save to both memory AND disk now
                saveBatteryLevel(context, deviceAddress, batteryLevel)
                saveLastSeen(context, deviceAddress)
                saveDeviceName(context, deviceAddress, deviceName)
            }

            if (batteryLevel in 1..19) {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.sendLowBatteryAlert(deviceName, batteryLevel)
            }
        }
    }
}