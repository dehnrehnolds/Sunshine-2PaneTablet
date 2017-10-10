package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;


/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(Context context, double high, double low) {
        boolean isMetric = Utility.isMetric(mContext);
        String highLowStr = Utility.formatTemperature(context, high, isMetric) + "/" + Utility.formatTemperature(context, low, isMetric);
        return highLowStr;
    }

    /*
        This is ported from FetchWeatherTask --- but now we go straight from the cursor to the
        string.
     */
    private String convertCursorRowToUXFormat(Context context, Cursor cursor) {

        String highAndLow = formatHighLows(
                context,
                cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP),
                cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));

        return Utility.formatDate(cursor.getLong(ForecastFragment.COL_WEATHER_DATE)) +
                " - " + cursor.getString(ForecastFragment.COL_WEATHER_DESC) +
                " - " + highAndLow;
    }


    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    /**
     * Copy/paste note: Replace existing newView() method in ForecastAdapter with this one.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        if (viewType==0) layoutId = R.layout.list_item_forecast_today;
        else layoutId = R.layout.list_item_forecast;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return view;
    }

//    /*
//        Remember that these views are reused as needed.
//     */
//    @Override
//    public View newView(Context context, Cursor cursor, ViewGroup parent) {
//        View view = LayoutInflater.from(context).inflate(R.layout.list_item_forecast, parent, false);
//
//        return view;
//    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
//    @Override
//    public void bindView(View view, Context context, Cursor cursor) {
//        // our view is pretty simple here --- just a text view
//        // we'll keep the UI functional with a simple (and slow!) binding.
//
//        TextView tv = (TextView)view;
//        tv.setText(convertCursorRowToUXFormat(cursor));
//    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.
        ViewHolder viewHolder = new ViewHolder(view);
        // Read weather icon ID from cursor
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        //wenn der cursor an position 0 sitzt, dann ist ein Art gefordert, ansosnten ein Icon
        boolean isIcon;
        if (getItemViewType(cursor.getPosition()) == 0) isIcon = false;
        else isIcon = true;
        int iconId = Utility.getWeatherConditionImage(weatherId, isIcon, context);
        viewHolder.iconView.setImageResource(iconId);

        // TODO Read date from cursor
        long weatherDate = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        String formatedDate = Utility.getFriendlyDayString(context,weatherDate);
        viewHolder.dayView.setText(formatedDate);

        // TODO Read weather forecast from cursor
        String weatherForecast = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(weatherForecast);

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(context);

        // Read high temperature from cursor
        double high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(context, high, isMetric));

        // TODO Read low temperature from cursor
        double low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.lowTempView.setText(Utility.formatTemperature(context, low, isMetric));

    }
    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dayView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dayView = (TextView) view.findViewById(R.id.list_item_day_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }
}