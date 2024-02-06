package com.signsense.app.analysis;

import android.content.Context;
import android.util.Log;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.util.List;

public class HandAnalyser {
    private static final String TAG = "HandAnalyser"; // Tag for debug log
    private final Context appContext;

    private Module module; // The model itself


    public HandAnalyser(Context context) {
        Log.i(TAG, "Initialising Hand Analyser");
        appContext = context.getApplicationContext();
        // Импорт оптимизированной модели из ресурсов приложения
        try {
            module = LiteModuleLoader.loadModuleFromAsset(
                    appContext.getAssets(),
                    "class_model_lite.pt"
            );
        } catch (Exception e) { Log.e(TAG, "Error loading model!"); }
    }
    public int analyseHand(List<Float> landmarks) {
        int signId = 0;
        if (landmarks.size() > 0) {
            // Перевод из List<Float> в float[] для создания тенсора
            float[] data = new float[landmarks.size()];
            int j = 0;
            for (Float f : landmarks) { data[j++] = 1 - f; }
            // Задаем размер для тенсора (одномерный, с длинной координат)
            long[] size = new long[]{1, data.length};
            // Создаем вводный тенсор с данными и размером
            Tensor inputTensor = Tensor.fromBlob(data, size);
            // Запуск модели
            Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
            // Получение результата в баллах
            float[] scores = outputTensor.getDataAsFloatArray();
            // Ищем результат с максимальным баллом (схожесть симбола) - это и есть жест
            float maxScore = -Float.MAX_VALUE;
            int maxScoreIdx = -1;
            for (int i = 0; i < scores.length; i++) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i];
                    maxScoreIdx = i;
                }
            }
            // Получаем айди жеста
            signId = maxScoreIdx + 1;
            Log.d(TAG, "SIGN ID: " + signId);
        }

        return signId;
    }
}
