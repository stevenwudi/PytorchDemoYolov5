package org.pytorch.demo.nlpWudi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.BaseModuleActivity;
import org.pytorch.demo.InfoViewFactory;
import org.pytorch.demo.R;
import org.pytorch.demo.Utils;
import org.pytorch.demo.vision.view.ResultRowView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

public class BertTextClassificationActivity extends BaseModuleActivity {

  private static final long EDIT_TEXT_STOP_DELAY = 600l;
  private static final int TOP_K = 3;
  private static final String SCORES_FORMAT = "%.2f";

  private EditText mEditText;
  private View mResultContent;
  private ResultRowView[] mResultRowViews = new ResultRowView[TOP_K];

  private String mLastBgHandledText;
  private static class AnalysisResult {
    private final String[] topKClassNames;
    private final float[] topKScores;

    public AnalysisResult(String[] topKClassNames, float[] topKScores) {
      this.topKClassNames = topKClassNames;
      this.topKScores = topKScores;
    }
  }

  private Runnable mOnEditTextStopRunnable = () -> {
    final String text = mEditText.getText().toString();
    mBackgroundHandler.post(() -> {
      if (TextUtils.equals(text, mLastBgHandledText)) {
        return;
      }

      if (TextUtils.isEmpty(text)) {
        runOnUiThread(() -> applyUIEmptyTextState());
        mLastBgHandledText = null;
        return;
      }

      final AnalysisResult result;
      try {
        result = analyzeText(text);
        if (result != null) {
          runOnUiThread(() -> applyUIAnalysisResult(result));
          mLastBgHandledText = text;
        }
      } catch (JSONException e) {
        e.printStackTrace();
      }

    });
  };

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_text_classification);
    mEditText = findViewById(R.id.text_classification_edit_text);
    findViewById(R.id.text_classification_clear_button).setOnClickListener(v -> mEditText.setText(""));

    final ResultRowView headerRow = findViewById(R.id.text_classification_result_header_row);
    headerRow.nameTextView.setText(R.string.tweet_sentiment);
    headerRow.scoreTextView.setText(R.string.text_classification_score);
    headerRow.setVisibility(View.VISIBLE);

    mResultRowViews[0] = findViewById(R.id.text_classification_top1_result_row);
    mResultRowViews[1] = findViewById(R.id.text_classification_top2_result_row);
    mResultRowViews[2] = findViewById(R.id.text_classification_top3_result_row);
    mResultContent = findViewById(R.id.text_classification_result_content);
    mEditText.addTextChangedListener(new InternalTextWatcher());
  }

  @WorkerThread
  @Nullable
  private AnalysisResult analyzeText(final String text)  throws JSONException {
    URL searchUrl = NetworkUtils.buildUrl(text);
    String searchResults = null;
    try {
      searchResults = NetworkUtils.getResponseFromHttpUrl(searchUrl);
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Read the output as JSON object
    JSONObject sentiment = new JSONObject(searchResults);
    double positive_score = sentiment.getDouble("positive");
    double neutral_score = sentiment.getDouble("neutral");
    double negative_score = sentiment.getDouble("negative");

    final String[] topKClassNames = new String[TOP_K];
    topKClassNames[0] = "positive";
    topKClassNames[1] = "neutral";
    topKClassNames[2] = "negative";
    final float[] topKScores = new float[TOP_K];
    topKScores[0] = (float) positive_score;
    topKScores[1] = (float) neutral_score;
    topKScores[2] = (float) negative_score;


    return new AnalysisResult(topKClassNames, topKScores);
  }

  private void applyUIAnalysisResult(AnalysisResult result) {
    for (int i = 0; i < TOP_K; i++) {
      setUiResultRowView(
          mResultRowViews[i],
          result.topKClassNames[i],
          String.format(Locale.US, SCORES_FORMAT, result.topKScores[i]));
    }

    mResultContent.setVisibility(View.VISIBLE);
  }

  private void applyUIEmptyTextState() {
    mResultContent.setVisibility(View.GONE);
  }

  private void setUiResultRowView(ResultRowView resultRowView, String name, String score) {
    resultRowView.nameTextView.setText(name);
    resultRowView.scoreTextView.setText(score);
    resultRowView.setProgressState(false);
  }

  @Override
  protected int getInfoViewCode() {
    return InfoViewFactory.INFO_VIEW_TYPE_TEXT_CLASSIFICATION;
  }

  private class InternalTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
      mUIHandler.removeCallbacks(mOnEditTextStopRunnable);
      mUIHandler.postDelayed(mOnEditTextStopRunnable, EDIT_TEXT_STOP_DELAY);
    }
  }

}
