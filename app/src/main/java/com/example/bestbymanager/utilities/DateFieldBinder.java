package com.example.bestbymanager.utilities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateFieldBinder {
    public static final String DATE_FORMAT = "MM/dd/yy";

    private static SimpleDateFormat newFormatter() { return new SimpleDateFormat(DATE_FORMAT, Locale.US); }

    public static void bind(TextView field, Context context) { bind(field, context, newFormatter().format(new Date())); }

    // Initialize the Expiration date fields and hook up a DatePicker
    public static void bind(TextView field, Context context, String dateString) {
        Date selected = parseTheDate(dateString, context);
        field.setText(newFormatter().format(selected));
        attachExpirationListener(field, context, selected);
    }

    // Attach a DatePickerDialog to search for dates in the present or past
    public static void attachSearchListener(TextView field, Context context, Date selected) {
        field.setOnClickListener(null);
        field.setOnClickListener(v -> {
            Calendar searchCalendar = Calendar.getInstance();
            searchCalendar.setTime(selected);
            DatePickerDialog searchDialog = new DatePickerDialog(
                    context,
                    (picker, year,month,day) -> {
                        searchCalendar.set(year,month,day);
                        field.setText(newFormatter().format(searchCalendar.getTime()));
                    },
                    searchCalendar.get(Calendar.YEAR),
                    searchCalendar.get(Calendar.MONTH),
                    searchCalendar.get(Calendar.DAY_OF_MONTH)
            );
            searchDialog.show();
        });
    }

    // Attach a DatePickerDialog that seeds from the current field value and prevents from choosing past dates
    public static void attachExpirationListener(TextView field, Context context, Date selected) {
        field.setOnClickListener(null);
        field.setOnClickListener(v -> {
            Calendar expirationCalendar = Calendar.getInstance();
            expirationCalendar.setTime(selected);
            DatePickerDialog expirationDialog = new DatePickerDialog(
                    context,
                    (picker, year,month,day) -> {
                        expirationCalendar.set(year,month,day);
                        field.setText(newFormatter().format(expirationCalendar.getTime()));
                    },
                    expirationCalendar.get(Calendar.YEAR),
                    expirationCalendar.get(Calendar.MONTH),
                    expirationCalendar.get(Calendar.DAY_OF_MONTH)
            );
            expirationDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            expirationDialog.show();
        });
    }

    public static Date parseTheDate(String validDate, Context context) {
        try {
            Date parse = newFormatter().parse(validDate);
            return (parse != null) ? parse : new Date();
        } catch (ParseException e) {
            Toast.makeText(context, "Invalid date! Defaulting to today.", Toast.LENGTH_SHORT).show();
            return new Date();
        }
    }

    public static String stripString(TextView toStrip) { return toStrip.getText().toString().trim(); }
}

