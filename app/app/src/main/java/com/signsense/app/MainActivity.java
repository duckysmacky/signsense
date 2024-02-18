package com.signsense.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import org.opencv.android.OpenCVLoader;

import java.util.Locale;

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

        loadSettings();
    }

    public void switchCamera(View view) { // Launches the camera part
        Toast.makeText(this, "Opening camera...", Toast.LENGTH_LONG).show();
        Intent activity = new Intent(this, CameraActivity.class);
        startActivity(activity);
    }

    public void switchImageSelector(View view) { // Launch Image selector
        Intent activity = new Intent(this, VideoActivity.class);
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
}