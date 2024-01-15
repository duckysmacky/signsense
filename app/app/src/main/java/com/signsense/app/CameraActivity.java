package com.signsense.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.os.Bundle;
import androidx.camera.core.*;
import org.jetbrains.annotations.NotNull;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.*;

public class CameraActivity extends org.opencv.android.CameraActivity {

    private ImageButton capturePhoto, toggleFlash, flipCamera;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private CameraBridgeViewBase cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.cameraView);
        capturePhoto = findViewById(R.id.button_capturePhoto);
        toggleFlash = findViewById(R.id.button_toggleFlash);
        flipCamera = findViewById(R.id.button_flipCamera);

        askPermissions();

        flipCamera.setOnClickListener(new View.OnClickListener() { // Check for clicks and rotates camera accordingly
            @Override
            public void onClick(View view) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
            }
        });

        startCamera();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() { // Returns our cameraView View (single one, in case we have many)
        return Collections.singletonList(cameraView);
    }

    // Enabling /  Disabling camera based on app state
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
            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) { // On each new frame
                return inputFrame.rgba();
            }
        });

        if (OpenCVLoader.initDebug()) {
            cameraView.enableView();
        }
    }

    private void setFlashIcon(Camera camera) { // Toggling flash idk at this point im so done
        if (camera.getCameraInfo().hasFlashUnit()) { // Check for flash on camera
            if (camera.getCameraInfo().getTorchState().getValue() == 0) { // If off, turn on, blah blah blah you know the deal
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else { // who tf doesn't have flash on their camera 💀💀
            Toast.makeText(CameraActivity.this, "Flash is not currently available", Toast.LENGTH_SHORT).show(); // nah but really
        }
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) { // Don't ask me what it does 💀
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    // Permission asking
    private void askPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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