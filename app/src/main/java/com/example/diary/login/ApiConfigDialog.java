package com.example.diary.login;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.diary.R;
import com.example.diary.Utils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ApiConfigDialog extends DialogFragment {
  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    // Use the Builder class for convenient dialog construction
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    // Inflate the control view
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.api_config_layout, null);

    // Set current API value
    TextInputEditText currentApiInputReadonly = view.findViewById(R.id.current_api_input);
    currentApiInputReadonly.setText(Utils.getApiRoot(getContext()));
    currentApiInputReadonly.setFocusableInTouchMode(false);
    currentApiInputReadonly.clearFocus();

    // Input
    final TextInputEditText apiNewInput = view.findViewById(R.id.new_api_input);
    MaterialButton resetButton = view.findViewById(R.id.api_default_button);

    // Reset to default
    resetButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        apiNewInput.setText(getString(R.string.default_api_root));
      }
    });

    builder
        .setView(view)
        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            String newApi = apiNewInput.getText().toString();
            if (newApi.isEmpty()) {
              return;
            }
            if (!newApi.endsWith("/")) {
              newApi += "/";
            }
            Utils.setApiRoot(getContext(), newApi);
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
