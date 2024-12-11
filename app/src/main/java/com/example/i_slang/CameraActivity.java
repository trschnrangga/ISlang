package com.example.i_slang;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button takephoto;
    private ImageCapture imageCapture;
    private ImageView imageView;
    private Button deletephoto;
    private Bitmap currentBitmap;
    private TextView predictionTextView;
    private TensorFlowProcess tensorFlowProcess;
    private Button flipCam;
    private SurfaceView surfaceView;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private int cameraFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camerapage);

        imageView = findViewById(R.id.image);
        previewView = findViewById(R.id.previewView);
        takephoto = findViewById(R.id.takePhotobtn);
        deletephoto = findViewById(R.id.deletePhotobtn);
        predictionTextView = findViewById(R.id.predictResults);
        flipCam = findViewById(R.id.flipcam);

        cameraFlag = 0;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permission already granted, proceed with camera functionality
            startCamera(cameraFlag);
        }

        flipCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle the cameraFlag
                cameraFlag = (cameraFlag == 0) ? 1 : 0;

                // Restart the camera with the new flag
                startCamera(cameraFlag);
            }
        });

        takephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
        deletephoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletePhoto();
            }
        });

        tensorFlowProcess = new TensorFlowProcess(this);
        tensorFlowProcess.initializeModel(this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
                startCamera(cameraFlag);
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera(int cameraFlag){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Fast capture mode// Set rotation to match display
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)                 // Optional: Choose 4:3 aspect ratio for standard captures
                        .build();


                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

//                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);
                CameraSelector cameraSelector = (cameraFlag == 1)
                        ? CameraSelector.DEFAULT_FRONT_CAMERA
                        : CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (previewView.getVisibility() == View.GONE){
            return;
        }
        // Ensure imageCapture use case is not null
        if (imageCapture == null) return;
        long startTime = System.nanoTime();
        // Create output file to hold the image
        long endTime = System.nanoTime();
        Log.d("Performance", "Time taken: " + (endTime - startTime) + "ns");
        // Set up ImageCapture metadata and output options

        // Take a photo
        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                    @OptIn(markerClass = ExperimentalGetImage.class)
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        long startTime = System.nanoTime();
                        currentBitmap = imageProxyToBitmap(image);
                        long endTime = System.nanoTime();
                        Log.d("Performance", "Time taken: " + (endTime - startTime) + "ns");
                        if (currentBitmap != null) {
                            Log.d("CameraXApp", "Bitmap created successfully: Width = " + currentBitmap.getWidth() + ", Height = " + currentBitmap.getHeight());
                        } else {
                            Log.d("CameraXApp", "Bitmap creation failed.");
                        }

                        runOnUiThread(() -> {
                            previewView.setVisibility(View.GONE);
                            imageView.setVisibility(View.VISIBLE);
                            processImage(currentBitmap);
                        });

                        image.close();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraXApp", "Photo capture failed: " + exception.getMessage(), exception);
                    }
                }
        );
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);
    }

    private void deletePhoto(){
        if (currentBitmap != null && !currentBitmap.isRecycled()){
            currentBitmap.recycle();
            currentBitmap = null;
        }
        previewView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
        imageView.setImageBitmap(null);
        predictionTextView.setText("\n\n");
    }

    private void processImage(Bitmap bitmap) {
        // Pre-process the image and run inference
        ByteBuffer preprocessedImage = tensorFlowProcess.preprocessImage(bitmap);

        // Run the model inference
        TensorBuffer output = TensorBuffer.createFixedSize(
                new int[] {1, tensorFlowProcess.numChannel, tensorFlowProcess.numElements},
                DataType.FLOAT32
        );

        tensorFlowProcess.interpreter.run(preprocessedImage, output.getBuffer());

        Log.e("outputArray", String.valueOf(output.getBuffer()));
        // Post-process the output to get bounding boxes and labels
        List<BoundingBox> boundingBoxes = tensorFlowProcess.postProcess(output.getFloatArray());

        // Draw bounding boxes on the image
        Bitmap resultBitmap = tensorFlowProcess.drawBoundingBoxes(bitmap, boundingBoxes);

        // Display the result image
        imageView.setImageBitmap(resultBitmap);

        // Display the prediction results
        StringBuilder predictions = new StringBuilder();
        for (BoundingBox box : boundingBoxes) {
            predictions.append("Detected Alphabet: ").append("'" + box.clsName + "'").append("\n");
            predictions.append("Confidence: ").append(Math.floor(box.cnf*100)+"%").append("\n\n");
        }
        predictionTextView.setText(predictions.toString());
    }
}