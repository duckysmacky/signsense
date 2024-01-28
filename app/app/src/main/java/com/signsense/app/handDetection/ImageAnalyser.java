package com.signsense.app.handDetection;

import android.graphics.Bitmap;
import android.util.Log;
import org.pytorch.IValue;
import org.pytorch.MemoryFormat;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class ImageAnalyser {
    private static final String TAG = "ImageAnalyser"; // Tag for debug log
    private Module module;

    public ImageAnalyser() {
        // Loading model
        try {
            module = Module.load("file:///android_asset/image_model.pt");
        } catch (Exception e) {
            Log.e(TAG, "Error reading assets", e);
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

        String resultText = ImageClasses.IMAGENET_CLASSES[maxScoreIdx];

        return resultText;
    }
}
