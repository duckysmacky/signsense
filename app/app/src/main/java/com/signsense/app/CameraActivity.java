package com.signsense.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import com.signsense.app.OpenCV.CameraBridgeViewBase2;
import com.signsense.app.imageProcessing.ColorBlobDetector;
import org.jetbrains.annotations.NotNull;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
//import org.opencv.objdetect.FaceDetectorYN;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CameraActivity extends org.opencv.android.CameraActivity {
    private static final String TAG = "Hand-Detection";
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;
    private Scalar CONTOUR_COLOR = new Scalar(255,0,0,255);
    private Scalar CONTOUR_COLOR_WHITE = new Scalar(255,255,255,255);
    private final int mDetectorType = JAVA_DETECTOR;

    private ColorBlobDetector mDetector;

    private ImageButton capturePhoto, toggleFlash, flipCamera;

    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private CameraBridgeViewBase cameraView;

    private MatOfByte modelBuffer;
    private MatOfByte configBuffer;
    //private FaceDetectorYN faceDetector;
    private Size imageInputSize = null;
    private CascadeClassifier cascadeClassifier;

    private Mat greyFrame, rgbFrame, bgrFrame, scaledFrame;
    private MatOfRect rects;

    private float scaleOffset = 2f; // Offset for scaling, for some reason we need to div / mult a lot of things by it so it renders correctly

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.cameraView);
        capturePhoto = findViewById(R.id.button_capturePhoto);
        toggleFlash = findViewById(R.id.button_toggleFlash);
        flipCamera = findViewById(R.id.button_flipCamera);

        askPermissions();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        flipCamera.setOnClickListener(new View.OnClickListener() { // Check for clicks and rotates camera accordingly
            @Override
            public void onClick(View view) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
            }
        });

        startCamera();
    }

    @Override
    protected List<CameraBridgeViewBase> getCameraViewList() { // Returns our cameraView View (single one, in case we have many)
        return Collections.singletonList(cameraView);
    }
    // Enabling /  Disabling camera based on app state
    @Override
    protected void onResume() {
        super.onResume();
        cameraView.enableView();
    }
    @Override
    protected void onPause() {
        super.onPause();
        cameraView.disableView();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
    }

    private void startCamera() {
        cameraView.setCvCameraViewListener(new CameraBridgeViewBase2.CvCameraViewListener2() {
            private int numberOfFingers = 0;

            @Override
            public void onCameraViewStarted(int width, int height) {
                greyFrame = new Mat();
                rgbFrame = new Mat();
                bgrFrame = new Mat();
                scaledFrame = new Mat();
                rects = new MatOfRect();
                mDetector = new ColorBlobDetector();
            }

            @Override
            public void onCameraViewStopped() {
                greyFrame.release();
                rgbFrame.release();
                bgrFrame.release();
                scaledFrame.release();
                rects.release();
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                return null;
            }

            public void processFace(Mat rgba, Mat faces) {
                float[] faceData = new float[faces.cols() * faces.channels()]; // Converts the face frame into list of data of each pixel

                for (int i = 0; i < faces.rows(); i++) {
                    faces.get(i, 0, faceData);

                    // Draw bounding box
                    Imgproc.rectangle(rgba, new Rect(
                            Math.round(scaleOffset * faceData[0]),
                            Math.round(scaleOffset * faceData[1]),
                            Math.round(scaleOffset * faceData[2]),
                            Math.round(scaleOffset * faceData[3])
                            ), new Scalar(255, 255, 255), 10);

                    // Blur Face
//                    Imgproc.blur(faces, faces, new Size(10, 10));
                }
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase2.CvCameraViewFrame inputFrame) { // On each new frame
                rgbFrame = inputFrame.rgba();
                greyFrame = inputFrame.gray();

                //Imgproc.blur(rgbFrame, rgbFrame, new Size(5,5));
                Imgproc.GaussianBlur(rgbFrame, rgbFrame, new org.opencv.core.Size(3, 3), 1, 1);
                //Imgproc.medianBlur(rgbFrame, rgbFrame, 3);

                mDetector.process(rgbFrame);
                List<MatOfPoint> contours = mDetector.getContours();

                if (contours.size() <= 0) return rgbFrame;
                Log.e(TAG, "No contours!");

                RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0).toArray()));

                double boundWidth = rect.size.width;
                double boundHeight = rect.size.height;
                int boundPos = 0;

                for (int i = 1; i < contours.size(); i++) {
                    rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(i).toArray()));
                    if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
                        boundWidth = rect.size.width;
                        boundHeight = rect.size.height;
                        boundPos = i;
                    }
                }

                Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours.get(boundPos).toArray()));

                Imgproc.rectangle(rgbFrame, boundRect.tl(), boundRect.br(), CONTOUR_COLOR_WHITE, 2, 8, 0 );

                int rectHeightThresh = 0;
                double a = boundRect.br().y - boundRect.tl().y;
                a *= 0.7;
                a += boundRect.tl().y;

                //Core.rectangle( rgbFrame, boundRect.tl(), boundRect.br(), CONTOUR_COLOR, 2, 8, 0 );
                Imgproc.rectangle(rgbFrame, boundRect.tl(), new Point(boundRect.br().x, a), CONTOUR_COLOR, 2, 8, 0 );

                MatOfPoint2f pointMat = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(boundPos).toArray()), pointMat, 3, true);
                contours.set(boundPos, new MatOfPoint(pointMat.toArray()));

                MatOfInt hull = new MatOfInt();
                MatOfInt4 convexDefect = new MatOfInt4();
                Imgproc.convexHull(new MatOfPoint(contours.get(boundPos).toArray()), hull);

                if(hull.toArray().length < 3) return rgbFrame;

                Imgproc.convexityDefects(new MatOfPoint(contours.get(boundPos)	.toArray()), hull, convexDefect);

                List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
                List<Point> listPo = new LinkedList<Point>();
                for (int j = 0; j < hull.toList().size(); j++) {
                    listPo.add(contours.get(boundPos).toList().get(hull.toList().get(j)));
                }

                MatOfPoint e = new MatOfPoint();
                e.fromList(listPo);
                hullPoints.add(e);

                List<MatOfPoint> defectPoints = new LinkedList<MatOfPoint>();
                List<Point> listPoDefect = new LinkedList<Point>();
                for (int j = 0; j < convexDefect.toList().size(); j = j+4) {
                    Point farPoint = contours.get(boundPos).toList().get(convexDefect.toList().get(j+2));
                    Integer depth = convexDefect.toList().get(j+3);
                    if(farPoint.y < a){
                        listPoDefect.add(contours.get(boundPos).toList().get(convexDefect.toList().get(j+2)));
                    }
                    Log.d(TAG, "defects ["+j+"] " + convexDefect.toList().get(j+3));
                }

                MatOfPoint e2 = new MatOfPoint();
                e2.fromList(listPo);
                defectPoints.add(e2);

                Log.d(TAG, "hull: " + hull.toList());
                Log.d(TAG, "defects: " + convexDefect.toList());

                Imgproc.drawContours(rgbFrame, hullPoints, -1, CONTOUR_COLOR, 3);

                int defectsTotal = (int) convexDefect.total();
                Log.d(TAG, "Defect total " + defectsTotal);

                this.numberOfFingers = listPoDefect.size();
                if (this.numberOfFingers > 5) {
                    this.numberOfFingers = 5;
                }

                for(Point p : listPoDefect){
                    Imgproc.circle(rgbFrame, p, 6, new Scalar(255,0,255));
                }

                return rgbFrame;

//                cascadeClassifier.detectMultiScale(greyFrame, rects, 1.1, 2);

//                for (Rect rect : rects.toList()) { // For each rectangle in our rects
//                    Mat face = rgbFrame.submat(rect);
//                    // Gets (crops) the face part of the frame (guess what is inside the rectangle? thats right, our face)
//
//                    Imgproc.blur(face, face, new Size(10, 10));
//                    // Blurs the face with power of 10 !!!
//
//                    Imgproc.rectangle(rgbFrame, rect, new Scalar(0, 255, 0), 10);
//                    // Draws a rectangle over a detected object to final rbFrame
//
//                    face.release();
//                }
            }
        });

        if (OpenCVLoader.initDebug()) {
            cameraView.enableView();

            try {
                InputStream is = getResources().openRawResource(R.raw.face_detection_yunet_2023mar); // Get the model

                // 1. We create a byte array (size of how many bytes we can read from file) as our buffer for read data from the model file
                // 2. We create bytesRead which will store how many bytes were actually read when we call the read() method on our InputStream object
                // 3. We use the read() method on our InputStream object to read data from the model file into our byte array

                int size = is.available();
                byte[] buffer = new byte[size];
                int bytesRead = is.read(buffer);
                is.close();

                modelBuffer = new MatOfByte(buffer); // Pass our bytes to ModelBuffer
                configBuffer = new MatOfByte();

                //faceDetector = FaceDetectorYN.create("onnx", modelBuffer, configBuffer, new Size(320, 320));
                // Initiating a FaceDetector based on ONNX model

//                if (faceDetector == null) {
//                    Log.e("OpenCV", "Failed to create FaceDetectorYN!");
//                    (Toast.makeText(this, "Failed to create FaceDetectorYN!", Toast.LENGTH_LONG)).show();
//                } else {
//                    Log.i("OpenCV", "FaceDetectorYN initialized successfully!");
//                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("OpenCV", "Failed to ONNX model from resources! Exception thrown: " + e);
                (Toast.makeText(this, "Failed to ONNX model from resources!", Toast.LENGTH_LONG)).show();
                return;
            }
        }
    }

    // Flash settings (depricated)
    private void setFlashIcon(Camera camera) { // Toggling flash idk at this point im so done
        if (camera.getCameraInfo().hasFlashUnit()) { // Check for flash on camera
            if (camera.getCameraInfo().getTorchState().getValue() == 0) { // If off, turn on, blah blah blah you know the deal
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else { // who tf doesn't have flash on their camera 💀💀
            Toast.makeText(CameraActivity.this, "Flash is not currently available", Toast.LENGTH_SHORT).show(); // nah but really
        }
    }

    // Permission asking
    private void askPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 103);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 103 && grantResults.length > 0) { // Check if the code is for askPermissions and availability to grant is there
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) { // If we haven't already granted this permission ask for it again
                askPermissions();
            }
        }
    }
}