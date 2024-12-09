package com.example.i_slang;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class OtherPages extends AppCompatActivity {


    private Button backBtn;
    private FloatingActionButton cameraBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int layoutId = intent.getIntExtra("layout_id", R.layout.activity_main); // Default fallback layout

        setContentView(layoutId);

        backBtn = findViewById(R.id.backButton);
        cameraBtn = findViewById(R.id.FAB);

        // Set the same listener to all views
        backBtn.setOnClickListener(commonClickListener);
        cameraBtn.setOnClickListener(commonClickListener);
    }

    View.OnClickListener commonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.backButton){
                Intent intent = new Intent(OtherPages.this, MainActivity.class);
                startActivity(intent);
            }

            else if(view.getId() == R.id.FAB){
                Intent intent = new Intent(OtherPages.this, CameraActivity.class);
                startActivity(intent);
            }

        }
    };
}