package com.signsense.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.signsense.app.analysis.HandDetector;

import java.io.IOException;
import java.util.Locale;

public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "VideoActivity";
    private static final int CODE_VIDEO = 400;

    private Button selectVideo;
    private VideoView videoView;

    private MediaController mediaController;
    private HandDetector handDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        selectVideo = findViewById(R.id.button_selectVideo);
        videoView = findViewById(R.id.view_videoView);

        // Attach media controller to video view (pause, skip, etc.) based on size
        videoView.setOnPreparedListener(mp -> mp.setOnVideoSizeChangedListener((mp1, width, height) -> {
            mediaController = new MediaController(VideoActivity.this);
            videoView.setMediaController(mediaController);
            mediaController.setAnchorView(videoView);
        }));

        // Setup hand detector for video mode
        handDetector = new HandDetector(getApplicationContext(), RunningMode.VIDEO);

        selectVideo.setOnClickListener(view -> {
            // On click, open new window for selecting videos
            Intent selectVideo = new Intent(Intent.ACTION_PICK);
            selectVideo.setType("video/*");
            startActivityForResult(selectVideo, CODE_VIDEO);
        });

        loadSettings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_VIDEO) {
            // Get video Uri and attach it to video view
            Uri videoUri = data.getData();
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(videoUri);
            videoView.start();

            try {
                handDetector.detectVideo(videoUri, 500);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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