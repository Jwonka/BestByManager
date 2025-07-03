package com.example.bestbymanager.utilities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

public final class LocalDateBinder {
    private static final DateTimeFormatter FMT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('/')
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter(Locale.US);

    public static String format(LocalDate date) {
        return date.format(FMT);
    }

    public static LocalDate parseOrToday(String s) {
        try {
            return LocalDate.parse(s, FMT);
        }
        catch (DateTimeParseException ex) {
            return LocalDate.now();
        }
    }

    public static void bindDateField(TextView field, Context context) {

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

