package com.signsense.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.signsense.app.handDetection.HandDetector;
import org.jetbrains.annotations.NotNull;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;

public class ImageSelectorActivity extends AppCompatActivity {
    Button selectPhoto, openCamera;
    ImageView photoPreview;
    Bitmap imageBitmap;
    Mat imageMat; // OpenCV datatype for image processing
    int SELECTPHOTO_CODE = 100;
    int OPENCAMERA_CODE = 101;

    private HandDetector handDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_selector);

        selectPhoto = findViewById(R.id.button_selectPhoto);
        openCamera = findViewById(R.id.button_openCamera);
        photoPreview = findViewById(R.id.photoPreview);

        handDetector = new HandDetector(getApplicationContext());

        askPermissions();

        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent fileExplorer = new Intent(Intent.ACTION_GET_CONTENT); // Create new intent for selecting files (content)
                fileExplorer.setType("image/*"); // Set type to get images
                startActivityForResult(fileExplorer, SELECTPHOTO_CODE);
            }
        });

        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent imageCapture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); // Create new intent to open phone camera
                startActivityForResult(imageCapture, OPENCAMERA_CODE);
            }
        });
    }

    // Photo Selection and Camera Photo Capture
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Photo selection
        if (requestCode == SELECTPHOTO_CODE && data != null) { // Check for the corresponding code and if user selected image (data)
            // Storing the selected image to bitmap for later usage using MediaStore
            // MediaStore -> Type: Image -> get bitmap to store
            // Indian man said it's recommended to use getContentResolver
            // We also pass our selected image's data as URI (the same way we can store CameraX photos in CameraActivity)
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                photoPreview.setImageBitmap(imageBitmap); // Show image on screen
                Log.i("Success", "Set new photo preview");

                imageMat = new Mat();
                Utils.bitmapToMat(imageBitmap, imageMat); // Convert our image for OpenCV usage (Mat)

//                Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGB2GRAY); // Use OpenCV image processing to convert to grayscale
//                Utils.matToBitmap(imageMat, imageBitmap); // Convert our image back from Mat to Bitmap

                handDetector.findHands(imageBitmap, true);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // Camera
        if (requestCode == OPENCAMERA_CODE && data != null) { // Check for the corresponding code and if user took a picture
            imageBitmap = (Bitmap) data.getExtras().get("data"); // Get the data part of our photo and typecast it to Bitmap
            photoPreview.setImageBitmap(imageBitmap); // Show image on screen
            Log.i("Success", "Set new photo preview");

            imageMat = new Mat();
            Utils.bitmapToMat(imageBitmap, imageMat); // Convert our image for OpenCV usage (Mat)

            handDetector.findHands(imageBitmap, true);
        }
    }

    // Permission asking
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 102 && grantResults.length > 0) { // Check if the code is for askPermissions and availability to grant is there
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) { // If haven't already granted this permission ask for it again
                askPermissions();
            }
        }
    }
    private void askPermissions() {
        if (ContextCompat.checkSelfPermission(ImageSelectorActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 102); // Request with code 102
        }
    }
}