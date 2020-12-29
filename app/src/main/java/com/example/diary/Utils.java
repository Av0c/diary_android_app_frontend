package com.example.diary;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.Base64;

// Helper class used to save/restore application state
public class Utils {
  static final String KEY_AUTHENTICATION = "authentication";
  static final String KEY_NAME = "name";

  public static void setAuthentication(Context context, String username, String password) {
    String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    // Log.d("MyDebug", "setAuthentication: " + encoded);
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(KEY_AUTHENTICATION, encoded)
        .apply();
  }

  public static String getAuthentication(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getString(KEY_AUTHENTICATION, "");
  }

  public static void setName(Context context, String name) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(KEY_NAME, name)
        .apply();
  }

  public static String getName(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getString(KEY_NAME, "");
  }
}
