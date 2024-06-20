package com.signsense.app.analysis;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import androidx.preference.PreferenceManager;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions;
import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.createFromOptions;

public class HandDetector {
    private static final String TAG = "HandDetector";

    private final int[] landmarkIds = new int[21]; // IDs for finger landmarks

    private final HandLandmarker handLandmarker;
    private final Context appContext;
    private final boolean draw;

    public HandDetector(Context context, RunningMode mode) {
        this.appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        // Assign Ids for landmarks from 0 to 20
        for (int i = 0; i < landmarkIds.length; i++) landmarkIds[i] = i;

        // Loading from settings
        this.draw = preferences.getBoolean("draw", false);
        final int MAX_HANDS = preferences.getInt("maxHands", 2);
        final float DETECTION_CON = (float) preferences.getInt("detectionCon", 50) / 100;
        final float TRACKING_CON = (float) preferences.getInt("trackingCon", 50) / 100;
        final float PRESENCE_CON = (float) preferences.getInt("presenceCon", 50) / 100;

        // Loading hand detection model
        BaseOptions baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .setDelegate(Delegate.GPU) // ALL I HAD TO DO IS TO SET IT TO FUCKING GPU MODE AND NOW IT WORKS
                .build();

        // Logs information about the hand detector config
        Log.i(TAG, "Successfully loaded Hand Detector Model");
        Log.i(TAG, "Hand Detector Configuration:");
        Log.i(TAG, "Draw: " + draw);
        Log.i(TAG, "Max Hands: " + MAX_HANDS);
        Log.i(TAG, "Detection Confidence: " + DETECTION_CON);
        Log.i(TAG, "Tracking Confidence: " + TRACKING_CON);
        Log.i(TAG, "Presence Confidence: " + PRESENCE_CON);

        // Setting up the Hand Landmarker
        handLandmarker = createFromOptions(appContext, HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(mode)
                .setNumHands(MAX_HANDS)
                .setMinHandDetectionConfidence(DETECTION_CON)
                .setMinTrackingConfidence(TRACKING_CON)
                .setMinHandPresenceConfidence(PRESENCE_CON)
                .build()
        );
    }

    // Function for detecting hand when using live camera (frame by frame)
    public List<Float> detectFrame(Bitmap bitmap) {
        List<Float> landmarks = new ArrayList<>();

        // Convert bitmap (frame) to MPImage
        MPImage image = new BitmapImageBuilder(bitmap).build();

        // Detecting hand
        HandLandmarkerResult result = handLandmarker.detect(image);

        // Adding tip x and y coordinates to list of landmarks
        if (!result.landmarks().isEmpty()) {
            for (List<NormalizedLandmark> landmark : result.landmarks()) {
                for (int id : landmarkIds) { // Getting x and y for every tip
                    float x = landmark.get(id).x();
                    float y = landmark.get(id).y();
                    landmarks.add(x);
                    landmarks.add(y);
                }
            }
            Log.i(TAG, landmarks.toString());
        }

        return landmarks;
    }

    public Mat drawHand(Mat frame, List<Float> landmarks, boolean found) {
        if (!draw) return frame;

        // If found Green, else Red
        Scalar color = found ? new Scalar(0, 255, 0, 255) : new Scalar(255, 0, 0, 255);

        for (int i = 0; i < landmarks.size() - 1; i += 2) {
            float x = landmarks.get(i);
            float y = landmarks.get(i + 1);

            // Drawing circles at the fingertips by finding coordinates via multiplication
            // E.g. we have 200 pixels wide screen and the fingertip X is 0.54, so we draw x at 200 * 0.54 = 108
            Imgproc.circle(
                    frame,
                    new Point(frame.width() * x, frame.height() * y),
                    5,
                    color,
                    10
            );
        }

        return frame;
    }
}
