package com.example.i_slang;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MainActivity extends AppCompatActivity {


    private CardView bahasaIsyaratPage;
    private CardView galleryPage;
    private CardView appInfoPage;
    private CardView profilePage;
    private CardView tutorialPage;
    private FloatingActionButton camerabutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bahasaIsyaratPage = findViewById(R.id.bahasa_isyarat);
        galleryPage = findViewById(R.id.gallery);
        appInfoPage = findViewById(R.id.info_aplikasi);
        profilePage = findViewById(R.id.profile);
        tutorialPage = findViewById(R.id.tutorial);
        camerabutton = findViewById(R.id.FAB);

        // Set the same listener to all views
        bahasaIsyaratPage.setOnClickListener(commonClickListener);
        galleryPage.setOnClickListener(commonClickListener);
        appInfoPage.setOnClickListener(commonClickListener);
        profilePage.setOnClickListener(commonClickListener);
        tutorialPage.setOnClickListener(commonClickListener);
        camerabutton.setOnClickListener(commonClickListener);
    }

    View.OnClickListener commonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.bahasa_isyarat){
                Intent intent = new Intent(MainActivity.this, OtherPages.class);
                intent.putExtra("layout_id", R.layout.bahasa_isyarat); // Pass the desired layout
                startActivity(intent);
            }
            else if (view.getId() == R.id.gallery){

            }
            else if(view.getId() == R.id.info_aplikasi){
                Intent intent = new Intent(MainActivity.this, OtherPages.class);
                intent.putExtra("layout_id", R.layout.info_aplikasi); // Pass the desired layout
                startActivity(intent);
            }
            else if(view.getId() == R.id.profile){

            }
            else if(view.getId() == R.id.tutorial){
                Intent intent = new Intent(MainActivity.this, OtherPages.class);
                intent.putExtra("layout_id", R.layout.tutorial); // Pass the desired layout
                startActivity(intent);
            }
            else if(view.getId() == R.id.FAB){
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }

        }
    };
}