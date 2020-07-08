package org.pytorch.demo;

import android.content.Intent;
import android.os.Bundle;

import org.pytorch.demo.nlpWudi.NLPListActivity;
import org.pytorch.demo.vision.VisionListActivity;
import org.pytorch.demo.yolo.YOLOListActivity;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    findViewById(R.id.main_vision_click_view).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, VisionListActivity.class)));
    findViewById(R.id.main_nlp_click_view).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, NLPListActivity.class)));
    findViewById(R.id.main_yolo_click_view).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, YOLOListActivity.class)));

  }
}
