package com.first.bluetoothconnectivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val CHANNEL_ID = "battery_alert_channel"
    private val NOTIFICATION_ID = 1001

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Battery Alerts",    // name shown in phone settings
            NotificationManager.IMPORTANCE_HIGH  // HIGH = makes sound + pops up
        ).apply {
            description = "Alerts when connected device battery is low"
            enableVibration(true)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.createNotificationChannel(channel)
    }
    // actually register the channel with Android
    fun sendLowBatteryAlert(deviceName: String, batteryLevel: Int) {
        // Get default alert sound
        val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Low Battery Alert")
            .setContentText("$deviceName battery is at $batteryLevel%")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $deviceName battery has dropped to $batteryLevel%. Please charge soon!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alertSound)       // plays alert sound
            .setVibrate(longArrayOf(0, 500, 200, 500))  // vibration pattern
            .setAutoCancel(true)        // dismiss when tapped
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}