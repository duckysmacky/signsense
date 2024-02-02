package com.signsense.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import org.jetbrains.annotations.NotNull;
import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "Unable to load OpenCV!");
        } else {
            Log.d("OpenCV", "OpenCV loaded Successfully!");
        }

        askPermissions();
        loadSettings();
    }

    public void switchCamera(View view) { // Launches the camera part
        Intent activity = new Intent(this, CameraActivity.class);
        startActivity(activity);
    }

    public void switchImageSelector(View view) { // Launch Image selector
        Intent activity = new Intent(this, VideoActivity.class);
        startActivity(activity);
    }

    public void switchSettings(View view) { // Launch Settings
        Intent activity = new Intent(this, SettingsActivity.class);
        startActivity(activity);
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        TextView appThemeText = (TextView) findViewById(R.id.text_appTheme);
        appThemeText.setText("Theme: " + preferences.getString("theme", ""));

        /*
        TODO:
         - Change main activity to fragment (for settings loading)
         - Load from settings to app
         */
    }

    // Permission asking
    private void askPermissions() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 103);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 103 && grantResults.length > 0) { // Check if the code is for askPermissions and availability to grant is there
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) { // If we haven't already granted this permission ask for it again
                askPermissions();
            }
        }
    }
}