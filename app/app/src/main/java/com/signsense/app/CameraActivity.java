package com.signsense.app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.signsense.app.analysis.HandAnalyser;
import com.signsense.app.analysis.HandDetector;
import org.jetbrains.annotations.NotNull;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class CameraActivity extends org.opencv.android.CameraActivity {
    private static final String TAG = "Camera"; // Tag for debug log
    private static final int CODE_REQUEST_CAMERA = 103;

    private JavaCameraView cameraView;
    private TextView translatedLetter, translatedWord, lastWords;

    private Mat greyFrame, rgbFrame;

    private HandDetector handDetector;
    private HandAnalyser handAnalyser;

    private boolean flashlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.cameraView);
        ImageButton toggleFlash = findViewById(R.id.button_toggleFlash);

        translatedLetter = findViewById(R.id.text_camera_translation_letter_value);
        translatedWord = findViewById(R.id.text_camera_translation_word_value);
        lastWords = findViewById(R.id.text_camera_translation_lastwords_value);

        // Setup hand detection for frame (image) mode
        handDetector = new HandDetector(this, RunningMode.IMAGE);
        handAnalyser = new HandAnalyser(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        toggleFlash.setOnClickListener(view -> {
            if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                toggleFlashlight();
            } else {
                Toast.makeText(this, "Flashlight is not available", Toast.LENGTH_SHORT).show();
            }
        });

        askPermissions();
        loadSettings();
        startCamera();
    }

    @Override
    protected List<? extends JavaCameraView> getCameraViewList() {
        return Collections.singletonList(cameraView);
    }

    // Enabling / Disabling camera based on app state
    @Override
    protected void onResume() {
        super.onResume();
        cameraView.enableView();
    }
    @Override
    protected void onPause() {
        super.onPause();
        cameraView.disableView();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
    }

    private void startCamera() {
        Log.i(TAG, "Started camera");
        int lastRecentWordsLen = 0;

        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {

            @Override
            public void onCameraViewStarted(int width, int height) {
                cameraView.setCameraPermissionGranted();
                greyFrame = new Mat();
                rgbFrame = new Mat();
            }

            @Override
            public void onCameraViewStopped() {
                greyFrame.release();
                rgbFrame.release();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) { // On each new frame
                rgbFrame = inputFrame.rgba();
                greyFrame = inputFrame.gray();

                Bitmap bitmap = Bitmap.createBitmap(rgbFrame.cols(), rgbFrame.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rgbFrame, bitmap);

                List<Float> landmarks = handDetector.detectFrame(bitmap);
                String word = handAnalyser.getWord();
                translatedLetter.setText(handAnalyser.analyseHand(landmarks));

                // Run all the UI stuff on the background
                runOnUiThread(() -> {
                    if (word.length() > 0) {
                        translatedWord.setText(word);
                    } else {
                        List<String> recentWords = handAnalyser.getRecentWords();
                        if (recentWords.size() != lastRecentWordsLen) {
                            String lastWordsText = "";
                            for (String w : recentWords) {
                                lastWordsText = w + " " + lastWordsText;
                            }
                            lastWords.setText(lastWordsText);
                            // TODO: Fix the fucking "Only the original thread that created a view hierarchy can touch its views."
                            // Otherwise, it kinda works? good for now
                        }
                    }
                });

                return handDetector.drawHand(rgbFrame, landmarks);
            }
        });

        if (OpenCVLoader.initLocal()) {
            cameraView.enableView();
        }
    }

    private void toggleFlashlight() {
        if (flashlight) {
            cameraView.turnOffTheFlash();
        } else {
            cameraView.turnOnTheFlash();
        }
        flashlight = !flashlight;
        cameraView.enableView();
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
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CODE_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQUEST_CAMERA && grantResults.length > 0) { // Check if the code is for askPermissions and availability to grant is there
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) { // If we haven't already granted this permission ask for it again
                askPermissions();
            } else {
                cameraView.setCameraPermissionGranted();
            }
        }
    }
}