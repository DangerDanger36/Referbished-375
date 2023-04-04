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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.a375project.databinding.ActivityTheMainBinding
import io.github.controlwear.virtual.joystick.android.JoystickView
import kotlin.math.log

class TheMainActivity: AppCompatActivity(), SensorEventListener {

    //Global Vars
    private lateinit var sensorManager: SensorManager
    private lateinit var boat: ImageView
    private lateinit var angleShower: TextView
    private lateinit var throttleShower: TextView
    private lateinit var blueToothConnect: ImageButton
    private lateinit var bAdapter: BluetoothAdapter
    private lateinit var bSocket: BluetoothSocket
    private lateinit var joyStick: JoystickView
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var switchControls: Switch
    private lateinit var binding: ActivityTheMainBinding
    private var throttle: Int = 0
    private var sidesSending: Int = 0
    private var sideToSend: Int = 0
    private var sidesSendingString: String = ""
    private var angleSending: Int = 0
    private var angleSendingString: String = ""
    private var angleToSend: Int = 0
    private  var throttleToSend: String = ""
    private var isConnected: Boolean = false

    //Do not change code used in Bluetooth to send a code to the phone
    private val REQUEST_CODE_ENABLE_BT: Int = 1;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTheMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //intalizing the front-end aspects that this back-end changes or uses to present data to the user
        angleShower = findViewById(R.id.angelShower)
        boat = findViewById(R.id.boat)
        blueToothConnect = findViewById(R.id.blueToothConnect)
        joyStick = findViewById(R.id.joystick)
        throttleShower = findViewById(R.id.throttleNum)
        switchControls = findViewById(R.id.switchControls)

        //if button is clicked then connect to the boat
        blueToothConnect.setOnClickListener(View.OnClickListener() {
            fun onClick(){
                connectBlutoth()
            }
        })

        setUpSensor()

        //if the differnt controls switch is active, then switches to joystick controls
        switchControls.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                switchControls.setText(switchControls.textOff)
                sensorManager.unregisterListener(this)
                changedControls()
            }else {
                switchControls.setText(switchControls.textOn)
                setUpSensor()
            }
        }

    }

    //Sets up the Accelerometer for sending the direction to the boat and to manipulate the boat image
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
                rotationY = sides * 3f  //The rotaion of the image on the Y axis (Left and Right)
                //rotation = sides      //Commented out to lock the rotaion of the Boat, so the image does not contort
            }

            angleShower.text = "Left/Right ${sides.toInt()}"

            if(sideToSend != sidesSending && isConnected){      //only when the boat is connected will it try to write to the boat to turn
                sidesSending = (sides.toInt()*10) + 90
                sidesSendingString = "<" + (sidesSending).toString() + ">"      //Lines 85-87 are the converstions to send to the Aurdino by converting the data into an bitarray
                sideToSend = 90

                sideToSend = (sides.toInt() *10) + 90
                bSocket.outputStream.write(sidesSendingString.toByteArray(Charsets.UTF_8))
            }



            if(isConnected) {       //if the Boat is connected to the phone to actully control the throttle
                joyStick.setOnMoveListener { angle, strength ->
                    var throttleToSend = "<" + (throttle).toString() + ">"
                    throttleShower.text = "Throttle Percent ${strength.toString()}"
                    bSocket.outputStream.write(throttleToSend.toByteArray(Charsets.UTF_8))
                    throttle = strength * 2
                }
            }else{                  //Otherwise it shows only shows how much throttle you are giving it, helps when testing
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
        sensorManager.unregisterListener(this)
        if(angleToSend != angleSending && isConnected){
            joyStick.setOnMoveListener { angle, strength ->
                angleShower.text = "Left/Right ${angle}"
                throttleShower.text = "Throttle Percent ${strength.toString()}"

                angleSending = (angle.toInt() * 10) + 90
                angleSendingString = "<" + (angleSending).toString() + ">"      //Lines 131-133 are the converstions to send to the Aurdino by converting the data into an bitarray
                angleToSend = 90

                angleToSend = (angle.toInt() *10) + 90

                throttleToSend = "<" + (throttle).toString() + ">"
                throttle = strength * 2

                bSocket.outputStream.write(throttleToSend.toByteArray(Charsets.UTF_8))
                bSocket.outputStream.write(angleSendingString.toByteArray(Charsets.UTF_8))
            }
        }else{
                joyStick.setOnMoveListener { angle, strength ->
                    throttleShower.text = "Throttle Percent ${strength.toString()}"
                    angleShower.text = "Left/Right ${angle}"

                }
            }
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