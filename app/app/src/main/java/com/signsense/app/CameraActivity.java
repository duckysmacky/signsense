package com.signsense.app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

    private Mat rgbFrame;

    private HandDetector handDetector;
    private HandAnalyser handAnalyser;

    private boolean flashlight = false;
    private boolean alphabet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.cameraView);

        CardView cameraTranslationCard = findViewById(R.id.card_cameraTranslation);
        CardView alphabetCard = findViewById(R.id.card_alphabet);

        ImageButton toggleAlphabet = findViewById(R.id.button_toggleAlphabet);
        ImageButton toggleFlash = findViewById(R.id.button_toggleFlash);

        translatedLetter = findViewById(R.id.text_camera_translation_letter_value);
        translatedWord = findViewById(R.id.text_camera_translation_word_value);
        lastWords = findViewById(R.id.text_camera_translation_lastwords_value);

        // Setup hand detection for frame (image) mode
        handDetector = new HandDetector(this, RunningMode.IMAGE);
        handAnalyser = new HandAnalyser(this);

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        toggleFlash.setOnClickListener(view -> {
            if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                toggleFlashlight();
            } else {
                Toast.makeText(this, "Flashlight is not available", Toast.LENGTH_SHORT).show();
            }
        });

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

        askPermissions();
        loadSettings();
    }

    @Override
    protected List<? extends JavaCameraView> getCameraViewList() {
        Log.i(TAG, "Called getCameraViewList");
        return Collections.singletonList(cameraView);
    }

    // Enabling / Disabling camera based on app state
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Camera resumed");
        cameraView.enableView();
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Camera paused");
        cameraView.disableView();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Camera destroyed");
        cameraView.disableView();
    }

    private void startCamera() {
        Log.i(TAG, "Started camera");
        int lastRecentWordsLen = 0;
        cameraView.enableView();

        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {

            @Override
            public void onCameraViewStarted(int width, int height) {
                Log.i(TAG, "Camera view started");
                cameraView.enableView();
                rgbFrame = new Mat();
            }

            @Override
            public void onCameraViewStopped() {
                Log.i(TAG, "Camera view stopped");
                cameraView.disableView();
                rgbFrame.release();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) { // On each new frame
                Log.i(TAG, "Frame received");
                rgbFrame = inputFrame.rgba();

                Bitmap bitmap = Bitmap.createBitmap(rgbFrame.cols(), rgbFrame.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rgbFrame, bitmap);

                List<Float> landmarks = handDetector.detectFrame(bitmap);
                String word = handAnalyser.getWord();
                translatedLetter.setText(handAnalyser.analyseHand(landmarks));

                // Run all the UI stuff on the background
                runOnUiThread(() -> {
                    Log.i(TAG, "Updating UI");
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
                        }
                    }
                });

                return handDetector.drawHand(rgbFrame, landmarks);
            }
        });
    }

    private void toggleFlashlight() {
        if (flashlight) {
            cameraView.turnOffTheFlash();
        } else {
            cameraView.turnOnTheFlash();
        }
        flashlight = !flashlight;
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

    // Permission asking
    private void askPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions granted");
            cameraView.setCameraPermissionGranted();
            startCamera();
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
                cameraView.setCameraPermissionGranted();
                startCamera();
            } else {
                // Camera will stay off
                Log.i(TAG, "Camera permission denied");
                askPermissions();
            }
        }
    }
}