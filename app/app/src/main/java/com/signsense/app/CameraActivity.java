package com.signsense.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.signsense.app.analysis.HandAnalyser;
import com.signsense.app.analysis.HandDetector;
import org.jetbrains.annotations.NotNull;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.Collections;
import java.util.List;


public class CameraActivity extends org.opencv.android.CameraActivity {
    private static final String TAG = "Camera"; // Tag for debug log

    private ImageButton toggleFlash, flipCamera;
    private CameraBridgeViewBase cameraView;
    private TextView translationText;

    private Mat greyFrame, rgbFrame;

    private HandDetector handDetector;
    private HandAnalyser handAnalyser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.cameraView);
        toggleFlash = findViewById(R.id.button_toggleFlash);
        flipCamera = findViewById(R.id.button_flipCamera);
        translationText = findViewById(R.id.text_signTranslation);

        handDetector = new HandDetector(getApplicationContext(), 2, 0.5f, 0.5f);
        //TODO: fix hand analyser model
        //handAnalyser = new HandAnalyser(getApplicationContext());

        askPermissions();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

                Mat newFrame = handDetector.findHands(bitmap, true);
                //translationText.setText(handAnalyser.analyseHand(handDetector.getLandmarks()));

                return newFrame;
            }
        });

        if (OpenCVLoader.initDebug()) {
            cameraView.enableView();
        }
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