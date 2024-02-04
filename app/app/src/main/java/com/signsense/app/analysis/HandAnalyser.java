package com.signsense.app.analysis;

import android.content.Context;
import android.util.Log;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class HandAnalyser {
    private static final String TAG = "HandAnalyser"; // Tag for debug log
    private final Context appContext;

    private Module module; // The model itself


    public HandAnalyser(Context context) {
        Log.i(TAG, "Initialising Hand Analyser");
        appContext = context.getApplicationContext();

        // Loading model from .pt file (must be optimised for mobile + lite)
        try {
            module = LiteModuleLoader.loadModuleFromAsset(
                    appContext.getAssets(),
                    "class_model_lite.pt"
            );
        } catch (Exception e) {
            Log.e(TAG, "Error loading model!");
        }
        Log.i(TAG, "Loaded hand classification model!");
    }

    public int analyseHand(List<Float> landmarks) {
        int signId = 0;
        if (landmarks.size() > 0) {
            Log.i(TAG, "Analysing hand wth landmarks: \n" + landmarks);

            // Converting list of float (landmarks) to array (input data)
            float[] data = new float[landmarks.size()];
            int j = 0;
            for (Float f : landmarks) { data[j++] = 1 - f; }

            // Setting the size for tensor (one dimension, with length of data)
            long[] size = new long[]{1, data.length};

            // Creating the input tensor with data and size
            // Take in an array of float of x and y, 1-dimensional
            Tensor inputTensor = Tensor.fromBlob(data, size);

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
            signId = maxScoreIdx + 1;

            Log.d(TAG, "SIGN CLASS: " + signId);
        }

        return signId;
    }
}
