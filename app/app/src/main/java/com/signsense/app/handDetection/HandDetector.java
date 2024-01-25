package com.signsense.app.handDetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions;
import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.createFromOptions;

public class HandDetector {
    private static final String TAG = "HandDetector";

    private boolean mode = false;
    private boolean runOnGPU = false;
    private int maxHands = 1;
    private int modelComplexity = 1;
    private float detectionCon = 0.5f;
    private float trackCon = 0.5f;
    private List<Integer> tipIds = new ArrayList<>();
    //private Context context;

    private MPImage image;

    private BaseOptions baseOptions;
    private HandLandmarker handLandmarker;
    private HandLandmarkerResult result;



    public HandDetector(/*boolean mode, int maxHands, int modelComplexity, float detectionCon, float trackCon*/) {
//        this.mode = mode;
//        this.maxHands = maxHands;
//        this.modelComplexity = modelComplexity;
//        this.detectionCon = detectionCon;
//        this.trackCon = trackCon;

        // Fingertip IDs (from nodes)
        for (int i = 4; i <= 20; i += 4) {
            tipIds.add(i);
        }

        // Loading model
        try {
            baseOptions = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build();
        } catch (Exception e) {
            Log.e(TAG, "Error loading Hand Detector Model");
        }

        // Setting up the HandLandmarker
        handLandmarker = createFromOptions(this, HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(maxHands)
                .setMinHandDetectionConfidence(detectionCon)
                .setMinTrackingConfidence(trackCon)
                .build()
        );
    }

    public List<Float> findHands(Mat frame, boolean draw) {
        int timestampMs = 1;

        // Converting OpenCV Mat to Bitmap to Mediapipe MPImage
        Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bitmap);
        image = new BitmapImageBuilder(bitmap).build();

        // Detecting hand
        result = handLandmarker.detectForVideo(image, timestampMs);
        List<Float> landmarks = new ArrayList<>();

        // Adding tip coordinates to list of landmark
        if (result.landmarks().size() > 0) {
            for (List<NormalizedLandmark> landmark : result.landmarks()) {
                for (int tipId : tipIds) { // Getting every tipID's X and Y
                    landmarks.add(landmark.get(tipId).x());
                    landmarks.add(landmark.get(tipId).y());
                }
                if (draw) {
                    Log.i(TAG, "Draw Hand");
                }
            }
        }

        Log.i(TAG, landmarks.toString());
        return landmarks;
    }
}
