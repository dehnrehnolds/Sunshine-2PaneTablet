package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

import org.w3c.dom.Text;

import static android.R.attr.data;
import static android.R.attr.format;
import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_TEXT;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;

/**
 * Created by Robert on 06.10.2017.
 */

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private String mForecast;
    private ShareActionProvider mShareActionProvider;
    private final int MY_LOADER_ID = 1;
    private String shareMassege = " #SunshineApp";
    private String LOG_TAG = DetailFragment.class.getSimpleName();

    private static final String[] DETAIL_COLUMNS = {
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            WeatherEntry.COLUMN_HUMIDITY,
            WeatherEntry.COLUMN_WIND_SPEED,
            WeatherEntry.COLUMN_DEGREES,
            WeatherEntry.COLUMN_PRESSURE,
            WeatherEntry.COLUMN_WEATHER_ID
    };

    // projection changes go hand in hand with column index changes
    private static final int COL_WEATHER_ID = 0;
    private static final int COL_WEATHER_DATE = 1;
    private static final int COL_WEATHER_DESC = 2;
    private static final int COL_WEATHER_MAX_TEMP = 3;
    private static final int COL_WEATHER_MIN_TEMP = 4;
    private static final int COL_WEATHER_HUMIDITY = 5;
    private static final int COL_WEATHER_WIND_SPEED = 6;
    private static final int COL_WEATHER_DEGREES = 7;
    private static final int COL_WEATHER_PRESSURE = 8;
    private static final int COL_WEATHER_CONDITION_ID = 9;

    public ImageView mIconView;
    public TextView mDateView;
    public TextView mDescriptionView;
    public TextView mHighTempView;
    public TextView mLowTempView;
    public TextView mHumidityView;
    public TextView mWindView;
    public TextView mPressureView;


    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    static final String DETAIL_URI = "URI";
    private Uri mUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
            }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        //getting all necessary views to fill with data from the cursor in onLoadFinished()
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon_imageview);
        mDateView = (TextView) rootView.findViewById(R.id.detail_day_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_temp_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_temp_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);

        return rootView;

    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflator) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflator.inflate(R.menu.detailfragment, menu);
        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        if (mForecast != null){
            mShareActionProvider.setShareIntent(createShareIntent());
            Log.d(LOG_TAG,"shareActionProvider erstellt");
        }
    }

    public Intent createShareIntent(){
        Intent shareIntent = new Intent(ACTION_SEND);
        shareIntent.addFlags(FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("plain/text");
        shareIntent.putExtra(EXTRA_TEXT, mForecast + shareMassege);
        return shareIntent;

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MY_LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
        if (null != mUri) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (!data.moveToFirst()) { return; }

        int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);
        //es wird im detail view immer das Art Bild gezeigt
        int iconId = Utility.getWeatherConditionImage(weatherId, false, getContext());
        mIconView.setImageResource(iconId);


        String dateString = Utility.getFriendlyDayString(getContext(),
                data.getLong(COL_WEATHER_DATE));
        mDateView.setText(dateString);

        String weatherDescription =
                data.getString(COL_WEATHER_DESC);
        mDescriptionView.setText(weatherDescription);

        boolean isMetric = Utility.isMetric(getActivity());

        String high = Utility.formatTemperature(getContext(),
                data.getDouble(COL_WEATHER_MAX_TEMP), isMetric);
        mHighTempView.setText(high);

        String low = Utility.formatTemperature(getContext(),
                data.getDouble(COL_WEATHER_MIN_TEMP), isMetric);
        mLowTempView.setText(low);

        Float pressure = data.getFloat(COL_WEATHER_PRESSURE);
        mPressureView.setText(String.format(getContext().getString(R.string.format_pressure),
                pressure));

        Float windSpeed = data.getFloat(COL_WEATHER_WIND_SPEED);
        Float degree = data.getFloat(COL_WEATHER_DEGREES);
        mWindView.setText(Utility.getFormattedWind(getContext(),windSpeed,degree));

        Float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
        Log.d("HUMIDITY", "Humidity: " + Float.toString(humidity));
        mHumidityView.setText(String.format(getContext().getString(R.string.format_humidity),
                humidity));

        mForecast = String.format("%s - %s - %s/%s", dateString, weatherDescription, high, low);

//        TextView detailTextView = (TextView)getView().findViewById(R.id.detail_text);
//        detailTextView.setText(mForecast);

        // If onCreateOptionsMenu has already happened, we need to update the share intent now.
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }

    void onLocationChanged( String newLocation ) {
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(MY_LOADER_ID, null, this);
        }
    }
}
