package com.signsense.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void switchCamera(View view) { // Launches the camera part
        Intent cameraActivity = new Intent(this, CameraActivity.class);
        startActivity(cameraActivity);
    }
}