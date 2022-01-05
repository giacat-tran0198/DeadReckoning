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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener, OnMapReadyCallback, CompoundButton.OnCheckedChangeListener {

    private static final int PHYISCAL_ACTIVITY = 0;
    private static final int eRadius = 6371000; //rayon de la terre en m
    private static int DELTA = 5;

    Button buttonStart;
    Button buttonReset;
    TextView textNumberStep;
    EditText editTextSizeStep;
    EditText editTextAngle;
    Switch switchStepSensor;
    com.google.android.material.slider.Slider editDELTA;

    double magnitudePrevious = 0;
    int stepCount = 0;
    float stepSize = 0.9f;
    float correctionFab = 17f;
    float[] orientationVals = new float[3];
    float[] mRotationMatrixFromVector = new float[16];
    float[] mRotationMatrix = new float[16];
    boolean switchStepAvailable;

    SupportMapFragment mapFragment;
    GoogleMap googleMap;
    LatLng defaultDepart = new LatLng(49.40019786621855, 2.800168926152195); //Hall PG
    // Test BF
//    LatLng defaultDepart = new LatLng(49.415495420197736, 2.819529127135657);
    Location currentLocation = new Location("Current Location");
    Context mContext;
    PopupWindow mPopupWindow;
    List<LatLng> listStep;
    Marker currentMarker;

    SensorManager sensorManager;
    Sensor SDSensor;
    Sensor RotSensor;
    Sensor AccSensor;

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

        editTextSizeStep = findViewById(R.id.sizeField);
        editTextAngle = findViewById(R.id.correctionAngle);
        editDELTA = findViewById(R.id.sliderDELTA);

        textNumberStep = findViewById(R.id.textNumberStep);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        SDSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        RotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        AccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        // Get the application context
        mContext = getApplicationContext();

        initListStep();

        // Select switch step sensor
        switchStepSensor = findViewById(R.id.switchStepSensor);
    }

    private void onChangeTextButtonStart() {
        buttonStart.setText(getString(buttonStart.isSelected() ? R.string.stop : R.string.start));
    }

    private void initListStep() {
        listStep = new ArrayList<>();
        listStep.add(defaultDepart);

        currentLocation.setLatitude(defaultDepart.latitude);
        currentLocation.setLongitude(defaultDepart.longitude);

        if (googleMap != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(defaultDepart).title("Default"));
            setCameraFollowedPosition(defaultDepart);
        }
    }

    private void drawPolyline() {
        PolylineOptions poly = new PolylineOptions()
                .width(5)
                .color(Color.BLUE)
                .geodesic(true)
                .addAll(listStep);
        googleMap.addPolyline(poly);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.buttonStart) {
            buttonStart.setSelected(!buttonStart.isSelected());
            onChangeTextButtonStart();
            editTextSizeStep.setFocusableInTouchMode(false);
            editTextSizeStep.setFocusable(false);
            editTextAngle.setFocusableInTouchMode(false);
            editTextAngle.setFocusable(false);
            editDELTA.setFocusableInTouchMode(false);
            editDELTA.setFocusable(false);
            if (!buttonStart.isSelected()) {
                editTextSizeStep.setFocusableInTouchMode(true);
                editTextSizeStep.setFocusable(true);
                editTextAngle.setFocusableInTouchMode(true);
                editTextAngle.setFocusable(true);
                editDELTA.setFocusableInTouchMode(true);
                editDELTA.setFocusable(true);
            }

            String height = editTextSizeStep.getText().toString();
            String angle = editTextAngle.getText().toString();
            DELTA = (int) editDELTA.getValue();
            if (height.length() > 0) {
                float finalHeight = Float.parseFloat(height);
                stepSize = finalHeight / 215;
            }
            if (angle.length() > 0) {
                correctionFab = Float.parseFloat(angle);
            }

        } else if (id == R.id.buttonReset) {
            buttonStart.setSelected(false);
            onChangeTextButtonStart();

            stepCount = 0;
            magnitudePrevious = 0;
            textNumberStep.setText(String.valueOf(stepCount));
            editTextSizeStep.getText().clear();
            editTextSizeStep.setFocusableInTouchMode(true);
            editTextSizeStep.setFocusable(true);

            editTextAngle.getText().clear();
            editTextAngle.setFocusableInTouchMode(true);
            editTextAngle.setFocusable(true);

            editDELTA.setValue(5);
            editDELTA.setFocusableInTouchMode(true);
            editDELTA.setFocusable(true);

            initListStep();

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            if (SDSensor != null) {
                switchStepAvailable = true;
                Toast.makeText(this, "Step Detector turned on", Toast.LENGTH_SHORT).show();
            } else {
                switchStepSensor.setChecked(false);
                Toast.makeText(this, "Step Detector unavailable", Toast.LENGTH_SHORT).show();
            }
        } else {
            switchStepAvailable = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!buttonStart.isSelected()) return;
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrixFromVector, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, orientationVals);

            orientationVals[0] = (float) orientationVals[0];
            orientationVals[1] = (float) orientationVals[1];
            orientationVals[2] = (float) orientationVals[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR && switchStepAvailable) {
            calNextPos(orientationVals[0] + (float) Math.toRadians(correctionFab));
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !switchStepAvailable) {
            float x_acceleration = event.values[0];
            float y_acceleration = event.values[1];
            float z_acceleration = event.values[2];
            double magnitude = Math.sqrt(x_acceleration * x_acceleration + y_acceleration * y_acceleration + z_acceleration * z_acceleration);
            double magnitudeDelta = magnitude - magnitudePrevious;
            magnitudePrevious = magnitude;

            if (magnitudeDelta > DELTA) {
                calNextPos(orientationVals[0] - (float) Math.toRadians(5f));
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }



    @Override
    protected void onResume() {
        super.onResume();

        // Simple function that registers all of the required sensor listeners with the sensor manager.
        // Register the listeners. Used for receiving notifications from
        // the SensorManager when sensor values have changed.
        sensorManager.registerListener(this, RotSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, SDSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, AccSensor, SensorManager.SENSOR_DELAY_NORMAL);


        // Set event
        buttonStart.setOnClickListener(this);
        buttonReset.setOnClickListener(this);

        switchStepSensor.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Simple function that un-registers all of the required sensor listeners from the sensor manager.
        // Perform un-registration of the sensor listeners
        sensorManager.unregisterListener(this, RotSensor);
        sensorManager.unregisterListener(this, SDSensor);
        sensorManager.unregisterListener(this, AccSensor);

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(defaultDepart).title("Default").snippet("Default"));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(defaultDepart).zoom(20).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        googleMap.getUiSettings().setCompassEnabled(true);

        this.googleMap = googleMap;
    }

    private void calNextPos(float bearing) {
        // Prise en compte du nouveau pas
        stepCount++;
        textNumberStep.setText(String.valueOf(stepCount));

        currentLocation = computeNextStep(stepSize, bearing, currentLocation);
        LatLng latlng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        listStep.add(latlng);

        setCameraFollowedPosition(latlng);

        if (currentMarker != null) currentMarker.remove();
        currentMarker = googleMap.addMarker(new MarkerOptions().position(latlng).title("Current").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        drawPolyline();
    }

    private void setCameraFollowedPosition(LatLng currLatLng){
        CameraPosition cameraPosition = new CameraPosition.Builder().target(currLatLng).zoom(20).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Calcule la nouvelle position de l'utilisateur à partir de la courante
     *
     * @param stepSize la taille du pas qu'a fait l'utilisateur
     * @param bearing  l'angle de direction
     * @param currentLocation la location currently
     * @return la nouvelle localisation
     */
    public Location computeNextStep(float stepSize, float bearing, Location currentLocation) {
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

        //mettre a jour l'angle
        newLoc.setBearing((currentLocation.getBearing() + 180) % 360);

        return newLoc;
    }

}