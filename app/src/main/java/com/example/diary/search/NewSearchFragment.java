package com.example.diary.search;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.diary.R;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;

import static com.example.diary.search.SearchFragment.DATE_TIME_FORMAT;

public class NewSearchFragment extends DialogFragment {
  private static final int DATE_LOWER_PICKER_REQUEST = 321000;
  private static final int DATE_UPPER_PICKER_REQUEST = 323000;

  // Search criteria
  // private String phrase, ipAddress;
  // private double latLower, latUpper, longLower, longUpper;
  private Date dateLower = null;
  private Date dateUpper = null;

  // Inputs
  private TextInputEditText phraseInput, ipInput,
      latLowerInput, latUpperInput, longLowerInput, longUpperInput, dateLowerInput, dateUpperInput;

  private AlertDialog newSearchDialog;

  NewSearchFragment() {

  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    // Use the Builder class for convenient dialog construction
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

    // Inflate the control view
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View view = inflater.inflate(R.layout.new_search_layout, null);

    // Bind inputs
    phraseInput = view.findViewById(R.id.phrase_input);
    ipInput = view.findViewById(R.id.ip_input);
    latLowerInput = view.findViewById(R.id.lat_lower_input);
    latUpperInput = view.findViewById(R.id.lat_upper_input);
    longLowerInput = view.findViewById(R.id.long_lower_input);
    longUpperInput = view.findViewById(R.id.long_upper_input);

    builder
        .setView(view)
        .setPositiveButton("Search", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            // (Re)fetch data
            Intent intent = new Intent(getActivity(), SearchFragment.class);

            HashMap<String, String> search = new HashMap<>();

            SimpleDateFormat formatter = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH);

            search.put("phrase", phraseInput.getText().toString());
            search.put("ip_address", ipInput.getText().toString());
            search.put("lat_lower", latLowerInput.getText().toString());
            search.put("lat_upper", latUpperInput.getText().toString());
            search.put("long_lower", longLowerInput.getText().toString());
            search.put("long_upper", longUpperInput.getText().toString());
            search.put("time_lower", dateLower != null ? formatter.format(dateLower) : "");
            search.put("time_upper", dateUpper != null ? formatter.format(dateUpper) : "");
            intent.putExtra("search", search);
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
          }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            // User cancelled the dialog
          }
        });
    newSearchDialog = builder.create();

    dateLowerInput = view.findViewById(R.id.date_lower_input);
    dateUpperInput = view.findViewById(R.id.date_upper_input);

    final FragmentManager fm = getParentFragmentManager();
    dateLowerInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean b) {
        if (!b) return;

        //Initialize your Date however you like it.
        Calendar calendar = Calendar.getInstance();
        if (dateLower != null) {
          calendar.setTime(dateLower);
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Initialize DatePicker
        DialogFragment newFragment = new DatePickerFragment(year, month, day);
        newFragment.setTargetFragment(NewSearchFragment.this, DATE_LOWER_PICKER_REQUEST);
        newFragment.show(fm, "datePicker");
      }
    });

    dateUpperInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View view, boolean b) {
        if (!b) return;

        //Initialize your Date however you like it.
        Calendar calendar = Calendar.getInstance();
        if (dateUpper != null) {
          calendar.setTime(dateUpper);
        }
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Initialize DatePicker
        DialogFragment newFragment = new DatePickerFragment(year, month, day);
        newFragment.setTargetFragment(NewSearchFragment.this, DATE_UPPER_PICKER_REQUEST);
        newFragment.show(fm, "datePicker");
      }
    });

    return newSearchDialog;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Bundle bundle = data.getExtras();
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    if (resultCode == Activity.RESULT_OK) {
      switch (requestCode) {
        case DATE_LOWER_PICKER_REQUEST:
          // Set date
          int yearLower = bundle.getInt("year");
          int monthLower = bundle.getInt("month");
          int dayLower = bundle.getInt("day");

          Calendar calendarLower = new GregorianCalendar();
          calendarLower.set(yearLower, monthLower, dayLower);
          dateLower = calendarLower.getTime();

          dateLowerInput.setText(formatter.format(dateLower));
          break;

        case DATE_UPPER_PICKER_REQUEST:
          /// Set date
          int yearUpper = bundle.getInt("year");
          int monthUpper = bundle.getInt("month");
          int dayUpper = bundle.getInt("day");

          Calendar calendarUpper = new GregorianCalendar();
          calendarUpper.set(yearUpper, monthUpper, dayUpper);
          dateUpper = calendarUpper.getTime();

          dateUpperInput.setText(formatter.format(dateUpper));
          break;
      }
    }
  }

  public static class DatePickerFragment extends DialogFragment
      implements DatePickerDialog.OnDateSetListener {
    private int year;
    private int month;
    private int day;

    DatePickerFragment(int year, int month, int day) {
      this.year = year;
      this.month = month;
      this.day = day;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      // Create a new instance of DatePickerDialog and return it
      return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
      // Do something with the date chosen by the user
      Intent intent = new Intent(getActivity(), SearchFragment.class);
      intent.putExtra("year", year);
      intent.putExtra("month", month);
      intent.putExtra("day", day);
      getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
    }
  }
}
