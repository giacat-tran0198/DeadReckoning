package com.mi12.deadreckoning;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button buttonStart;
    Button buttonReset;
    TextView textStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find item
        buttonStart = findViewById(R.id.buttonStart);
        buttonReset = findViewById(R.id.buttonReset);
        textStep = findViewById(R.id.textStep);

        // Set event
        buttonStart.setOnClickListener(v -> {
            buttonStart.setSelected(!buttonStart.isSelected());
            onChangeTextButtonStart();
        });
        buttonReset.setOnClickListener(v -> {
            buttonStart.setSelected(false);
            onChangeTextButtonStart();
        });

    }


    private void onChangeTextButtonStart() {
        buttonStart.setText(getString(buttonStart.isSelected() ? R.string.stop : R.string.start));
    }
}