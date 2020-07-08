package org.pytorch.demo.yolo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;

import org.pytorch.demo.Constants;
import org.pytorch.demo.R;
import org.pytorch.demo.YOLOv5;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class YOLOV5Activity extends AbstractCameraXActivityYoLo<YOLOV5Activity.AnalysisResult> {

    private TextView mFpsText;
    private TextView mMsText;
    private TextView mMsAvgText;
    private ImageView resultImageView;
    // Yolo model
    private YOLOv5 mYolov5;

    // Result columns
    private static final int MOVING_AVG_PERIOD = 10;
    private static final String FORMAT_MS = "%dms";
    private static final String FORMAT_AVG_MS = "avg:%.0fms";
    private static final String FORMAT_FPS = "%.1fFPS";
    private long mMovingAvgSum = 0;
    private Queue<Long> mMovingAvgQueue = new LinkedList<>();


    private double threshold = 0.3, nms_threshold = 0.7;
    private boolean mAnalyzeImageErrorState;

    static class AnalysisResult {
        private final Box[] result;
        private final long analysisDuration;
        private final long moduleForwardDuration;
        private final Bitmap bitmapImage;

        public AnalysisResult(Box[] result,
                              Bitmap bitmapImage,
                              long moduleForwardDuration,
                              long analysisDuration) {
            this.result = result;
            this.bitmapImage = bitmapImage;
            this.moduleForwardDuration = moduleForwardDuration;
            this.analysisDuration = analysisDuration;
        }
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_yolov5;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        return ((ViewStub) findViewById(R.id.yolov5_texture_view_stub))
                .inflate()
                .findViewById(R.id.image_classification_texture_view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFpsText = findViewById(R.id.yolov5_fps_text);
        mMsText = findViewById(R.id.yolov5_ms_text);
        mMsAvgText = findViewById(R.id.yolov5_ms_avg_text);
        resultImageView = findViewById(R.id.imageView);
    }


    @Override
    protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
        mMovingAvgSum += result.moduleForwardDuration;
        mMovingAvgQueue.add(result.moduleForwardDuration);
        if (mMovingAvgQueue.size() > MOVING_AVG_PERIOD) {
            mMovingAvgSum -= mMovingAvgQueue.remove();
        }


        mMsText.setText(String.format(Locale.US, FORMAT_MS, result.moduleForwardDuration));
        if (mMsText.getVisibility() != View.VISIBLE) {
            mMsText.setVisibility(View.VISIBLE);
        }
        mFpsText.setText(String.format(Locale.US, FORMAT_FPS, (1000.f / result.analysisDuration)));
        if (mFpsText.getVisibility() != View.VISIBLE) {
            mFpsText.setVisibility(View.VISIBLE);
        }

        if (mMovingAvgQueue.size() == MOVING_AVG_PERIOD) {
            float avgMs = (float) mMovingAvgSum / MOVING_AVG_PERIOD;
            mMsAvgText.setText(String.format(Locale.US, FORMAT_AVG_MS, avgMs));
            if (mMsAvgText.getVisibility() != View.VISIBLE) {
                mMsAvgText.setVisibility(View.VISIBLE);
            }
        }

        // Draw bounding box etc.
        Bitmap mutableBitmap = result.bitmapImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        final Paint boxPaint = new Paint();
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4 * result.bitmapImage.getWidth() / 800);
        boxPaint.setTextSize(40 * result.bitmapImage.getWidth() / 800);
        for (Box box : result.result) {
            boxPaint.setColor(box.getColor());
            boxPaint.setStyle(Paint.Style.FILL);
            canvas.drawText(box.getLabel(), box.x0, box.y0, boxPaint);
            boxPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(box.getRect(), boxPaint);
        }
        resultImageView.setImageBitmap(mutableBitmap);
    }

    private static Bitmap ImageToBitmap(ImageProxy imageProxy){
        Image image = imageProxy.getImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

    }
    @Override
    @WorkerThread
    @Nullable
    protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
        if (mAnalyzeImageErrorState) {
            return null;
        }

        try {
            if (mYolov5 == null) {
                // initialise the module here
                mYolov5 = new YOLOv5();
                mYolov5.init(getAssets());
                Log.d("yolo", "thread == " + Thread.currentThread().getName());

            }
            final long moduleForwardStartTime = SystemClock.elapsedRealtime();
            // Calculate the forward time
            final long startTime = SystemClock.elapsedRealtime();
            // Buffer the image to bitmap
            Bitmap bitmapImage = ImageToBitmap(image);
            Box[] result = mYolov5.detect(bitmapImage, threshold, nms_threshold);
            final long moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;
            final long analysisDuration = SystemClock.elapsedRealtime() - startTime;
            return new AnalysisResult(result, bitmapImage, moduleForwardDuration, analysisDuration);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error during image analysis", e);
            mAnalyzeImageErrorState = true;
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    showErrorDialog(v -> YOLOV5Activity.this.finish());
                }
            });
            return null;
        }
    }


}
