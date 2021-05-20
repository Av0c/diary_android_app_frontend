package com.example.diary.submit;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.diary.FileUtils;
import com.example.diary.R;
import com.example.diary.Utils;
import com.example.diary.login.LoginActivity;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SubmitFragment extends Fragment {
  private static final String TAG = "MyDebug";

  static final int REQUEST_LOCATION_SETTINGS = 7090;
  static final int REQUEST_IMAGE_CAPTURE = 1000;
  static final int REQUEST_VIDEO_CAPTURE = 1001;
  static final int REQUEST_VOICE_CAPTURE = 1002;
  static final int REQUEST_FILE_SELECT = 1003;

  private TextInputEditText messageInput;
  private TextInputEditText attachmentInfo;
  private TextInputEditText locationInfo;

  private FusedLocationProviderClient fusedLocationClient;

  private String mediaPath;
  private File attachedFile;
  private Location lastLocation;

  private final OkHttpClient okHttpClient = new OkHttpClient();

  Call post(String url, RequestBody body, Callback callback) {
    Request request = new Request.Builder()
        .header(
            "Authorization",
            "Basic " + Utils.getAuthentication(getActivity().getApplicationContext())
        )
        .url(url)
        .post(body)
        .build();

    Call call = okHttpClient.newCall(request);
    call.enqueue(callback);
    return call;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "Debugging");

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
    Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.submit_main, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    // Name display
    TextInputEditText nameInput = view.findViewById(R.id.name_input);
    nameInput.setText(
        Utils.getName(getContext())
    );

    messageInput = view.findViewById(R.id.message_input);
    attachmentInfo = view.findViewById(R.id.attachment_info_text);
    locationInfo = view.findViewById(R.id.location_info);

    locationInfo.setKeyListener(null); // Disable editing location info
    TextInputLayout locationInfoContainer = view.findViewById(R.id.location_info_container);
    locationInfoContainer.setEndIconOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onFetchLocation();
      }
    });

    // Attachment inputs
    MaterialButton pictureButton = view.findViewById(R.id.attachment_camera_button);
    MaterialButton recordButton = view.findViewById(R.id.attachment_record_button);
    MaterialButton voiceButton = view.findViewById(R.id.attachment_voice_button);
    MaterialButton fileButton = view.findViewById(R.id.attachment_file_button);

    // Controls
    MaterialButton submitButton = view.findViewById(R.id.submit_button);
    MaterialButton clearButton = view.findViewById(R.id.clear_button);

    pictureButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dispatchTakePictureIntent();
      }
    });

    recordButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dispatchTakeVideoIntent();
      }
    });

    voiceButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dispatchVoiceRecordIntent();
      }
    });

    fileButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dispatchFileSelectIntent();
      }
    });

    submitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        onSubmit();
      }
    });

    clearButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        attachmentInfo.setText("No file attached");
        attachedFile = null;
      }
    });

    MaterialButton logoutButton = view.findViewById(R.id.logout_button);
    logoutButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
      }
    });

    // Fetch first location infos
    onFetchLocation();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (resultCode == Activity.RESULT_OK) {
      switch (requestCode) {
        case REQUEST_IMAGE_CAPTURE: {
          // User took a photo
          File imageFile = new File(mediaPath);

          BitmapFactory.Options bitMapOption = new BitmapFactory.Options();
          bitMapOption.inJustDecodeBounds = true;
          BitmapFactory.decodeFile(mediaPath, bitMapOption);

          int imageWidth = bitMapOption.outWidth;
          int imageHeight = bitMapOption.outHeight;

          attachmentInfo.setText(String.format(
              "Attached: 1 Photo " +
                  "\nResolution: %sx%s" +
                  "\nFile size: %s KB",
              imageWidth, imageHeight,
              imageFile.length() / 1000
          ));

          attachedFile = imageFile;
        }
        break;
        case REQUEST_VIDEO_CAPTURE: {
          // User record a video
          File videoFile = new File(mediaPath);

          Uri videoUri = FileProvider.getUriForFile(
              getActivity().getApplicationContext(),
              getActivity().getPackageName() + ".provider",
              videoFile
          );

          MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
          metadataRetriever.setDataSource(getActivity().getApplicationContext(), videoUri);

          String height = metadataRetriever
              .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
          String width = metadataRetriever
              .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
          String duration = metadataRetriever
              .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
          long durationSec = Long.parseLong(duration) / 1000;
          String size = String.valueOf(videoFile.length() / 1000);
          attachmentInfo.setText(String.format(
              "Attached: 1 Video " +
                  "\nDuration: %ss" +
                  "\nResolution: %sx%s" +
                  "\nFile size: %s KB",
              durationSec,
              width, height,
              size
          ));
          attachedFile = videoFile;
        }
        break;
        case REQUEST_VOICE_CAPTURE: {
          // User record a audio clip using the built-in recorder
          // If device has no built-in recorder, button does nothing
          Uri localUri = data.getData();

          String voicePath = FileUtils.getPath(getActivity().getApplicationContext(), localUri);

          File voiceFile = new File(voicePath);

          MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
          metadataRetriever.setDataSource(getActivity().getApplicationContext(), localUri);
          String duration = metadataRetriever
              .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

          long durationSec = Long.parseLong(duration) / 1000;
          String size = String.valueOf(voiceFile.length() / 1000);

          attachmentInfo.setText(String.format(
              "Attached: 1 Audio " +
                  "\nDuration: %ss" +
                  "\nFile size: %s KB",
              durationSec,
              size
          ));
          attachedFile = voiceFile;
        }
        break;
        case REQUEST_FILE_SELECT: {
          Uri uri = data.getData();
          // String path = FileUtils.getPath(getActivity().getApplicationContext(), uri);
          // Log.d(TAG, "onActivityResult: " + fileUri);

          Cursor cursor = getActivity().getContentResolver()
              .query(uri, null, null, null, null, null);

          String displayName = "Unknown";
          String size = "Unknown";

          try {
            if (cursor != null && cursor.moveToFirst()) {
              displayName = cursor.getString(
                  cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

              int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

              size = null;
              if (!cursor.isNull(sizeIndex)) {
                size = String.valueOf(cursor.getInt(sizeIndex) / 1000);
              }
            }
          } finally {
            cursor.close();
          }

          attachmentInfo.setText(String.format(
              "Attached: 1 File " +
                  "\nName: %s" +
                  "\nFile size: %s KB",
              displayName,
              size
          ));


          InputStream inputStream;
          try {
            inputStream = getActivity().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
              attachedFile = inputStreamToFile(inputStream, displayName);
            }
            else {
              Log.e("FILE ERROR", "File is null ?");
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        break;
        case REQUEST_LOCATION_SETTINGS:
          onFetchLocation();
          break;
      }
    }
  }

  private void onFetchLocation() {
    // Setup LocationRequest object to check location settings
    final LocationRequest[] locationRequest = {LocationRequest.create()};
    locationRequest[0].setInterval(5000);
    locationRequest[0].setFastestInterval(5000);
    locationRequest[0].setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest[0]);

    // Check if location settings are satisfied
    final SettingsClient client = LocationServices.getSettingsClient(getActivity());
    Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

    task.addOnSuccessListener(getActivity(), new OnSuccessListener<LocationSettingsResponse>() {
      @Override
      public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
        Log.d(TAG, "Location settings success");
        if (
            ActivityCompat.checkSelfPermission(
                getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
                getActivity().getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
          Toast
              .makeText(
                  getActivity().getApplicationContext(),
                  "No location permission, please allow it from Settings",
                  Toast.LENGTH_LONG
              )
              .show();
          locationInfo.setText("Not available");
          return;
        }
        Log.d(TAG, "Location permission is good");
        Log.d(TAG, locationRequest[0].toString());
        LocationCallback locationCallback =  new LocationCallback() {
          @Override
          public void onLocationResult(@NotNull LocationResult locationResult) {
            Log.d(TAG, locationResult.toString());
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();
            lastLocation = location;
            locationInfo.setText(
                String.format(
                    Locale.ENGLISH,
                    "%.4f, %.4f",
                    location.getLatitude(),
                    location.getLongitude()
                )
            );
          }
        };

        fusedLocationClient.requestLocationUpdates(
            locationRequest[0],
            locationCallback,
            Looper.myLooper()
        );
      }
    });

    task.addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        Log.d(TAG, "Location settings failed: " + e.toString());
        if (e instanceof ResolvableApiException) {
          // Location settings are wrong but can be fixed (turned on)
          try {
            ResolvableApiException resolvable = (ResolvableApiException) e;
//            startIntentSenderForResult(
//                resolvable.getResolution().getIntentSender(),
//                REQUEST_LOCATION_SETTINGS,
//                null, 0, 0, 0, null
//            );
            Log.d(TAG, "Trying to resolve: " + resolvable.toString());
            resolvable.startResolutionForResult(
                getActivity(),
                REQUEST_LOCATION_SETTINGS
            );

          } catch (IntentSender.SendIntentException sendEx) {
            Log.e(TAG, "Failed to resolve location settings");
          }
        }
      }
    });
  }

  private void onSubmit() {
    String message = messageInput.getText().toString();

    MultipartBody.Builder body = new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("text_message", message);

    if (lastLocation != null) {
      String longitude = String.valueOf(lastLocation.getLongitude());
      String latitude = String.valueOf(lastLocation.getLatitude());
      body = body
          .addFormDataPart("longitude", longitude)
          .addFormDataPart("latitude", latitude);
    }

    if (attachedFile != null) {
      body = body.addFormDataPart(
          "file",
          attachedFile.getName(),
          RequestBody.create(
              attachedFile, MediaType.parse("application/octet-stream")
          )
      );
    }

    RequestBody requestBody = body.build();

    post(Utils.getApiRoot(getContext()) + "media", requestBody, new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        Log.e(TAG, "MEDIA FAIL: " + e.toString());
        getActivity().runOnUiThread(new Runnable() {
          public void run() {
            onSubmitResult(false);
          }
        });
      }

      @Override
      public void onResponse(Call call, Response response) {
        if (response.isSuccessful()) {
          Log.d(TAG, "MEDIA OK: " + response.toString());
          getActivity().runOnUiThread(new Runnable() {
            public void run() {
              onSubmitResult(true);
            }
          });
        } else {
          Log.e(TAG, "MEDIA FAIL: " + response.toString());
          getActivity().runOnUiThread(new Runnable() {
            public void run() {
              onSubmitResult(false);
            }
          });
        }
        response.body().close();
      }
    });
  }

  private void onSubmitResult(boolean isSuccessful) {
    if (isSuccessful) {
      Toast
          .makeText(
              getActivity().getApplicationContext(),
              "Your entry is submitted !",
              Toast.LENGTH_LONG
          )
          .show();
    }
    else {
      Toast
          .makeText(
              getActivity().getApplicationContext(),
              "Failed to submit, please try again",
              Toast.LENGTH_LONG
          )
          .show();
    }
  }

  private File inputStreamToFile(InputStream initialStream, String fileName) throws IOException {
    byte[] buffer = new byte[initialStream.available()];
    initialStream.read(buffer);

    File outputDir = getActivity().getApplicationContext().getCacheDir();
    File outputFile;
    try {
      outputFile = File.createTempFile("diary_upload", "_" + fileName, outputDir);

      File targetFile = new File(outputFile.getAbsolutePath());
      OutputStream outStream = new FileOutputStream(targetFile);
      outStream.write(buffer);

      return targetFile;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private File createFile(String dirType, String suffix) throws IOException {
    // Create a file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String fileName = "DIARY_" + timeStamp + "_";

    File storageDir = getActivity()
        .getApplicationContext()
        .getExternalFilesDir(dirType);

    File file = File.createTempFile(
        fileName,
        suffix,
        storageDir
    );

    mediaPath = file.getAbsolutePath();

    return file;
  }

  private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    try {
      File photoFile = null;
      try {
        photoFile = createFile(Environment.DIRECTORY_DCIM, ".jpg");
      } catch (IOException e) {
        Log.e("Failed to create image file", e.getMessage());
      }

      if (photoFile != null) {
        Uri photoURI = FileProvider.getUriForFile(
            getActivity().getApplicationContext(),
            "com.example.diary.provider",
            photoFile
        );
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
      }
    } catch (ActivityNotFoundException e) {
      Log.e("Failed to start intent", e.getMessage());
    }
  }

  private void dispatchTakeVideoIntent() {
    Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    try {
      File file = null;
      try {
        file = createFile(Environment.DIRECTORY_DCIM, ".mp4");
      } catch (IOException e) {
        Log.e("Failed to create video file", e.getMessage());
      }

      if (file != null) {
        Uri uri = FileProvider.getUriForFile(
            getActivity().getApplicationContext(),
            "com.example.diary.provider",
            file
        );
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
      }

    } catch (ActivityNotFoundException e) {
      Log.e("Failed to start intent", e.getMessage());
    }
  }

  private void dispatchVoiceRecordIntent() {
    try {
      Intent voiceIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
      startActivityForResult(voiceIntent, REQUEST_VOICE_CAPTURE);
    } catch (ActivityNotFoundException e) {
      Log.e("Failed to start intent", e.getMessage());
    }
  }

  private void dispatchFileSelectIntent() {
    Intent contentIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    contentIntent.setType("*/*");
    contentIntent.addCategory(Intent.CATEGORY_OPENABLE);
    try {
      startActivityForResult(contentIntent, REQUEST_FILE_SELECT);
    } catch (ActivityNotFoundException e) {
      Log.e("Failed to start intent", e.getMessage());
    }
  }
}
