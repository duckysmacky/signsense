package com.signsense.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import org.opencv.android.OpenCVLoader;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main";
    private static final int CODE_REQUEST_CAMERA = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initLocal()) {
            Log.d("OpenCV", "OpenCV loaded Successfully!");
        } else {
            Log.e("OpenCV", "Unable to load OpenCV!");
        }

        loadSettings();
    }

    public void switchCamera(View view) { // Launches the camera part
        Toast.makeText(this, "Opening camera...", Toast.LENGTH_LONG).show();
        Intent activity = new Intent(this, CameraActivity.class);
        startActivity(activity);
    }

    public void switchInfo(View view) {
        Intent activity = new Intent(this, InfoActivity.class);
        startActivity(activity);
    }

    public void switchSettings(View view) { // Launch Settings
        Intent activity = new Intent(this, SettingsActivity.class);
        startActivity(activity);
    }

    private void loadSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String appTheme = preferences.getString("theme", "");
        String appLanguage = preferences.getString("appLanguage", "");

        switch (appTheme) {
            case "sync":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        Locale locale = new Locale(appLanguage);
        Resources resources = this.getResources();

        Locale.setDefault(locale);
        resources.getConfiguration().setLocale(locale);
        resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
    }

    // Permission asking
    private void askPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions granted");
        } else {
            Log.d(TAG, "Permission prompt");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODE_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera can be turned on
                Log.i(TAG, "Camera permission granted");
            } else {
                // Camera will stay off
                Log.i(TAG, "Camera permission denied");
                askPermissions();
            }
        }
    }
}