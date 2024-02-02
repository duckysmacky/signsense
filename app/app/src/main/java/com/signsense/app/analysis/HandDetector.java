package com.signsense.app.analysis;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.core.Delegate;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions;
import static com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.createFromOptions;

public class HandDetector {
    private static final String TAG = "HandDetector";

    private final int[] tipIds = new int[]{4, 8, 12, 16, 20}; // IDs for fingertips

    private MPImage image;

    private BaseOptions baseOptions;
    private HandLandmarker handLandmarker;
    private Context appContext;

    private boolean draw;

    public HandDetector(Context context, RunningMode mode, boolean draw, int maxHands, float detectionCon, float trackCon, float presenceCon) {
        appContext = context.getApplicationContext();
        this.draw = draw;

        // Loading model
        baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .setDelegate(Delegate.GPU) // ALL I HAD TO DO IS TO SET IT TO FUCKING GPU MODE AND NOW IT WORKS ASGAOGYHOAHGOA
                .build();
        Log.i(TAG, "Successfully loaded Hand Detector Model " + baseOptions.toString());

        // Setting up the Hand Landmarker
        handLandmarker = createFromOptions(appContext, HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(mode)
                .setNumHands(maxHands)
                .setMinHandDetectionConfidence(detectionCon)
                .setMinTrackingConfidence(trackCon)
                .setMinHandPresenceConfidence(presenceCon)
                .build()
        );
    }

    public List<Float> detectFrame(Bitmap bitmap) {
        List<Float> landmarks = new ArrayList<>();

        image = new BitmapImageBuilder(bitmap).build();
        Mat frame = new Mat();
        Utils.bitmapToMat(bitmap, frame);

        // Detecting hand
        HandLandmarkerResult result = handLandmarker.detect(image);

        // Adding tip coordinates to list of landmark
        if (result.landmarks().size() > 0) {
            for (List<NormalizedLandmark> landmark : result.landmarks()) {
                for (int tipId : tipIds) { // Getting X and Y for every tip
                    float x = landmark.get(tipId).x();
                    float y = landmark.get(tipId).y();
                    landmarks.add(x);
                    landmarks.add(y);
                    if (draw) {
                        // Drawing circles at the fingertips by finding coordinates via multiplication
                        // E.g. we have 200 pixels wide screen and the fingertip X is 0.54, so we draw x at 200 * 0.54 = 108
                        Imgproc.circle(
                                frame,
                                new Point(frame.width() * x, frame.height() * y),
                                5,
                                new Scalar(255, 0, 0, 255),
                                10
                        );
                    }
                }
            }
        }
        Log.i(TAG, landmarks.toString());

        return landmarks;
    }

    public List<List<Float>> detectVideo(Uri videoUri, long interval) throws IOException {
        List<List<Float>> landmarksList = new ArrayList<>();
        List<Float> landmarks = new ArrayList<>();

        // Setup retriever to get video data
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(appContext, videoUri);

        // Set video length and start time
        long videoLength = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        long startTime = SystemClock.uptimeMillis();

        // Get the size of the video from the first frame
        Bitmap frame = retriever.getFrameAtTime(0);
        int width = frame.getWidth();
        int height = frame.getHeight();

        // Get the total frames we need to analyse based on interval (in ms) between them
        List<List<Float>> results = new ArrayList<>();
        int totalFrames = (int) (videoLength / interval);

        // Loop through each frame and add result to results list
        for (int i = 0; i < totalFrames; i++) {
            long timeStamp = i * interval;
            frame = retriever.getFrameAtTime(timeStamp, MediaMetadataRetriever.OPTION_CLOSEST);

            //Convert frame to ARGB_8888 (required by damn mediapipe)
            Bitmap aFrame = frame.copy(Bitmap.Config.ARGB_8888, false);

            // Convert ARGB_8888 frame to MPImage
            MPImage image = new BitmapImageBuilder(aFrame).build();

            HandLandmarkerResult result = handLandmarker.detectForVideo(image, timeStamp);

            // Adding tip coordinates to list of landmark
            if (result.landmarks().size() > 0) {
                for (List<NormalizedLandmark> landmark : result.landmarks()) {
                    for (int tipId : tipIds) { // Getting X and Y for every tip
                        float x = landmark.get(tipId).x();
                        float y = landmark.get(tipId).y();
                        landmarks.add(x);
                        landmarks.add(y);
                    }
                }
            }

            Log.i(TAG, landmarks.toString());

            // Add to the list of results
            landmarksList.add(landmarks);
        }

        retriever.release();

        Log.i(TAG, landmarksList.toString());

        return landmarksList;
    }
}
