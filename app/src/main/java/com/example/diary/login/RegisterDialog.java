package com.example.diary.login;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class RegisterDialog extends DialogFragment {
  private String username, password;
  private OnRegisterListener listener;

  public interface OnRegisterListener {
    public void onRegister(String username, String password);
  }

  RegisterDialog(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    try {
      listener = (OnRegisterListener) context;
    } catch (ClassCastException e) {
      throw new ClassCastException(context.toString() + " must implement OnRegisterListener");
    }
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    builder
        .setMessage("Register a new user from the current credentials?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            listener.onRegister(username, password);
          }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            // User cancelled the dialog
          }
        });
    return builder.create();
  }
}
