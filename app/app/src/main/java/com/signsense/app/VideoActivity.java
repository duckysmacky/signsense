package com.signsense.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.signsense.app.analysis.HandDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "VideoActivity";
    private static final int CODE_VIDEO = 400;

    private VideoView videoView;
    private TextView lastWords;

    private MediaController mediaController;
    private HandDetector handDetector;
    //private HandAnalyser handAnalyser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        Button selectVideo1 = findViewById(R.id.button_selectVideo);
        videoView = findViewById(R.id.view_videoView);

        lastWords = findViewById(R.id.text_video_translation_lastwords_value);

        // Attach media controller to video view (pause, skip, etc.) based on size
        videoView.setOnPreparedListener(mp -> mp.setOnVideoSizeChangedListener((mp1, width, height) -> {
            mediaController = new MediaController(VideoActivity.this);
            videoView.setMediaController(mediaController);
            mediaController.setAnchorView(videoView);
        }));

        // Setup hand detector for video mode
        handDetector = new HandDetector(this, RunningMode.VIDEO);
        //handAnalyser = new HandAnalyser(this);

        selectVideo1.setOnClickListener(view -> {
            // On click, open new window for selecting videos
            Intent selectVideo = new Intent(Intent.ACTION_PICK);
            selectVideo.setType("video/*");
            startActivityForResult(selectVideo, CODE_VIDEO);
        });

        loadSettings();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_VIDEO) {
            // Get video Uri and attach it to video view
            Uri videoUri = data.getData();
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(videoUri);
            videoView.start();

            lastWords.setText("Analysing video...");
            analyseVideo(videoUri);
        }
    }

    public void analyseFrame(Uri videoUri) {
        final int INTERVAL = 100;
        List<List<Float>> landmarksList = new ArrayList<>();

        // Setup retriever to get video data
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this, videoUri);

        Log.i(TAG, "Retriever info: " + retriever.getFrameAtTime(1000, MediaMetadataRetriever.OPTION_CLOSEST).getHeight());

        // Set video length and start time
        long videoLength = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        // Get the total frames we need to analyse based on interval (in ms) between them
        int totalFrames = (int) (videoLength / INTERVAL);

        Log.i(TAG, "Video info: " + videoUri.toString());
        Log.i(TAG, "Total video length: " + videoLength);
        Log.i(TAG, "Total video frames to analyse: " + totalFrames);

        // Loop through each frame and add result to results list
        for (int i = 0; i < totalFrames; i++) {
            long timeStamp = i * INTERVAL;
            Bitmap frame = retriever.getFrameAtTime(timeStamp, MediaMetadataRetriever.OPTION_CLOSEST);

            Log.i(TAG, "Analysing at timestamp: " + timeStamp);

            //Convert frame to ARGB_8888 (required by damn mediapipe)
            Bitmap aFrame = frame.copy(Bitmap.Config.ARGB_8888, false);

            Log.i(TAG, "Frame size: " + aFrame.getHeight() + " " + aFrame.getWidth());

            List<Float> landmarks = handDetector.detectFrame(aFrame);
            Log.i(TAG, "Found landmarks: " + landmarks.toString());

            landmarksList.add(landmarks);
        }

        Log.i(TAG, "Total landmarks found: " + landmarksList.toString());
    }

    private void analyseVideo(Uri videoUri) {
        try {
            // TODO: Interval setting
            List<List<Float>> landmarksList = handDetector.detectVideo(videoUri, 1000);
            List<String> translatedLetters = new ArrayList<>();
            String letters = "";

//                for (List<Float> landmarks : landmarksList) {
//                    String letter = handAnalyser.analyseHand(landmarks);
//                    translatedLetters.add(letter);
//                    letters += letter;
//                }

            lastWords.setText(letters);

        } catch (IOException e) {
            throw new RuntimeException(e);
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