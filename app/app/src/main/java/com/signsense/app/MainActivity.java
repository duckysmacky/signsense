package com.signsense.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
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

        loadSettings();
    }

    public void switchCamera(View view) { // Launches the camera part
        Intent activity = new Intent(this, CameraActivity.class);
        startActivity(activity);
    }

    public void switchImageSelector(View view) { // Launch Image selector
        Intent activity = new Intent(this, VideoActivity.class);
        startActivity(activity);

        /*
        TODO:
         - Make video (media) selector instead of photo selector
         - Make separate function to detect hands on video
         */
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
         - Add app settings
         - Add model settings
         - Change main activity to fragment (for settings loading)
         - Add russian localisation
         */
    }
}