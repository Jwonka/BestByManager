package com.example.bestbymanager.utilities;

import java.util.Calendar;
import java.util.Date;

public class DateUtility {

    // Strip off time so date-only comparisons can be made
    public static Date cleanDate(Date dayToClear) {
        Calendar day = Calendar.getInstance();
        day.setTime(dayToClear);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day.getTime();
    }

    // Return true if the given date is today or later, ignoring time
    public static boolean isOnOrAfterToday(Date today) {
        Date one = cleanDate(today);
        Date two = cleanDate(new Date());
        return !one.before(two);
    }

    // Return true if the given date is before today, ignoring time
    public static boolean isBeforeToday(Date yesterday) {
        Date one = cleanDate(yesterday);
        Date two = cleanDate(new Date());
        return one.before(two);
    }
}