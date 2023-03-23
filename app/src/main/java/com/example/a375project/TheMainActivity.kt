package com.example.a375project

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.controlwear.virtual.joystick.android.JoystickView

class TheMainActivity: AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var boat: ImageView
    private lateinit var angleShower: TextView
    private lateinit var blueToothConnect: ImageButton
    private lateinit var bAdapter: BluetoothAdapter
    private lateinit var bSocket: BluetoothSocket
    private lateinit var joyStick: JoystickView
    private var throttle: Int = 0
    private var sidesSending: Int = 0
    private var sideToSend: Int = 0
    private var sidesSendingString: String = ""

    private val REQUEST_CODE_ENABLE_BT: Int = 1;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        boat = findViewById(R.id.boat)
        angleShower = findViewById(R.id.angelShower)
        blueToothConnect = findViewById(R.id.blueToothConnect)

        blueToothConnect.setOnClickListener(View.OnClickListener() {
            fun onClick(){
                connectBlutoth()
            }
        })

        setUpSensor()

    }

    private fun setUpSensor(){
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            val sides = event.values[0]
            val upDown = event.values[1]

            boat.apply {
                val matrix = Matrix()
                //HI
                val pivotX = upDown * 3f
                val pivotY = sides * 3f

                boat.setScaleType(ImageView.ScaleType.MATRIX)

                matrix.postRotate(android.R.attr.angle.toFloat(), pivotX, pivotY)
                boat.setImageMatrix(matrix)
            }

            angleShower.text = "Left/Right ${sides.toInt()}"

            sidesSending = (sides.toInt()*10) + 90
            sidesSendingString = "<" + (sidesSending).toString() + ">"
            sideToSend = 90

            if(sideToSend != sidesSending){
                sideToSend = (sides.toInt() *10) + 90
                bSocket.outputStream.write(sidesSendingString.toByteArray(Charsets.UTF_8))
            }

            joyStick.setOnMoveListener { angle, strength ->
                var throttleToSend = "<" + (throttle).toString() + ">"
                bSocket.outputStream.write(throttleToSend.toByteArray(Charsets.UTF_8))
                throttle = strength*2
            }

        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }
    @SuppressLint("MissingPermission")
    fun connectBlutoth() {
        bAdapter = BluetoothAdapter.getDefaultAdapter()
        var deviceToConnectTo: BluetoothDevice
        if (bAdapter.isEnabled) {
            val devices = bAdapter.bondedDevices
            for (device in devices) {
                val deviceName = device.name
                val deviceAddress = device
                if (device.address.equals("98:DA:60:05:77:69")) {
                    deviceToConnectTo = device
                    Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show()
                }

                Toast.makeText(this, "Already On and Connected", Toast.LENGTH_LONG).show()
            }
        }else{
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, REQUEST_CODE_ENABLE_BT);

            if (bAdapter.isEnabled) {
                val devices = bAdapter.bondedDevices
                for (device in devices) {
                    val deviceName = device.name
                    val deviceAddress = device
                    if (device.address.equals("98:DA:60:05:77:69")) {
                        deviceToConnectTo = device
                        Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show()
                    }

                    Toast.makeText(this, "On and Connected", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

}