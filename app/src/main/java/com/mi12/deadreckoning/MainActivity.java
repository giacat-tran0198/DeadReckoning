package com.mi12.deadreckoning;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, OnMapReadyCallback {

    final double DELTA = 6;

    Button buttonStart;
    Button buttonReset;
    Button buttonRemind;

    TextView textNumberStep;

    SensorManager sensorManager;
    Sensor sensor;
    boolean isAccelerometerAvailable = false;

    double magnitudePrevious = 0;
    int stepCount = 0;
    float stepSize = 0.8f;

    boolean isPermissionLocationGranted;
    SupportMapFragment mapFragment;
    GoogleMap googleMap;
    PolylineOptions poly = new PolylineOptions();
    LatLng defaultDepart = new LatLng(49.40019786621855, 2.800168926152195);
    Location currentLocation = new Location("Current Location");


    Context mContext;

    PopupWindow mPopupWindow;

    StepPositioningHandler sph;
    DeviceAttitudeHandler dah;

    List<LatLng> listStep = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        checkMyPermission();

        // Find item
        buttonStart = findViewById(R.id.buttonStart);
        buttonReset = findViewById(R.id.buttonReset);
        buttonRemind = findViewById(R.id.buttonRemind);

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
//        Uri gmmIntentUri = Uri.parse("geo:49.40031263306482, 2.8002218997795527");
//        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//        mapIntent.setPackage("com.google.android.apps.maps");
//        startActivity(mapIntent);


        if (isPermissionLocationGranted) {
            mapFragment.getMapAsync(this);
        }

        // Get the application context
        mContext = getApplicationContext();
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

    private void resetListStep() {
        listStep = new ArrayList<>();
    }

    private void drawPolyline(Location location) {
        LatLng newLoc = new LatLng(location.getLongitude(), location.getLatitude());
        poly.add(newLoc);
        googleMap.addPolyline(poly);
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

            resetListStep();

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

                // Get new location and add the line on the map
                Location newLocation = sph.computeNextStep(stepSize, dah.orientationVals[0]);
                LatLng latlng = new LatLng (newLocation.getLongitude(),newLocation.getLatitude());
                CameraPosition cameraPosition = new CameraPosition.Builder().target(latlng).zoom(20).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                drawPolyline(newLocation);

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Set event
        buttonStart.setOnClickListener(this);
        buttonReset.setOnClickListener(this);
        buttonRemind.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensor != null) {
            sensorManager.unregisterListener(this, sensor);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        currentLocation.setLatitude(49.40019786621855);
        currentLocation.setLongitude(2.800168926152195);
        googleMap.addMarker(new MarkerOptions().position(defaultDepart).title("Default").snippet("Default"));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(defaultDepart).zoom(20).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        poly.add(defaultDepart);

        googleMap.addPolyline(poly);

    }


    public class StepPositioningHandler {

        private Location mCurrentLocation;
        private static final int eRadius = 6371000; //rayon de la terre en m

        /** Calcule la nouvelle position de l'utilisateur à partir de la courante
         * @param stepSize la taille du pas qu'a fait l'utilisateur
         * @param bearing l'angle de direction
         * @return la nouvelle localisation
         */
        public Location computeNextStep(float stepSize,float bearing) {
            Location newLoc = new Location(mCurrentLocation);
            float angDistance = stepSize / eRadius;
            double oldLat = mCurrentLocation.getLatitude();
            double oldLng = mCurrentLocation.getLongitude();
            double newLat = Math.asin( Math.sin(Math.toRadians(oldLat))*Math.cos(angDistance) +
                    Math.cos(Math.toRadians(oldLat))*Math.sin(angDistance)*Math.cos(bearing) );
            double newLon = Math.toRadians(oldLng) +
                    Math.atan2(Math.sin(bearing)*Math.sin(angDistance)*Math.cos(Math.toRadians(oldLat)),
                            Math.cos(angDistance) - Math.sin(Math.toRadians(oldLat))*Math.sin(newLat));
            //reconversion en degres
            newLoc.setLatitude(Math.toDegrees(newLat));
            newLoc.setLongitude(Math.toDegrees(newLon));

            newLoc.setBearing((mCurrentLocation.getBearing()+180)% 360);
            mCurrentLocation = newLoc;

            return newLoc;
        }

    }

    public class DeviceAttitudeHandler {

        SensorManager sm;
        Sensor sensor;
        private float[] mRotationMatrixFromVector = new float[9];
        private float[] mRotationMatrix = new float[9];
        public float[] orientationVals = new float[3];
        private final int sensorType = Sensor.TYPE_ROTATION_VECTOR;

        public DeviceAttitudeHandler(SensorManager sm) {
            super();
            this.sm = sm;
            sensor = sm.getDefaultSensor(sensorType);
        }

        public void start() {
            sm.registerListener((SensorEventListener) this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        public void stop() {
            sm.unregisterListener((SensorEventListener) this);
        }

        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        public void onSensorChanged(SensorEvent event) {
            // Convert the rotation-vector to a 4x4 matrix.
            SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector,
                    event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrixFromVector,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);

            orientationVals[0] = (float) orientationVals[0];
            orientationVals[1] = (float) orientationVals[1]; // axe de rotation
            orientationVals[2] = (float) orientationVals[2];

        }

    }

}