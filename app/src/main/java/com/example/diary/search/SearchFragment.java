package com.example.diary.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.example.diary.R;
import com.example.diary.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SearchFragment extends Fragment {
  private static final int NEW_SEARCH_REQUEST_CODE = 779779;
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private static SimpleDateFormat formatter;

  // Global UI elements
  private TextView resultTitle;
  private JSONArray results = new JSONArray();
  private View thisView;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    formatter = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH);
    // Init networking
    AndroidNetworking.initialize(getActivity().getApplicationContext());
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.search_main, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    thisView = view;
    resultTitle = view.findViewById(R.id.result_title);

    MaterialButton newSearchButton = view.findViewById(R.id.new_search_button);
    newSearchButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        DialogFragment newFragment = new NewSearchFragment();
        newFragment.setTargetFragment(SearchFragment.this, NEW_SEARCH_REQUEST_CODE);
        newFragment.show(SearchFragment.this.getParentFragmentManager(), "NewSearchFragment");
      }
    });
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    HashMap<String, String> search
        = (HashMap<String, String>) data.getSerializableExtra("search");
    if (requestCode == NEW_SEARCH_REQUEST_CODE) {
      onNewSearch(search);
    }
  }

  private void onNewSearch(Map<String, String> search) {
    resultTitle.setText("Fetching data...");

    AndroidNetworking.get(getString(R.string.api_root)+"media")
        .addHeaders("Authorization", "Basic " + Utils.getAuthentication(getActivity()))
        .addQueryParameter(search)
        .setTag("searchMedias")
        .setPriority(Priority.MEDIUM)
        .build()
        .getAsJSONArray(new JSONArrayRequestListener() {
          @Override
          public void onResponse(JSONArray response) {
            results = response;
            updateResultsView();
          }
          @Override
          public void onError(ANError error) {
            error.printStackTrace();
          }
        });
  }

  private void updateResultsView() {
    LinearLayout viewContainer = thisView.findViewById(R.id.search_result_container);
    viewContainer.removeAllViews();

    for (int i = 0; i < results.length(); i++) {
      try {
        JSONObject result = results.getJSONObject(i);
        viewContainer.addView(createCardViewFromResult(result));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    resultTitle.setText(results.length() + " result(s)");
  }

  private RelativeLayout createCardViewFromResult(JSONObject result) throws JSONException {
    LayoutInflater inflater = LayoutInflater.from(getActivity().getApplicationContext());
    RelativeLayout cardView = (RelativeLayout) inflater.inflate(
        R.layout.message_card_layout, null, false);

    RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    params.setMargins(150, 60, 150, 60);
    cardView.setLayoutParams(params);

    // Example JSON
    // "id": "965ca51a0e885c5f943a59bd63e07d72",
    // "file_name": "diary_upload8962754975735468790_sample.pdf",
    // "has_file": 1,
    // "text_message": "Pdf",
    // "username": "test",
    // "longitude": null,
    // "latitude": null,
    // "ip_address": "91.159.139.72",
    // "created_at": "2020-12-28 04:23:36"

    TextView cardName = cardView.findViewById(R.id.card_name);
    TextView cardDateTime = cardView.findViewById(R.id.card_date_time);
    TextInputEditText cardMessage = cardView.findViewById(R.id.card_message);
    TextInputEditText cardLocation = cardView.findViewById(R.id.card_location);
    TextInputEditText cardIpAddress = cardView.findViewById(R.id.card_ip_address);
    TextInputEditText cardFile = cardView.findViewById(R.id.card_file);
    // MaterialButton cardDownload = cardView.findViewById(R.id.card_download_button);

    cardName.setText(result.getString("username"));
    cardDateTime.setText(result.getString("created_at"));
    cardMessage.setText(result.getString("text_message"));
    cardLocation.setText(
        (!result.isNull("latitude") && !result.isNull("longitude"))
          ? result.getString("latitude") + ", " + result.getString("longitude")
          : "Not available"
    );
    cardIpAddress.setText(result.getString("ip_address"));
    cardFile.setText(
        (result.getInt("has_file") == 1)
          ? result.getString("file_name")
          : "No file"
    );

    return cardView;
  }
}
