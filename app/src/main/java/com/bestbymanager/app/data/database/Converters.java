package com.bestbymanager.app.data.database;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.Nullable;
import androidx.room.TypeConverter;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class Converters {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @TypeConverter
    @Nullable
    public static String fromOffsetDateTime(@Nullable OffsetDateTime odt) { return odt == null ? null : ISO.format(odt); }

    @TypeConverter
    @Nullable
    public static OffsetDateTime toOffsetDateTime(@Nullable String value) { return value == null ? null : OffsetDateTime.parse(value, ISO); }

    @TypeConverter
    public static byte[] fromBitmap(Bitmap bmp) {
        if (bmp == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
        return out.toByteArray();
    }
    @TypeConverter
    public static Bitmap toBitmap(byte[] bytes) {return (bytes == null) ? null : BitmapFactory.decodeByteArray(bytes, 0, bytes.length); }

    @TypeConverter
    public static Long fromLocalDate(LocalDate date) { return date == null ? null : date.toEpochDay(); }

    @TypeConverter
    public static LocalDate toLocalDate(Long epochDay) { return epochDay == null ? null : LocalDate.ofEpochDay(epochDay); }
}