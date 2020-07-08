package org.pytorch.demo.yolo;

import android.content.Intent;
import android.os.Bundle;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;


public class YOLOListActivity extends AbstractListActivity {
    @Override
    protected int getListContentLayoutRes() {
        return R.layout.yolo_list_content;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.yolov5_click_area).setOnClickListener(v -> {
            final Intent intent = new Intent(YOLOListActivity.this, YOLOV5Activity.class);
            startActivity(intent);
        });

    }
}




