package com.signsense.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    ImageButton capturePhoto, toggleFlash, flipCamera;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private PreviewView previewView;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult( // Makes a variable so we can ask permissions with it when we need it
            new ActivityResultContracts.RequestPermission(), // Requests permission to grant permissions 💀
            new ActivityResultCallback<Boolean>() { // Check for callback -> if got positive reply, start the camera
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        startCamera(cameraFacing);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Assign views
        previewView = findViewById(R.id.cameraPreview);
        capturePhoto = findViewById(R.id.button_capturePhoto);
        toggleFlash = findViewById(R.id.button_toggleFlash);
        flipCamera = findViewById(R.id.button_flipCamera);

        // Check for CAMERA permission in this Activity -> if not, ask for permission, else start camera
        if (ContextCompat.checkSelfPermission(com.signsense.app.CameraActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(android.Manifest.permission.CAMERA); // Ask for permission
        } else {
            startCamera(cameraFacing);
        }

        flipCamera.setOnClickListener(new View.OnClickListener() { // Check for clicks and rotates camera accordingly
            @Override
            public void onClick(View view) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) { // Flip the camera visa versa
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing);
            }
        });
    }

    public void startCamera(int cameraFacing) { // Func to launch camera
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight()); // Set aspect ratio
        ListenableFuture listenableFuture = ProcessCameraProvider.getInstance(this); // Init a Future to add listeners to and chain them
        // This Future is Listenable (for listeners) and initialized by the ProcessCameraProvider, with THIS Activity as a base for the camera
        // CameraProvider is basically a skeleton to build upon to set up our camera

        listenableFuture.addListener(() -> { // Adding a listener into an empty lambda
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();
                // Getting the Camera Provider back from Future (idk why not 💀)

                Preview preview = new Preview.Builder() // Init a new class to show camera picture (aka what user sees)
                        .setTargetAspectRatio(aspectRatio) // Setting aspect rotation once again
                        .build(); // Finishing the build by building it (duh)

                ImageCapture imageCapture = new ImageCapture.Builder() // Init a new class for taking pictures and saving them
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Capture mode to the lowest latency
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()) // Make the final picture the same rotation as device
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder() // Init a new class to select our camera as input (I guess? 😴)
                        .requireLensFacing(cameraFacing) // Making sure the camera faces the right way
                        .build();

                cameraProvider.unbindAll(); // Closes every currently opened camera, so we can assign new value to it

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture); // Finally comes the Camera 🎉🎉
                // Init a new Camera using CameraProvider we set up earlier: binding it to the current Activity and adding all the previously created camera parts

                capturePhoto.setOnClickListener(new View.OnClickListener() { // Click listener to take pics
                    @Override
                    public void onClick(View view) {
                        // Requesting permission -> if doesn't have it - ask for it, if does have it - take a picture
                        if (ContextCompat.checkSelfPermission(com.signsense.app.CameraActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        } else {
                            takePicture(imageCapture); // Calls a func to take pic with set up ImageCapture
                        }
                    }
                });

                toggleFlash.setOnClickListener(new View.OnClickListener() { // Click listener for flash
                    @Override
                    public void onClick(View view) {
                        setFlashIcon(camera); // Calls a func to change flash for current Camera
                    }
                });

                preview.setSurfaceProvider(previewView.getSurfaceProvider()); // Set surface (place where the preview will be shown)
            } catch (ExecutionException | InterruptedException e) { // Oh yeah we did everything in try-catch because dumb phone go boom
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this)); // Set Executor (which will run the thread to handle all of the bullshit above) to this Activity
    }

    public void takePicture(ImageCapture imageCapture) { // Taking pictures 🍆✊💦👨🤳
        final File file = new File(getExternalFilesDir(null), System.currentTimeMillis() + ".jpg");
        // Init a new file which will save to device's external files directory with name of current time (and .jpg extention)
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        // Configure ImageCapture's File Output to be the file we declated above (with our favourite builder ofc)
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            // This is some intergalactic shit that I don't get, but we run takePicute func on our Image Capture with Output Options,
            // Executor (which runs some kind of CachedThreadPool🤓) and init a new Callback when we save the image:
            @Override
            public void onImageSaved(@NonNull @NotNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(com.signsense.app.CameraActivity.this, "Image saved at: " + file.getPath(), Toast.LENGTH_SHORT).show();
                        // yapee
                    }
                });
                startCamera(cameraFacing); // Start camera again
            }

            @Override
            public void onError(@NonNull @NotNull ImageCaptureException exception) { // If any error occurs
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(com.signsense.app.CameraActivity.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        // haha looser your pic didnt save
                    }
                });
                startCamera(cameraFacing); // Start camera again
            }
        });
    }

    private void setFlashIcon(Camera camera) { // Toggling flash idk at this point im so done
        if (camera.getCameraInfo().hasFlashUnit()) { // Check for flash on camera
            if (camera.getCameraInfo().getTorchState().getValue() == 0) { // If off, turn on, blah blah blah you know the deal
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else { // who tf doesnt have flash on their camera 💀💀
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(com.signsense.app.CameraActivity.this, "Flash is not currently available", Toast.LENGTH_SHORT).show(); // nah but really
                }
            });
        }
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) { // Don't ask me what it does 💀
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }
}