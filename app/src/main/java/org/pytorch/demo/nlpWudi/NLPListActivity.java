package org.pytorch.demo.nlpWudi;

import android.content.Intent;
import android.os.Bundle;

import org.pytorch.demo.AbstractListActivity;
import org.pytorch.demo.R;

public class NLPListActivity extends AbstractListActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    findViewById(R.id.kaggle_TSE_click_area).setOnClickListener(v -> {
      final Intent intent = new Intent(NLPListActivity.this, BertTextClassificationActivity.class);
      startActivity(intent);
    });
  }

  @Override
  protected int getListContentLayoutRes() {
    return R.layout.nlp_list_content;
  }
}
