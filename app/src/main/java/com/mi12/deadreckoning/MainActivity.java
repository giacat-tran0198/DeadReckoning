package com.mi12.deadreckoning;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    final double DELTA = 6;

    Button buttonStart;
    Button buttonReset;
    TextView textNumberStep;

    SensorManager sensorManager;
    Sensor sensor;
    boolean isAccelerometerAvailable = false;

    double magnitudePrevious = 0;
    Integer stepCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Find item
        buttonStart = findViewById(R.id.buttonStart);
        buttonReset = findViewById(R.id.buttonReset);
        textNumberStep = findViewById(R.id.textNumberStep);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            isAccelerometerAvailable = true;
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            isAccelerometerAvailable = false;
            Toast.makeText(this, "Sensor Accelerometer not found", Toast.LENGTH_SHORT).show();
        }

    }

    private void onChangeTextButtonStart() {
        buttonStart.setText(getString(buttonStart.isSelected() ? R.string.stop : R.string.start));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.buttonStart) {
            buttonStart.setSelected(!buttonStart.isSelected());
            onChangeTextButtonStart();
        } else if (id == R.id.buttonReset) {
            buttonStart.setSelected(false);
            onChangeTextButtonStart();

            stepCount = 0;
            magnitudePrevious = 0;
            textNumberStep.setText(stepCount);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == sensor && isAccelerometerAvailable && buttonStart.isSelected()) {
            float x_acceleration = event.values[0];
            float y_acceleration = event.values[1];
            float z_acceleration = event.values[2];

            double magnitude = Math.sqrt(Math.sqrt(x_acceleration) + Math.sqrt(y_acceleration) + Math.sqrt(z_acceleration));
            double magnitudeDelta = magnitude - magnitudePrevious;
            magnitudePrevious = magnitude;

            if (magnitudeDelta > DELTA) {
                stepCount++;
                textNumberStep.setText(stepCount);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensor != null){
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Set event
        buttonStart.setOnClickListener(this);
        buttonReset.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensor != null){
            sensorManager.unregisterListener(this, sensor);
        }
    }
}