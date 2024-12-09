package com.example.i_slang;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TensorFlowProcess {

    private static final String MODEL_PATH = "best_float32.tflite";
    private static final String LABEL_PATH = "labels.txt";
    public Interpreter interpreter;
    private int tensorWidth = 0;
    private int tensorHeight = 0;
    public int numChannel = 0;
    public int numElements = 0;
    private List<String> labels = new ArrayList<>();

    // Preprocess input
    private ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(INPUT_IMAGE_TYPE))
            .build();

    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final float CONFIDENCE_THRESHOLD = 0.1f;
    private static final float IOU_THRESHOLD = 0.5f;

    private Context context;
    public TensorFlowProcess(Context context) {
        this.context = context;
    }

    public void initializeModel(Context context) {
        try {
            ByteBuffer  model = FileUtil.loadMappedFile(context, MODEL_PATH);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // Set the number of threads
            interpreter = new Interpreter(model, options);

            int[] inputShape = interpreter.getInputTensor(0).shape();
            int[] outputShape = interpreter.getOutputTensor(0).shape();

            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];
            numChannel = outputShape[1];
            numElements = outputShape[2];

            // Read label.txt
            try (InputStream inputStream = context.getAssets().open(LABEL_PATH);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    labels.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ByteBuffer preprocessImage(Bitmap bitmap) {
        Log.d("ImageDimensions", "Width: " + bitmap.getWidth() + ", Height: " + bitmap.getHeight());
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false);
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(resizedBitmap);

        TensorImage processedImage = imageProcessor.process(tensorImage);
        return processedImage.getBuffer();
    }

    public List<BoundingBox> postProcess(float[] outputArray) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        Log.d("PostProcess", "Starting post-processing with numElements: " + numElements);
        for (int c = 0; c < numElements; c++) {
            float maxConf = -1.0f;
            int maxIdx = -1;
            int j = 4;
            int arrayIdx = c + numElements * j;
            while (j < numChannel) {
                if (outputArray[arrayIdx] > maxConf) {
                    maxConf = outputArray[arrayIdx];
                    maxIdx = j - 4;
                }
                j++;
                arrayIdx += numElements;
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                String className = labels.get(maxIdx);
                float cx = outputArray[c];
                float cy = outputArray[c + numElements];
                float w = outputArray[c + numElements * 2];
                float h = outputArray[c + numElements * 3];

                float x1 = cx - (w / 2f);
                float y1 = cy - (h / 2f);
                float x2 = cx + (w / 2f);
                float y2 = cy + (h / 2f);

                Log.d("PostProcess", "Detected Box: " + className + " Confidence: " + maxConf +
                        " Coords: (" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ")");

                if (x1 < 0f || x1 > 1f || y1 < 0f || y1 > 1f || x2 < 0f || x2 > 1f || y2 < 0f || y2 > 1f)
                    continue;

                boundingBoxes.add(new BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, className));
            }
        }

        return applyNMS(boundingBoxes);
    }

    public List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        List<BoundingBox> sortedBoxes = new ArrayList<>(boxes);
        Log.d("NMS", "Applying NMS to " + sortedBoxes.size() + " boxes");
        sortedBoxes.sort((b1, b2) -> Float.compare(b2.cnf, b1.cnf));

        List<BoundingBox> selectedBoxes = new ArrayList<>();
        while (!sortedBoxes.isEmpty()) {
            BoundingBox first = sortedBoxes.get(0);
            selectedBoxes.add(first);
            sortedBoxes.remove(0);

            Iterator<BoundingBox> iterator = sortedBoxes.iterator();
            while (((Iterator<?>) iterator).hasNext()) {
                BoundingBox nextBox = iterator.next();
                float iou = calculateIoU(first, nextBox);
                if (iou >= IOU_THRESHOLD) {
                    Log.d("NMS", "Removing box due to IoU >= threshold: " + iou);
                    iterator.remove();
                }
            }
        }
        Log.d("NMS", "Total Boxes After NMS: " + selectedBoxes.size());
        return selectedBoxes;
    }

    public float calculateIoU(BoundingBox box1, BoundingBox box2) {
        float x1 = Math.max(box1.x1, box2.x1);
        float y1 = Math.max(box1.y1, box2.y1);
        float x2 = Math.min(box1.x2, box2.x2);
        float y2 = Math.min(box1.y2, box2.y2);
        float intersectionArea = Math.max(0f, x2 - x1) * Math.max(0f, y2 - y1);
        float box1Area = box1.w * box1.h;
        float box2Area = box2.w * box2.h;
        return intersectionArea / (box1Area + box2Area - intersectionArea);
    }

    public Bitmap drawBoundingBoxes(Bitmap bitmap, List<BoundingBox> boxes) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(context,R.color.primary));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20f);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60f);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        for (BoundingBox box : boxes) {
            RectF rect = new RectF(
                    box.x1 * mutableBitmap.getWidth(),
                    box.y1 * mutableBitmap.getHeight(),
                    box.x2 * mutableBitmap.getWidth(),
                    box.y2 * mutableBitmap.getHeight()
            );
            canvas.drawRect(rect, paint);
            canvas.drawText(box.clsName, rect.left, rect.bottom, textPaint);
        }

        return mutableBitmap;
    }

}
