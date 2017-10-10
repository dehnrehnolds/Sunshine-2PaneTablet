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

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import static android.os.Build.ID;

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private final Context mContext;

    public FetchWeatherTask(Context context) {
        mContext = context;
    }

    private boolean DEBUG = true;

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param locationSetting The location string used to request updates from the server.
     * @param cityName A human-readable city name, e.g "Mountain View"
     * @param lat the latitude of the city
     * @param lon the longitude of the city
     * @return the row ID of the added location.
     */
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        long locationID;
        Cursor cLocation = mContext.getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null
        );

        //Wenn die Stadt in der datenbank gefunden wird...
        if (cLocation.moveToFirst())
            //..gebe deren ID zurück (später)
            locationID = cLocation.getLong(cLocation.getColumnIndex(WeatherContract.LocationEntry._ID));
        //wenn die Stadt noch nicht in der DB existiert...
        else {
            //..baue die hinzuzufügenden ContentValues zusammen, ...
            ContentValues contentValues = new ContentValues();
            contentValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,locationSetting);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME,cityName);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT,lat);
            contentValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG,lon);
            //...füge dies der CONTENT_URI der LocationEntries hinzu und hole die ID der ContentUri
            // (is naemlich eine, wie im Weather Contract festgelegt!!)
            Uri locationUri = mContext.getContentResolver().insert(WeatherContract.LocationEntry.CONTENT_URI, contentValues);
            locationID = ContentUris.parseId(locationUri);
        }
        //curser zu und locationID der hinzugefügten/bereits vorhandenen Location zurückgeben
        cLocation.close();
        return locationID;
    }


    @Override
    protected String[] doInBackground(String... params) {

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (params.length == 0) {
            return null;
        }
        String locationQuery = params[0];

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            final String APPID_PARAM = "APPID";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, "80d7b352bec7eca39230f1aac6338241")
                    .build();

            Log.d(LOG_TAG,builtUri.toString() );
            URL url = new URL(builtUri.toString());
            Log.d(LOG_TAG, "URL built from URI" );

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            Log.d(LOG_TAG, "HTTPUrlConnection opened");
            urlConnection.setRequestMethod("GET");
            Log.d(LOG_TAG, "requestMethod set to GET" );
            urlConnection.connect();
            Log.d(LOG_TAG,"Connected" );

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            Log.d(LOG_TAG,"Input Stream set");
            StringBuffer buffer = new StringBuffer();
            Log.d(LOG_TAG, "Buffer initiated" );
            if (inputStream == null) {
                // Nothing to do.
                Log.w(LOG_TAG, "Input stream is null" );
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            Log.d(LOG_TAG,"reader set" );

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }
            Log.d(LOG_TAG,buffer.toString());

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

//        try {
//            return getWeatherDataFromJson(forecastJsonStr, locationQuery);
//        } catch (JSONException e) {
//            Log.e(LOG_TAG, e.getMessage(), e);
//            e.printStackTrace();
//        }
//        // This will only happen if there was an error getting or parsing the forecast.
        return null;
    }

    @Override
    protected void onPostExecute(String[] result) {
            }
            // New data is back from the server.  Hooray!
        }