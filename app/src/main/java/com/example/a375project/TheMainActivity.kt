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
import com.example.a375project.databinding.ActivityTheMainBinding
import io.github.controlwear.virtual.joystick.android.JoystickView
import kotlin.math.log

class TheMainActivity: AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var boat: ImageView
    private lateinit var angleShower: TextView
    private lateinit var throttleShower: TextView
    private lateinit var blueToothConnect: ImageButton
    private lateinit var bAdapter: BluetoothAdapter
    private lateinit var bSocket: BluetoothSocket
    private lateinit var joyStick: JoystickView
    private lateinit var binding: ActivityTheMainBinding
    private var throttle: Int = 0
    private var sidesSending: Int = 0
    private var sideToSend: Int = 0
    private var sidesSendingString: String = ""
    private var isConnected: Boolean = false

    private val REQUEST_CODE_ENABLE_BT: Int = 1;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTheMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        angleShower = findViewById(R.id.angelShower)
        boat = findViewById(R.id.boat)
        blueToothConnect = findViewById(R.id.blueToothConnect)
        joyStick = findViewById(R.id.joystick)
        throttleShower = findViewById(R.id.throttleNum)

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

            boat.apply {
                rotationY = sides * 3f  //The rotaion of the image
                rotation = sides
            }

            angleShower.text = "Left/Right ${sides.toInt()}"

            sidesSending = (sides.toInt()*10) + 90
            sidesSendingString = "<" + (sidesSending).toString() + ">"
            sideToSend = 90

            if(sideToSend != sidesSending && isConnected){
                sideToSend = (sides.toInt() *10) + 90
                bSocket.outputStream.write(sidesSendingString.toByteArray(Charsets.UTF_8))
            }



            if(isConnected) {
                joyStick.setOnMoveListener { angle, strength ->
                    var throttleToSend = "<" + (throttle).toString() + ">"
                    bSocket.outputStream.write(throttleToSend.toByteArray(Charsets.UTF_8))
                    throttle = strength * 2
                }
            }else{
                joyStick.setOnMoveListener {angle, strength ->
                    throttleShower.text = "Throttle Percent ${strength.toString()}"
                }
            }

        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }

    //This function is what is called when the switch to change to joystick controls is on
    fun changedControls(){

    }

    //This is the function that is called when the bluetooth button is clicked
    @SuppressLint("MissingPermission")
    fun connectBlutoth() {
        bAdapter = BluetoothAdapter.getDefaultAdapter() //Gets Adapter
        var deviceToConnectTo: BluetoothDevice
        //The if/else statement detects if Bluetooth is already enabled
        if (bAdapter.isEnabled) {
            val devices = bAdapter.bondedDevices
            for (device in devices) {       //Gets a list bonded devices
                val deviceName = device.name
                val deviceAddress = device
                if (device.address.equals("98:DA:60:05:77:69")) {       //Checking for the Boat's Mac addres
                    deviceToConnectTo = device
                    isConnected = true;
                    Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show()
                }
                //If everything is setup before starting
                Toast.makeText(this, "Already On and Connected", Toast.LENGTH_LONG).show()
            }
        }else{
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)     //Ask the user on a pop-up window to enable Bluetooth
            startActivityForResult(intent, REQUEST_CODE_ENABLE_BT);

            if (bAdapter.isEnabled) {              //Same as if Bluetooth was on to begin on so the user doesn't have to click the button twice
                val devices = bAdapter.bondedDevices
                for (device in devices) {
                    val deviceName = device.name
                    val deviceAddress = device
                    if (device.address.equals("98:DA:60:05:77:69")) {
                        deviceToConnectTo = device
                        isConnected = true;
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