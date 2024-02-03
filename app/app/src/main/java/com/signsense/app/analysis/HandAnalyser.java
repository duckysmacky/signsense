package com.signsense.app.analysis;

import android.content.Context;
import android.util.Log;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.*;
import java.util.List;

public class HandAnalyser {
    private static final String TAG = "HandAnalyser"; // Tag for debug log
    private final Context appContext;

    private Module module; // The model itself


    public HandAnalyser(Context context) {
        Log.i(TAG, "Initialising Hand Analyser");
        appContext = context.getApplicationContext();

        // Loading model
        try {
            module = LiteModuleLoader.load(assetFilePath(appContext, "class_model_lite.pt"));
            Log.i(TAG, "Loaded model");
        } catch (Exception e) {
            Log.e(TAG, "Error loading model");
        }
    }

    public String analyseHand(List<Float> landmarks) {
        String resultText = "";
        if (landmarks.size() > 0) {
            Log.i(TAG, "Analysing hand wth landmarks: \n" + landmarks);

            // Converting List of float to primitive list of double
            double[] landmarkList = landmarks.stream().mapToDouble(i -> i).toArray();

            // Setting the input for model
            Tensor inputTensor = Tensor.fromBlob(landmarkList, new long[]{1, landmarks.size()});

            // Running the model
            Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

            // Getting tensor scores
            float[] scores = outputTensor.getDataAsFloatArray();

            // Searching for the index with maximum score
            float maxScore = -Float.MAX_VALUE;
            int maxScoreIdx = -1;
            for (int i = 0; i < scores.length; i++) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i];
                    maxScoreIdx = i;
                }
            }

            // Gets the name of the detected object by the highest score
            resultText = ImageClasses.IMAGENET_CLASSES[maxScoreIdx];

            Log.i(TAG, "SIGN CLASS: " + resultText);
        }

        return resultText;
    }

    // Function for reading asset files because the default way is kinda broken
    public static String assetFilePath(Context context, String assetName) throws IOException {
        Log.i(TAG, "Accessing model file");
        File file = new File(context.getFilesDir(), assetName);

//        if (file.exists() && file.length() > 0) {
//            return file.getAbsolutePath();
//        }
        return file.getPath();

//        try (InputStream is = context.getAssets().open(assetName)) {
//            try (OutputStream os = Files.newOutputStream(file.toPath())) {
//                byte[] buffer = new byte[4 * 1024];
//                int read;
//
//                while ((read = is.read(buffer)) != -1) {
//                    os.write(buffer, 0, read);
//                }
//
//                os.flush();
//            }
//
//            return file.getAbsolutePath();
//        }
    }
}
