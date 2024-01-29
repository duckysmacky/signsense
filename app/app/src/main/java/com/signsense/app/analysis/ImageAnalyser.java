package com.signsense.app.analysis;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.pytorch.IValue;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.*;

public class ImageAnalyser {
    private static final String TAG = "ImageAnalyser"; // Tag for debug log
    private final Context context;
    private Module module;

    public ImageAnalyser(Context context) {
        Log.i(TAG, "Initialising Image Analyser");
        this.context = context.getApplicationContext();

        // Loading model
        try {
            module = Module.load(assetFilePath(context, "image_model.pt"));
        } catch (Exception e) {
            Log.e(TAG, "Error loading model");
        }
    }

    public String analyse(Bitmap bitmap) {
        // Converting image (bitmap) to tensor
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                MemoryFormat.CHANNELS_LAST
        );

        // Running the model
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

        // Getting tensor scores
        final float[] scores = outputTensor.getDataAsFloatArray();

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
        String resultText = ImageClasses.IMAGENET_CLASSES[maxScoreIdx];

        return resultText;
    }

    // Function for reading asset files because the default way is kinda broken
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}
