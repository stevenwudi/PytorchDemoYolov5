package org.pytorch.demo;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.pytorch.demo.yolo.Box;

public class YOLOv5 {
    static {
        System.loadLibrary("yolov5");
    }

    public static native void init(AssetManager manager);
    public static native Box[] detect(Bitmap bitmap, double threshold, double nms_threshold);
}
