package com.example.a375project

import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity: AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var boat: ImageView
    private lateinit var angleShower: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        boat = findViewById(R.id.boat)

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

        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }

}