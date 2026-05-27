package com.first.bluetoothconnectivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt

// A data class to hold device info — like a Python dictionary but typed
data class DeviceInfo(
    val name: String,
    val address: String,
    val batteryLevel: Int?  // null means unknown
)

class DeviceAdapter(
    context: Context,
    private val devices: List<DeviceInfo>
) : ArrayAdapter<DeviceInfo>(context, 0, devices) {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // inflate = create the row layout from our XML file
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_device, parent, false)

        val device = devices[position]

        // Find views inside the row
        val tvDeviceName = view.findViewById<TextView>(R.id.tvDeviceName)
        val tvDeviceAddress = view.findViewById<TextView>(R.id.tvDeviceAddress)
        val tvBattery = view.findViewById<TextView>(R.id.tvBattery)
        val rowContainer = view.findViewById<LinearLayout>(R.id.rowContainer)

        // Set device name and address
        tvDeviceName.text = device.name
        tvDeviceAddress.text = device.address

        // Set battery text and color based on level
        when {
            device.batteryLevel == null -> {
                // Battery unknown — device connected but no report yet
                tvBattery.text = "🔋 --%"
                tvBattery.setTextColor("#999999".toColorInt())
                rowContainer.setBackgroundColor("#FFFFFF".toColorInt())
            }
            device.batteryLevel <= 20 -> {
                // LOW BATTERY — highlight the whole row red
                tvBattery.text = "⚠️ ${device.batteryLevel}%"
                tvBattery.setTextColor("#D32F2F".toColorInt())
                rowContainer.setBackgroundColor("#FFEBEE".toColorInt())
            }
            device.batteryLevel <= 50 -> {
                // Medium battery — show in orange
                tvBattery.text = "🔋 ${device.batteryLevel}%"
                tvBattery.setTextColor("#F57C00".toColorInt())
                rowContainer.setBackgroundColor("#FFFFFF".toColorInt())
            }
            else -> {
                // Good battery — show in green
                tvBattery.text = "🔋 ${device.batteryLevel}%"
                tvBattery.setTextColor("#388E3C".toColorInt())
                rowContainer.setBackgroundColor("#FFFFFF".toColorInt())
            }
        }

        return view
    }
}