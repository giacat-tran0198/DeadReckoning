package com.mi12.deadreckoning;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, OnMapReadyCallback {

    private static final int PHYISCAL_ACTIVITY = 0;
    //    final double DELTA = 3;
    private static final int eRadius = 6371000; //rayon de la terre en m
    Button buttonStart;
    Button buttonReset;
    Button buttonRemind;
    TextView textNumberStep;
    SensorManager sensorManager;
    double magnitudePrevious = 0;
    int stepCount = 0;
    float stepSize = 0.9f;
    float[] orientationVals = new float[3];
    float[] mRotationMatrix = new float[9];
    //    boolean isPermissionLocationGranted;
    SupportMapFragment mapFragment;
    GoogleMap googleMap;
    LatLng defaultDepart = new LatLng(49.40019786621855, 2.800168926152195);
    // Test BF
//    LatLng defaultDepart = new LatLng(49.415495420197736, 2.819529127135657);
    Location currentLocation = new Location("Current Location");
    Context mContext;
    PopupWindow mPopupWindow;
    List<LatLng> listStep;
    Polyline line;

    private Sensor SDSensor;
    private Sensor RotSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
            //ask for permission
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, PHYISCAL_ACTIVITY);
        }

        // Find item
        buttonStart = findViewById(R.id.buttonStart);
        buttonReset = findViewById(R.id.buttonReset);
        buttonRemind = findViewById(R.id.buttonRemind);

        textNumberStep = findViewById(R.id.textNumberStep);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        SDSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        RotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Register the sensors for event callback
        registerSensors();

        // Get the application context
        mContext = getApplicationContext();

        initListStep();
    }

    private void onChangeTextButtonStart() {
        buttonStart.setText(getString(buttonStart.isSelected() ? R.string.stop : R.string.start));
    }

    private void initListStep() {
        listStep = new ArrayList<>();
        listStep.add(defaultDepart);

        currentLocation.setLatitude(defaultDepart.latitude);
        currentLocation.setLongitude(defaultDepart.longitude);

        line.remove();
    }

    private void drawPolyline() {
        PolylineOptions poly = new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true)
                .addAll(listStep);
        line = googleMap.addPolyline(poly);
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

            initListStep();

        } else if (id == R.id.buttonRemind) {
            // Initialize a new instance of LayoutInflater service
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

            // Inflate the custom layout/view
            View customView = inflater.inflate(R.layout.popup, null);

            // Initialize a new instance of popup window
            mPopupWindow = new PopupWindow(
                    customView,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );

            // Set an elevation value for popup window
            // Call requires API level 21
            mPopupWindow.setElevation(5.0f);

            // Get a reference for the custom view close button
            ImageButton closeButton = customView.findViewById(R.id.ib_close);

            // Set a click listener for the popup window close button
            // Dismiss the popup window
            closeButton.setOnClickListener(view -> mPopupWindow.dismiss());

            // Finally, show the popup window at the center location of root relative layout
            mPopupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && buttonStart.isSelected()) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR && buttonStart.isSelected()) {
            stepCount++;
            textNumberStep.setText(String.valueOf(stepCount));

            //Tentative de correction Fab
            orientationVals[0] = (float) (orientationVals[0] - Math.toRadians(8));

            Location newLocation = computeNextStep(stepSize, orientationVals[0]);
            LatLng latlng = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
            listStep.add(latlng);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(latlng).zoom(20).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            drawPolyline();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensors();

        // Set event
        buttonStart.setOnClickListener(this);
        buttonReset.setOnClickListener(this);
        buttonRemind.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unRegisterSensors();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.addMarker(new MarkerOptions().position(defaultDepart).title("Default").snippet("Default"));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(defaultDepart).zoom(20).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    // Simple function that registers all of the required sensor listeners
    // with the sensor manager.
    private void registerSensors() {
        // Register the listeners. Used for receiving notifications from
        // the SensorManager when sensor values have changed.
        sensorManager.registerListener(this, RotSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, SDSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // Simple function that un-registers all of the required sensor listeners
    // from the sensor manager.
    private void unRegisterSensors() {
        // Perform un-registration of the sensor listeners
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR));
    }

    /**
     * Calcule la nouvelle position de l'utilisateur à partir de la courante
     *
     * @param stepSize la taille du pas qu'a fait l'utilisateur
     * @param bearing  l'angle de direction
     * @return la nouvelle localisation
     */
    public Location computeNextStep(float stepSize, float bearing) {
        Location newLoc = new Location(currentLocation);
        float angDistance = stepSize / eRadius;
        double oldLat = currentLocation.getLatitude();
        double oldLng = currentLocation.getLongitude();
        double newLat = Math.asin(Math.sin(Math.toRadians(oldLat)) * Math.cos(angDistance) +
                Math.cos(Math.toRadians(oldLat)) * Math.sin(angDistance) * Math.cos(bearing));
        double newLon = Math.toRadians(oldLng) +
                Math.atan2(Math.sin(bearing) * Math.sin(angDistance) * Math.cos(Math.toRadians(oldLat)),
                        Math.cos(angDistance) - Math.sin(Math.toRadians(oldLat)) * Math.sin(newLat));
        //reconversion en degres
        newLoc.setLatitude(Math.toDegrees(newLat));
        newLoc.setLongitude(Math.toDegrees(newLon));


        newLoc.setBearing((currentLocation.getBearing() + 180) % 360);

        currentLocation = newLoc;

        return newLoc;
    }

}