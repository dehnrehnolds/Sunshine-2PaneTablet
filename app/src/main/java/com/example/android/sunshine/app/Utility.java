/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;
import android.preference.PreferenceManager;
import android.text.format.Time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {
    public static String getPreferredLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_location_key),
                context.getString(R.string.pref_location_default));
    }

    public static boolean isMetric(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_metric))
                .equals(context.getString(R.string.pref_units_metric));
    }

    static String formatTemperature(Context context, double temperature, boolean isMetric) {
        double temp;
        if ( !isMetric ) {
            temp = 9*temperature/5+32;
        } else {
            temp = temperature;
        }
        return context.getString(R.string.format_temperature,temp);
    }

    static String formatDate(long dateInMillis) {
        Date date = new Date(dateInMillis);
        return DateFormat.getDateInstance().format(date);
    }
    // Format used for storing dates in the database.  ALso used for converting those strings
    // back into date objects for comparison/processing.
    public static final String DATE_FORMAT = "yyyyMMdd";

    /**
     * Helper method to convert the database representation of the date into something to display
     * to users.  As classy and polished a user experience as "20140102" is, we can do better.
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return a user-friendly representation of the date.
     */
    public static String getFriendlyDayString(Context context, long dateInMillis) {
        // The day string for forecast uses the following logic:
        // For today: "Today, June 8"
        // For tomorrow:  "Tomorrow"
        // For the next 5 days: "Wednesday" (just the day name)
        // For all days after that: "Mon Jun 8"

        Calendar calendar = Calendar.getInstance();
        int currentJulianDay = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTimeInMillis(dateInMillis);
        int julianDay = calendar.get(Calendar.DAY_OF_YEAR);

        // If the date we're building the String for is today's date, the format
        // is "Today, June 24"
        if (julianDay == currentJulianDay) {
            String today = context.getString(R.string.today);
            int formatId = R.string.format_full_friendly_date;
            return String.format(context.getString(
                    formatId),
                    today,
                    getFormattedMonthDay(context, dateInMillis));
        } else if ( julianDay < currentJulianDay + 7 ) {
            // If the input date is less than a week in the future, just return the day name.
            return getDayName(context, dateInMillis);
        } else {
            // Otherwise, use the form "Mon Jun 3"
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(dateInMillis);
        }
    }

    /**
     * Given a day, returns just the name to use for that day.
     * E.g "today", "tomorrow", "wednesday".
     *
     * @param context Context to use for resource localization
     * @param dateInMillis The date in milliseconds
     * @return
     */
    public static String getDayName(Context context, long dateInMillis) {
        // If the date is today, return the localized version of "Today" instead of the actual
        // day name.

        Calendar calendar = Calendar.getInstance();
        int currentJulianDay = calendar.get(Calendar.DAY_OF_YEAR);
        calendar.setTimeInMillis(dateInMillis);
        int julianDay = calendar.get(Calendar.DAY_OF_YEAR);
        if (julianDay == currentJulianDay) {
            return context.getString(R.string.today);
        } else if ( julianDay == currentJulianDay +1 ) {
            return context.getString(R.string.tomorrow);
        } else {
            calendar = Calendar.getInstance();
            currentJulianDay = calendar.get(Calendar.DAY_OF_YEAR);
            // Otherwise, the format is just the day of the week (e.g "Wednesday".
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
            return dayFormat.format(dateInMillis);
        }
    }

    /**
     * Converts db date format to the format "Month day", e.g "June 24".
     * @param context Context to use for resource localization
     * @param dateInMillis The db formatted date string, expected to be of the form specified
     *                in Utility.DATE_FORMAT
     * @return The day in the form of a string formatted "December 6"
     */
    public static String getFormattedMonthDay(Context context, long dateInMillis ) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateInMillis);
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utility.DATE_FORMAT);
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("MMMM dd");
        String monthDayString = monthDayFormat.format(dateInMillis);
        return monthDayString;
    }
    public static String getFormattedWind(Context context, float windSpeed, float degrees) {
        int windFormat;
        if (Utility.isMetric(context)) {
            windFormat = R.string.format_wind_kmh;
            windSpeed = windSpeed * 3.6f;
        } else {
            windFormat = R.string.format_wind_mph;
            windSpeed = 3600.0f * windSpeed / 1609.0f;
        }

        // From wind direction in degrees, determine compass direction as a string (e.g NW)
        // You know what's fun, writing really long if/else statements with tons of possible
        // conditions.  Seriously, try it!
        final String[] directionsText = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
        final int DEGREES_TOTAL = 360;
        final int DIR_TOTAL = 8;

        String direction = directionsText[Math.round(degrees / (DEGREES_TOTAL / DIR_TOTAL)) % DIR_TOTAL];

        return String.format(context.getString(windFormat), windSpeed, direction);
    }

    /**
     * Helper method to provide the icon or art resource id according to the weather condition id
     * returned by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon or art, or ic_launcher if no relation is found.
     */
    public static int getWeatherConditionImage(int weatherId, boolean isIcon, Context context) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        String resourceName = "ic_";
        if (!isIcon) {
            resourceName = "art_";
        }
        if (weatherId >= 200 && weatherId <= 232) {
            resourceName += "storm";
        } else if (weatherId >= 300 && weatherId <= 321) {
            resourceName += "light_rain";
        } else if (weatherId >= 500 && weatherId <= 504) {
            resourceName += "rain";
        } else if (weatherId == 511) {
            resourceName += "snow";
        } else if (weatherId >= 520 && weatherId <= 531) {
            resourceName += "rain";
        } else if (weatherId >= 600 && weatherId <= 622) {
            resourceName += "snow";
        } else if (weatherId >= 701 && weatherId <= 761) {
            resourceName += "fog";
        } else if (weatherId == 761 || weatherId == 781) {
            resourceName += "storm";
        } else if (weatherId == 800) {
            resourceName += "clear";
        } else if (weatherId == 801) {
            resourceName += "light_clouds";
        } else if (weatherId >= 802 && weatherId <= 804) {
            resourceName += "clouds";
        } else {
            resourceName = "ic_launcher";
        }

        return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
    }

}