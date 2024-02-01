package com.signsense.app;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "VideoActivity";
    private static final int CODE_VIDEO = 400;

    private Button selectVideo;
    VideoView videoView;

    MediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        selectVideo = findViewById(R.id.button_selectVideo);
        videoView = findViewById(R.id.view_videoView);

        // Attach media controller to video view (pause, skip, etc.)
        mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);

        selectVideo.setOnClickListener(view -> {
            // On click, open new window for selecting videos
            Intent selectVideo = new Intent(Intent.ACTION_PICK);
            selectVideo.setType("video/*");
            startActivityForResult(selectVideo, CODE_VIDEO);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_VIDEO) {
            // Get video Uri and attach it to video view
            Uri videoUri = data.getData();
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(videoUri);
            videoView.start();
        }
    }
}