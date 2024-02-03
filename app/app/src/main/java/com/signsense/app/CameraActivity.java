package com.signsense.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.signsense.app.analysis.HandAnalyser;
import com.signsense.app.analysis.HandDetector;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;


public class CameraActivity extends org.opencv.android.CameraActivity {
    private static final String TAG = "Camera"; // Tag for debug log

    private ImageButton toggleFlash, flipCamera;
    private JavaCameraView cameraView;
    private TextView translationText;

    private Mat greyFrame, rgbFrame;

    private HandDetector handDetector;
    private HandAnalyser handAnalyser;

    private Camera mCamera;
    private Camera.Parameters parameters;
    private CameraManager camManager;
    private boolean flashlight = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.cameraView);
        toggleFlash = findViewById(R.id.button_toggleFlash);
        flipCamera = findViewById(R.id.button_flipCamera);
        translationText = findViewById(R.id.text_cameraTranslation);

        // Setup hand detection for frame (image) mode
        handDetector = new HandDetector(this, RunningMode.IMAGE);

        //TODO: fix hand analyser model
        //handAnalyser = new HandAnalyser(getApplicationContext());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        toggleFlash.setOnClickListener(view -> {
            if (getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                toggleFlashlight();
            } else {
                Toast.makeText(this, "Flashlight is not available", Toast.LENGTH_SHORT).show();
            }
        });

        startCamera();
    }

    @Override
    protected List<CameraBridgeViewBase> getCameraViewList() { // Returns our cameraView View (single one, in case we have many)
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
        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {

            @Override
            public void onCameraViewStarted(int width, int height) {
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
                //handAnalyser.analyseHand(landmarks);

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
    }
}