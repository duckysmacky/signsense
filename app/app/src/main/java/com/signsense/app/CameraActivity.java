package com.signsense.app;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.signsense.app.analysis.HandAnalyser;
import com.signsense.app.analysis.HandDetector;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class CameraActivity extends org.opencv.android.CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "Camera"; // Tag for debug log

    private JavaCamera2View camera;
    private TextView translatedLetter, translatedWord, lastWords;

    private Mat rgbFrame;

    private HandDetector handDetector;
    private HandAnalyser handAnalyser;

    private boolean alphabet = false;

    private int lastRecentWordsLen = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        camera = findViewById(R.id.cameraView);

        CardView cameraTranslationCard = findViewById(R.id.card_cameraTranslation);
        CardView alphabetCard = findViewById(R.id.card_alphabet);

        ImageButton toggleAlphabet = findViewById(R.id.button_toggleAlphabet);

        translatedLetter = findViewById(R.id.text_camera_translation_letter_value);
        translatedWord = findViewById(R.id.text_camera_translation_word_value);
        lastWords = findViewById(R.id.text_camera_translation_lastwords_value);

        // Setup hand detection for frame (image) mode
        handDetector = new HandDetector(this, RunningMode.IMAGE);
        handAnalyser = new HandAnalyser(this);

        // Dactyl alphabet popup button
        toggleAlphabet.setOnClickListener(view -> {
            if (!alphabet) {
                cameraTranslationCard.setVisibility(View.INVISIBLE);
                alphabetCard.setVisibility(View.VISIBLE);
            } else {
                cameraTranslationCard.setVisibility(View.VISIBLE);
                alphabetCard.setVisibility(View.INVISIBLE);
            }
            alphabet = !alphabet;
        });

        loadSettings();

        camera.setCameraPermissionGranted();
        camera.setVisibility(SurfaceView.VISIBLE);
        camera.setCvCameraViewListener(this);
        Log.i(TAG, "Started camera");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Camera resumed");
        if (camera != null) camera.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Camera paused");
        if (camera != null) camera.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Camera destroyed");
        if (camera != null) camera.disableView();
    }

    private void loadSettings() {
        Log.i(TAG, "Loading settings");
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

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(camera);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera view started");
        camera.enableView();
        rgbFrame = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "Camera view stopped");
        camera.disableView();
        rgbFrame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.i(TAG, "Frame received");
        rgbFrame = inputFrame.rgba();

        // Rotate 90 clockwise
        Core.flip(rgbFrame.t(), rgbFrame, 1);

        Bitmap bitmap = Bitmap.createBitmap(rgbFrame.cols(), rgbFrame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbFrame, bitmap);

        List<Float> landmarks = handDetector.detectFrame(bitmap);
        String word = handAnalyser.getWord();
        translatedLetter.setText(handAnalyser.analyseHand(landmarks));

        // Run all the UI stuff on the background
        runOnUiThread(() -> {
            Log.i(TAG, "Updating UI");
            if (!word.isEmpty()) {
                translatedWord.setText(word);
            } else {
                List<String> recentWords = handAnalyser.getRecentWords();
                if (recentWords.size() != lastRecentWordsLen) {
                    StringBuilder lastWordsText = new StringBuilder();
                    for (String w : recentWords) {
                        lastWordsText.insert(0, w + " ");
                    }
                    lastWords.setText(lastWordsText.toString());
                }
            }
        });

        return handDetector.drawHand(rgbFrame, landmarks);
    }
}