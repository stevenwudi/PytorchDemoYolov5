package org.pytorch.demo.yolo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;

import org.pytorch.demo.BaseModuleActivity;
import org.pytorch.demo.StatusBarUtils;

public abstract class AbstractCameraXActivityYoLo<R> extends BaseModuleActivity {
  // Note: in order to change the input_size here, you have to recompile the
  // input_size in YoloV5.cpp
  private static final int input_size = 640;

  private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
  private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};
  private static final int REQUEST_EXTERNAL_STORAGE = 1;
  private static String[] PERMISSIONS_STORAGE = {
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE};

  private long mLastAnalysisResultTime;

  protected abstract int getContentViewLayoutId();

  protected abstract TextureView getCameraPreviewTextureView();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    StatusBarUtils.setStatusBarOverlay(getWindow(), true);
    setContentView(getContentViewLayoutId());
    Log.d("pytorch","oncreate = "+Thread.currentThread().toString());
    startBackgroundThread();
    Log.d("pytorch","after oncreate = "+Thread.currentThread().toString());
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this,
          PERMISSIONS,
          REQUEST_CODE_CAMERA_PERMISSION);

    } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    != PackageManager.PERMISSION_GRANTED){
      ActivityCompat.requestPermissions(
              this,
              PERMISSIONS_STORAGE,
              REQUEST_EXTERNAL_STORAGE
      );
    }else
    {
      setupCameraX();
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
      if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
        Toast.makeText(
            this,
            "You can't use image classification example without granting CAMERA permission",
            Toast.LENGTH_LONG)
            .show();
        finish();
      } else {
        setupCameraX();
      }
    }
  }

  private void setupCameraX() {
    Log.d("pytorch","setupCameraX = " +Thread.currentThread().getName());
    final TextureView textureView = getCameraPreviewTextureView();
    final PreviewConfig previewConfig = new PreviewConfig.Builder().build();
    final Preview preview = new Preview(previewConfig);
    preview.setOnPreviewOutputUpdateListener(output -> textureView.setSurfaceTexture(output.getSurfaceTexture()));

    final ImageAnalysisConfig imageAnalysisConfig =
        new ImageAnalysisConfig.Builder()
            .setTargetResolution(new Size(input_size/2, input_size/2))
            .setCallbackHandler(mBackgroundHandler)
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            .build();
    final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
    imageAnalysis.setAnalyzer(
        (image, rotationDegrees) -> {
          if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 500) {
            return;
          }

          final R result = analyzeImage(image, rotationDegrees);
          if (result != null) {
            mLastAnalysisResultTime = SystemClock.elapsedRealtime();
            runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
          }
        });

    CameraX.bindToLifecycle(this, preview, imageAnalysis);
  }

  @WorkerThread
  @Nullable
  protected abstract R analyzeImage(ImageProxy image, int rotationDegrees);

  @UiThread
  protected abstract void applyToUiAnalyzeImageResult(R result);
}
