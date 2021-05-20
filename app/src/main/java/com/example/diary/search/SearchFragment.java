package com.example.diary.search;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONArrayRequestListener;
import com.example.diary.R;
import com.example.diary.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static android.content.Context.DOWNLOAD_SERVICE;

public class SearchFragment extends Fragment {
  private static final String TAG = "MyDebug";
  private static final int NEW_SEARCH_REQUEST_CODE = 779779;
  private static final String[] IMAGE_EXTENSIONS = {"png", "jpg", "jpeg", "bmp", "gif"};
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  // Global UI elements
  private TextView resultTitle;
  private View thisView;

  private long downloadId;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Init networking
    AndroidNetworking.initialize(getActivity().getApplicationContext());

    // Register download receiver
    getActivity().registerReceiver(
        onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    );
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

    AndroidNetworking.get(Utils.getApiRoot(getContext()) + "media")
        .addHeaders("Authorization", "Basic " + Utils.getAuthentication(getActivity()))
        .addQueryParameter(search)
        .setTag("searchMedias")
        .setPriority(Priority.MEDIUM)
        .build()
        .getAsJSONArray(new JSONArrayRequestListener() {
          @Override
          public void onResponse(JSONArray response) {
            Log.d(TAG, "MEDIA OK: " + response);
            updateResultsView(response);
          }
          @Override
          public void onError(ANError error) {
            error.printStackTrace();
          }
        });
  }

  private void updateResultsView(JSONArray response) {
    LinearLayout viewContainer = thisView.findViewById(R.id.search_result_container);
    viewContainer.removeAllViews();

    for (int i = 0; i < response.length(); i++) {
      try {
        JSONObject result = response.getJSONObject(i);
        viewContainer.addView(createCardViewFromResult(result));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    resultTitle.setText(response.length() + " result(s)");
  }

  @SuppressLint("ClickableViewAccessibility")
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

    final TextView cardName = cardView.findViewById(R.id.card_name);
    final TextView cardDateTime = cardView.findViewById(R.id.card_date_time);
    final TextInputEditText cardMessage = cardView.findViewById(R.id.card_message);
    final TextInputEditText cardLocation = cardView.findViewById(R.id.card_location);
    final TextInputEditText cardIpAddress = cardView.findViewById(R.id.card_ip_address);
    final TextInputEditText cardFile = cardView.findViewById(R.id.card_file);

    // De-activate inputs for read-only texts
    cardMessage.setKeyListener(null);
    cardLocation.setKeyListener(null);
    cardIpAddress.setKeyListener(null);
    cardFile.setKeyListener(null);

    cardName.setText(result.getString("username"));
    cardDateTime.setText(result.getString("created_at"));
    cardMessage.setText(result.getString("text_message"));
    cardIpAddress.setText(result.getString("ip_address"));
    cardFile.setText(
        (result.getInt("has_file") == 1)
          ? result.getString("file_name")
          : "No file"
    );

    // Open location
    TextInputLayout cardLocationContainer = cardView.findViewById(R.id.card_location_container);
    String locationString = "Not available";
    if (!result.isNull("latitude") && !result.isNull("longitude")) {
      locationString = result.getString("latitude")
          + ", " + result.getString("longitude");

      final String finalLocationString = locationString;

      cardLocationContainer.setEndIconOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
              "https://www.google.com/maps/search/?api=1&query=" + finalLocationString
          ));
          startActivity(browserIntent);
        }
      });
    }
    else {
      // Remove go to map button if location is null
      cardLocationContainer.setEndIconMode(TextInputLayout.END_ICON_NONE);
    }
    cardLocation.setText(locationString);

    // Open file
    TextInputLayout cardFileContainer = cardView.findViewById(R.id.card_file_container);
    String fileString = "No file";
    if (result.getInt("has_file") == 1) {
      fileString = result.getString("file_name");

      final JSONObject finalResult = result;

      String fileExtension = "";

      int i = fileString.lastIndexOf('.');
      if (i > 0) {
        fileExtension = fileString.substring(i+1);
      }

      final String fileUrl = Utils.getApiRoot(getContext())
              + "media/file/" + finalResult.getString("id");

      if (Arrays.asList(IMAGE_EXTENSIONS).contains(fileExtension)) {
        LinearLayout containerLayout = cardView.findViewById(R.id.card_image_preview_layout);
        containerLayout.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams layoutParams
            = (LinearLayout.LayoutParams) containerLayout.getLayoutParams();
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        containerLayout.setLayoutParams(layoutParams);
        new DownloadImageTask(
                (ImageView) cardView.findViewById(R.id.card_image_view)
        ).execute(fileUrl);
      }

      cardFileContainer.setEndIconOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          try {
            downloadFile(
                    Utils.getApiRoot(getContext()) + "media/file/" + finalResult.getString("id"),
                finalResult.getString("file_name")
            );
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      });
    }
    else {
      cardFileContainer.setEndIconMode(TextInputLayout.END_ICON_NONE);
    }
    cardFile.setText(fileString);
    return cardView;
  }

  private void downloadFile(String url, String fileName) {
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalFilesDir(
//            Environment.DIRECTORY_DOWNLOADS,
//            getString(R.string.download_dir) + "/" + fileName
            getContext(),
            Environment.DIRECTORY_DOWNLOADS,
            getString(R.string.download_dir) + "/" + fileName
        )
        .setMimeType(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(fileName)
            )
        )
        .setTitle(fileName)// Title of the Download Notification
        .setDescription("Downloading")// Description of the Download Notification
        .setRequiresCharging(false)// Set if charging is required to begin the download
        .setAllowedOverMetered(true)// Set if download is allowed on Mobile network
        .setAllowedOverRoaming(true);// Set if download is allowed on roaming network

    request.allowScanningByMediaScanner();

    DownloadManager downloadManager =
        (DownloadManager)this.getActivity().getSystemService(DOWNLOAD_SERVICE);
    downloadId = downloadManager.enqueue(request);// enqueue puts the download request in the queue.
  }

  private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      // Fetching the download id received with the broadcast
      long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
      // Checking if the received broadcast is for our enqueued download by matching download id
      if (downloadId == id) {
        DownloadManager downloadManager =
            (DownloadManager)getActivity().getSystemService(DOWNLOAD_SERVICE);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor downloadCursor = downloadManager.query(query);

        String localUri = "";
        String mime = "";

        if (downloadCursor.moveToFirst()) {
          localUri = downloadCursor.getString(downloadCursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
          mime = downloadCursor.getString(downloadCursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
        }
        downloadCursor.close();

        File file = new File(Uri.parse(localUri).getPath());

        String fileExtension = "";

        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
          fileExtension = file.getName().substring(i+1);
        }

        if (fileExtension.equals("m4a")) {
          mime = "audio/*";
        }

        Uri fileUri = FileProvider.getUriForFile(
            getActivity().getApplicationContext(),
            getActivity().getPackageName() + ".provider",
            file
        );

        // Open file with user selected app
        Intent fileIntent = new Intent();
        fileIntent.setAction(Intent.ACTION_VIEW);
        fileIntent.setDataAndType(fileUri, mime);
        fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(fileIntent);
      }
    }
  };

  private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
      this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
      String urlDisplay = urls[0];
      Bitmap mIcon = null;
      try {
        InputStream in = new java.net.URL(urlDisplay).openStream();
        mIcon = BitmapFactory.decodeStream(in);
      } catch (Exception e) {
        Log.e("Error", e.getMessage());
        e.printStackTrace();
      }
      return mIcon;
    }

    protected void onPostExecute(Bitmap result) {
      bmImage.setImageBitmap(result);
    }
  }
}
