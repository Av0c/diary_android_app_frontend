package com.example.diary.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.OkHttpResponseListener;

import com.example.diary.MainActivity;
import com.example.diary.R;
import com.example.diary.Utils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;

public class LoginActivity extends AppCompatActivity
  implements RegisterDialog.OnRegisterListener {
  private static final String TAG = "MyDebug";
  // Components
  private TextInputEditText usernameLoginInput;
  private TextInputLayout usernameLoginLayout;
  private TextInputEditText passwordLoginInput;
  private TextInputLayout passwordLoginLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    // Bind inputs
    usernameLoginInput = findViewById(R.id.usernameLoginInput);
    usernameLoginLayout = findViewById(R.id.usernameLoginLayout);
    passwordLoginInput = findViewById(R.id.passwordLoginInput);
    passwordLoginLayout = findViewById(R.id.passwordLoginLayout);

    Button buttonLogin = findViewById(R.id.buttonLogin);
    Button buttonRegister = findViewById(R.id.buttonRegister);

    buttonLogin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Validate inputs
        String tryUsername = usernameLoginInput.getText().toString();
        String tryPassword = passwordLoginInput.getText().toString();
        boolean validInputs = true;

        if (tryUsername.equals("")) {
          validInputs = false;
          usernameLoginLayout.setError("Username can not be blank");
        }
        if (tryPassword.equals("")) {
          validInputs = false;
          passwordLoginLayout.setError("Password can not be blank");
        }

        if (validInputs) {
          authorizeUser(tryUsername, tryPassword);
        }
      }
    });

    buttonRegister.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Validate inputs
        String tryUsername = usernameLoginInput.getText().toString();
        String tryPassword = passwordLoginInput.getText().toString();
        boolean validInputs = true;

        if (tryUsername.equals("")) {
          validInputs = false;
          usernameLoginLayout.setError("Username can not be blank");
        }
        if (tryPassword.equals("")) {
          validInputs = false;
          passwordLoginLayout.setError("Password can not be blank");
        }

        if (validInputs) {
          DialogFragment newFragment = new RegisterDialog(
              tryUsername,
              tryPassword
          );

          newFragment.show(getSupportFragmentManager(), "RegisterDialog");
        }
      }
    });

    // Init networking
    AndroidNetworking.initialize(getApplicationContext());
  }

  @Override
  public void onRegister(final String username, final String password) {
    // Log.d(TAG, "onRegister: " + username + ":" + password);

    // Clear previously-set error
    usernameLoginLayout.setError(null);
    passwordLoginLayout.setError(null);

    AndroidNetworking.post(getString(R.string.api_root)+"user")
        .addBodyParameter("username", username)
        .addBodyParameter("password", password)
        .setTag("postUserRegister")
        .setPriority(Priority.MEDIUM)
        .build()
        .getAsOkHttpResponse(new OkHttpResponseListener() {
          @Override
          public void onResponse(Response response) {
            if (response.code() == 200) {
              Utils.setAuthentication(LoginActivity.this, username, password);
              Utils.setName(LoginActivity.this, username);
              login();
            }
            else {
              usernameLoginLayout.setError("Failed to register username");
              passwordLoginLayout.setError("Failed to register password");
            }
          }
          @Override
          public void onError(ANError error) {
            error.printStackTrace();
            usernameLoginLayout.setError("Failed to register username");
            passwordLoginLayout.setError("Failed to register password");
          }
        });
  }

  private void authorizeUser(final String username, final String password) {
    // Clear previously-set error
    usernameLoginLayout.setError(null);
    passwordLoginLayout.setError(null);

    JSONObject postBody = new JSONObject();
    try {
      postBody.put("username", username);
      postBody.put("password", password);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    AndroidNetworking.post(getString(R.string.api_root)+"user/login")
        .addBodyParameter("username", username)
        .addBodyParameter("password", password)
        .setTag("postUserLogin")
        .setPriority(Priority.MEDIUM)
        .build()
        .getAsOkHttpResponse(new OkHttpResponseListener() {
          @Override
          public void onResponse(Response response) {
            if (response.code() == 200) {
              Utils.setAuthentication(LoginActivity.this, username, password);
              Utils.setName(LoginActivity.this, username);
              login();
            }
            else {
              usernameLoginLayout.setError("Failed to authorize username");
              passwordLoginLayout.setError("Failed to authorize password");
            }
          }
          @Override
          public void onError(ANError anError) {
            usernameLoginLayout.setError("Failed to authorize username");
            passwordLoginLayout.setError("Failed to authorize password");
          }
        });
  }

  private void login() {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
  }

}
