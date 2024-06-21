package com.signsense.app.analysis;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;
import androidx.preference.PreferenceManager;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.util.ArrayList;
import java.util.List;

public class SignTranslator implements Runnable {
    private static final String TAG = "HandAnalyser"; // Tag for debug log

    private final boolean SIGN_FLIP;
    private final int SIGN_DELAY;
    private final int COMPARE_LENGTH;
    private final int MODEL_VERSION;
    private final int RECOGNITION_THRESHOLD;

    private final String[] signDict;
    private final List<String> recentWords = new ArrayList<>();

    private Module module; // The model itself

    private String lastLetter = "";
    private String currentWord = "";
    private long signDelay = 0;

    public SignTranslator(Context context) {
        Log.i(TAG, "Initialising Sign Translator");
        Context appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        SIGN_FLIP = preferences.getBoolean("flip", false);
        SIGN_DELAY = preferences.getInt("delay", 1) * 500;
        COMPARE_LENGTH = preferences.getInt("compareLen", 10);
        MODEL_VERSION = preferences.getInt("modelVer", 3);
        // score + 60 = thresh
        RECOGNITION_THRESHOLD = preferences.getInt("recognitionThreshold", 80);

        String modelAssetName = "models/dactyl_v" + MODEL_VERSION + ".pt";
        signDict = SignDictionary.RU_DACTYL[MODEL_VERSION - 1];

        // Loading model from .pt file (MUST be optimised for mobile + lite)
        try {
            module = LiteModuleLoader.loadModuleFromAsset(appContext.getAssets(), modelAssetName);
        } catch (Exception e) {
            Log.e(TAG, "Error loading model");
        }
        Log.i(TAG, "Loaded sign analysis model version " + MODEL_VERSION);
    }

    @Override
    public void run() {

    }

    public String analyseHand(List<Float> landmarks) {
        String recognisedLetter = "";

        if (!landmarks.isEmpty()) {
            signDelay = SystemClock.currentThreadTimeMillis();
            Log.i(TAG, "Analysing hand wth landmarks: \n" + landmarks);

            // Converting list of float (landmarks) to array (input data)
            float[] data = new float[landmarks.size()];
            int i = 0;
            for (Float landmark : landmarks) {
                if (SIGN_FLIP) {
                    // Only flip the X axis (every 2nd)
                    data[i++] = i % 2 == 0 ? 1 - landmark : landmark;
                } else {
                    data[i++] = landmark;
                }
            }

            // Setting the size for tensor (one dimension, with length of data)
            long[] size = new long[]{1, data.length};

            // Creating the input tensor with data and size
            // Take in an array of float of x and y, 1-dimensional
            Tensor inputTensor = Tensor.fromBlob(data, size);

            // Running the model
            Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

            // Getting tensor scores
            float[] scores = outputTensor.getDataAsFloatArray();

            ScoreManager sm = new ScoreManager(signDict, scores);

            float score = sm.getBiggestScore();
            String foundLetter = sm.getLetter();

            boolean valid = score >= RECOGNITION_THRESHOLD;

            Log.d(TAG, "ALL SCORES: " + sm.getScores());
            Log.d(TAG, String.format("FOUND LETTER: %s (%f)", foundLetter, score));
            Log.d(TAG, "LAST LETTER: " + lastLetter);

            if (valid) {
                recognisedLetter = foundLetter;
                if (!foundLetter.equals(lastLetter)) {
                    Log.d(TAG, String.format("NEW RECOGNISED LETTER: %s (%f)", foundLetter, score));
                    lastLetter = foundLetter;
                    currentWord += foundLetter;
                }
            }
        } else if (SystemClock.currentThreadTimeMillis() - signDelay > SIGN_DELAY) {
            signDelay = SystemClock.currentThreadTimeMillis();
            Log.i(TAG, "Sign Delay");
            recentWords.add(currentWord);
            currentWord = "";
        }

        return recognisedLetter;
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public List<String> getRecentWords() {
        return recentWords;
    }
}
