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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandAnalyser {
    private static final String TAG = "HandAnalyser"; // Tag for debug log
    final boolean SIGN_FLIP;
    final int SIGN_DELAY;
    final int COMPARE_LENGTH;
    final int MODEL_VERSION;
    private final Context appContext;
    private Module module; // The model itself

    private String[] signDict;
    private final String[] signDict1 = {
            "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "и", "й", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф",
            "х", "ц", "ч", "ш", "щ", "ъ", "ы", "ь", "э", "ю", "я"
    };
    private final String[] signDict2 = {
            "а", "б", "в", "г", "д", "е", "ё", "ж", "з", "[и/й]", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф",
            "х", "ц", "ч", "[ш/щ]", "ъ", "ы", "ь", "э", "ю", "я"
    };
    private final String[] signDict3 = {
            "а", "б", "в", "г", "д", "[е/ё]", "ж", "з", "[и/й]", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф",
            "х", "ц", "ч", "[ш/щ]", "ъ", "ы", "ь", "э", "ю", "я"
    };
    private final String[] signDict4 = {
            "а", "б", "в", "г", "д", "[е/ё]", "ж", "з", "[и/й]", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф",
            "х", "ц", "ч", "[ш/щ]", "ъ", "ы", "ь", "э", "ю", "я"
    };

    private List<String> recentSigns = new ArrayList<>();
    private String word = "";
    private List<String> recentWords = new ArrayList<>();
    private long signDelay = 0;
    private String topSign = "";

    public HandAnalyser(Context context) {
        Log.i(TAG, "Initialising Hand Analyser");
        appContext = context.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(appContext);

        SIGN_FLIP = preferences.getBoolean("flip", false);
        SIGN_DELAY = preferences.getInt("delay", 1) * 500;
        COMPARE_LENGTH = preferences.getInt("compareLen", 10);
        MODEL_VERSION = preferences.getInt("modelVer", 3);

        String modelAssetName = "";

        switch (MODEL_VERSION) {
            case 1:
                modelAssetName = "sign_model_v1.pt";
                signDict = signDict1;
                break;
            case 2:
                modelAssetName = "sign_model_v2.pt";
                signDict = signDict2;
                break;
            case 3:
                modelAssetName = "sign_model_v3.pt";
                signDict = signDict3;
                break;
            case 4:
                modelAssetName = "sign_model_v4.pt";
                signDict = signDict4;
                break;
        }

        // Loading model from .pt file (must be optimised for mobile + lite)
        try {
            module = LiteModuleLoader.loadModuleFromAsset(
                    appContext.getAssets(),
                    modelAssetName
            );
        } catch (Exception e) {
            Log.e(TAG, "Error loading model!");
        }
        Log.i(TAG, "Loaded sign analysis model!");
    }

    public String analyseHand(List<Float> landmarks) {
        String sign = "";
        if (landmarks.size() > 0) {
            signDelay = SystemClock.currentThreadTimeMillis();
            Log.i(TAG, "Analysing hand wth landmarks: \n" + landmarks);

            // Converting list of float (landmarks) to array (input data)
            float[] data = new float[landmarks.size()];
            int j = 0;
            for (Float f : landmarks) {
                if (SIGN_FLIP) {
                    if (j % 2 == 0) {
                        data[j++] = 1 - f;
                    } else {
                        data[j++] = f;
                    }
                } else {
                    data[j++] = f;
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

            // Searching for the index with maximum score
            float maxScore = -Float.MAX_VALUE;
            int maxScoreIdx = -1;
            for (int i = 0; i < scores.length; i++) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i];
                    maxScoreIdx = i;
                }
            }
            sign = signDict[maxScoreIdx];
            Log.d(TAG, "SIGN: " + sign);
            Log.d(TAG, "RECENT SIGNS: " + recentSigns.toString());

            if (recentSigns.size() == COMPARE_LENGTH) {
                topSign = mostCommonSign(recentSigns);
                recentSigns.clear();
                word += topSign;
                Log.d(TAG, "TOP SIGN: " + topSign);
            } else {
                recentSigns.add(sign);
            }

        } else {
            if (SystemClock.currentThreadTimeMillis() - signDelay > SIGN_DELAY) {
                signDelay = SystemClock.currentThreadTimeMillis();
                Log.i(TAG, "Sign Delay");
                recentWords.add(word);
                word = "";
            }
        }

        return topSign;
    }

    public String getWord() {return word;}
    public List<String> getRecentWords() {return recentWords;}

    private String mostCommonSign(List<String> signs) {
        Map<String, Integer> occurrences = new HashMap<String, Integer>();
        final String[] commonSign = new String[1];

        for (String sign : signs) {
            if (occurrences.containsKey(sign)) {
                occurrences.put(sign, occurrences.get(sign) + 1);
            } else {
                occurrences.putIfAbsent(sign, 1);
            }
        }
        int maxOcc = occurrences.values().stream()
                .max(Integer::compare)
                .get();

        occurrences.forEach((key, value) -> {
            if (value == maxOcc) {
                commonSign[0] = key;
            }
        });

        return commonSign[0];
    }
}
