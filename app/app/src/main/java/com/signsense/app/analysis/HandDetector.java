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

    private HandLandmarker handLandmarker;
    private Context appContext;
    private boolean draw;

    public HandDetector(Context context, RunningMode mode) {
        this.appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        // Assign Ids for landmarks from 0 to 20
        for (int i = 0; i < landmarkIds.length; i++) { landmarkIds[i] = i; }

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
        if (result.landmarks().size() > 0) {
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

    // Function for detecting hand when the video is uploaded (breaks it down into multiple frames)
    public List<List<Float>> detectVideo(Uri videoUri, long interval) throws IOException {
        List<List<Float>> landmarksList = new ArrayList<>();

        // Setup retriever to get video data
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(appContext, videoUri);

        // Set video length and start time
        long videoLength = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        // Get the total frames we need to analyse based on interval (in ms) between them
        int totalFrames = (int) (videoLength / interval);

        Log.i(TAG, "Total video length: " + videoLength);
        Log.i(TAG, "Total video frames to analyse: " + totalFrames);

        // Loop through each frame and add result to results list
        for (int i = 0; i < totalFrames; i++) {
            List<Float> landmarks = new ArrayList<>();
            long timeStamp = i * interval;
            Bitmap frame = retriever.getFrameAtTime(timeStamp, MediaMetadataRetriever.OPTION_CLOSEST);

            Log.i(TAG, "Analysing at timestamp: " + timeStamp);

            //Convert frame to ARGB_8888 (required by damn mediapipe)
            Bitmap aFrame = frame.copy(Bitmap.Config.ARGB_8888, false);

            // Convert ARGB_8888 frame to MPImage
            MPImage image = new BitmapImageBuilder(aFrame).build();

            Log.i(TAG, "MPImage size: " + image.getHeight() + " | " + image.getWidth());

            HandLandmarkerResult result = handLandmarker.detectForVideo(image, timeStamp);

            Log.i(TAG, "Result: " + result.toString());
            Log.i(TAG, "Found landmarks: " + result.landmarks().toString());

            // Adding tip coordinates to list of landmark
            if (result.landmarks().size() > 0) {
                for (List<NormalizedLandmark> landmark : result.landmarks()) {
                    for (int id : landmarkIds) { // Getting X and Y for every tip
                        float x = landmark.get(id).x();
                        float y = landmark.get(id).y();
                        landmarks.add(x);
                        landmarks.add(y);

                        Log.i(TAG, "Landmark " + id + ":" + x + " | " + y);
                    }
                }

                Log.i(TAG, landmarks.toString());
            }

            // Add to the list of results
            landmarksList.add(landmarks);
        }

        retriever.release();

        Log.i(TAG, "Total landmarks found: " + landmarksList.toString());

        return landmarksList;
    }

    public Mat drawHand(Mat frame, List<Float> landmarks) {
        if (!draw) {
            return frame;
        }

        for (int i = 0; i < landmarks.size() - 1; i += 2) {
            float x = landmarks.get(i);
            float y = landmarks.get(i + 1);

            // Drawing circles at the fingertips by finding coordinates via multiplication
            // E.g. we have 200 pixels wide screen and the fingertip X is 0.54, so we draw x at 200 * 0.54 = 108
            Imgproc.circle(
                    frame,
                    new Point(frame.width() * x, frame.height() * y),
                    5,
                    new Scalar(0, 255, 0, 255),
                    10
            );
        }

        return frame;
    }
}