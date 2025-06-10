package com.example.bestbymanager.utilities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class LocalDateBinder {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd/yy").withLocale(Locale.US);

    public static LocalDate parseOrToday(String s) {
        try {
            return LocalDate.parse(s, FMT);
        }
        catch (DateTimeParseException ex) {
            return LocalDate.now();
        }
    }

    public static void bindDateField(TextView field, Context context) {
        bindDateField(field, context, LocalDate.now());
    }

    public static void bindDateField(TextView field, Context context, LocalDate today) {

        field.setOnClickListener(v -> {
            LocalDate seed = parseOrToday(field.getText().toString());

            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (picker, y, m, d) -> {
                        LocalDate picked = LocalDate.of(y, m + 1, d); // month is 0-based
                        field.setText(picked.format(FMT));
                    },
                    seed.getYear(),
                    seed.getMonthValue() - 1,
                    seed.getDayOfMonth()
            );
            dialog .show();
        });
    }

    public static void bindFutureDateField(TextView field, Context context) {
        bindFutureDateField(field, context, LocalDate.now());
    }

    public static void bindFutureDateField(TextView field, Context context, LocalDate today) {

        field.setOnClickListener(v -> {
            LocalDate seed = parseOrToday(field.getText().toString());
            long todayMillis = System.currentTimeMillis();

            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (p, y, m, d) -> {
                        LocalDate picked = LocalDate.of(y, m + 1, d);
                        field.setText(picked.format(FMT));
                    },
                    seed.getYear(),
                    seed.getMonthValue() - 1,
                    seed.getDayOfMonth());

            dialog.getDatePicker().setMinDate(todayMillis);
            dialog.show();
        });
    }
    public static String stripString(TextView toStrip) { return toStrip.getText().toString().trim(); }
}

