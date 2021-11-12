package com.mi12.deadreckoning;

import android.Manifest;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, OnMapReadyCallback {

    final double DELTA = 6;

    Button buttonStart;
    Button buttonReset;
    TextView textNumberStep;

    SensorManager sensorManager;
    Sensor sensor;
    boolean isAccelerometerAvailable = false;

    double magnitudePrevious = 0;
    int stepCount = 0;

    boolean isPermissionLocationGranted;
    SupportMapFragment mapFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        checkMyPermission();

        // Find item
        buttonStart = findViewById(R.id.buttonStart);
        buttonReset = findViewById(R.id.buttonReset);
        textNumberStep = findViewById(R.id.textNumberStep);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            isAccelerometerAvailable = true;
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            isAccelerometerAvailable = false;
            Toast.makeText(this, "Sensor Accelerometer not found", Toast.LENGTH_SHORT).show();
        }

        // show map
        if (isPermissionLocationGranted){
            mapFragment.getMapAsync(this);
        }
    }

    private void checkMyPermission() {
        Dexter
                .withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        Toast.makeText(MainActivity.this, "Permission Location Granted", Toast.LENGTH_SHORT).show();
                        isPermissionLocationGranted = true;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), "");
                        intent.setData(uri);
                        startActivity(intent);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                })
                .check();
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
            textNumberStep.setText(String.valueOf(stepCount));
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
                textNumberStep.setText(String.valueOf(stepCount));
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

    }
}