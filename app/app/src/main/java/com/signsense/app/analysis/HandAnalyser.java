package com.signsense.app.analysis;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.List;

public class HandAnalyser implements Runnable {
    private static final String TAG = "HandAnalyser";

    private final HandDetector handDetector;
    private final SignTranslator signTranslator;
    private final Mat frame;
    private final String lastLetter;

    private String recognisedLetter;
    private String currentWord;
    private Status analysisStatus;
    private Mat handFrame;

    public enum Status {
        UNKNOWN,
        FOUND,
        RECOGNISED
    }

    public HandAnalyser(HandDetector handDetector, SignTranslator signTranslator, Mat frame, String lastLetter) {
        this.handDetector = handDetector;
        this.signTranslator = signTranslator;
        this.frame = frame;
        this.lastLetter = lastLetter;
    }

    @Override
    public void run() {
        Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bitmap);

        List<Float> landmarks = handDetector.detectFrame(bitmap);
        recognisedLetter = signTranslator.analyseHand(landmarks);
        currentWord = signTranslator.getCurrentWord();


        if (recognisedLetter.isEmpty()) {
            analysisStatus = Status.UNKNOWN;
        } else if (!recognisedLetter.equals(lastLetter)) {
            analysisStatus = Status.RECOGNISED;
        } else {
            analysisStatus = Status.FOUND;
        }

        handFrame = handDetector.drawHand(frame, landmarks, analysisStatus);
    }

    public String getRecognisedLetter() {
        return recognisedLetter;
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public Status getAnalysisStatus() {
        return analysisStatus;
    }

    public Mat getHandFrame() {
        return handFrame;
    }
}
