package com.example.sander.myweatherapp;


import android.app.Fragment;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class WeatherFragment extends Fragment {

    private static ArrayAdapter<String> weatherAdapter;
    

    public WeatherFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.weatherfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("Lviv");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.weather_fragment, container, false);
        String[] data = {
                "Натисніть Refresh"
        };
        List<String> weekForecast = new ArrayList<String>(Arrays.asList(data));
        weatherAdapter =
                new ArrayAdapter<String>(
                        getActivity(),
                        R.layout.list_item_forecast,
                        R.id.list_item_forecast_textview,
                        weekForecast);


// Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(weatherAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                     @Override
             public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                 String forecast = weatherAdapter.getItem(position);
                Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                 }
            });
        
        return rootView;
        
    }

    public  class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

       private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private String getReadableDateString(long time){
        // converting the date into proper state

            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        //prepearing degree value

        private String formatHighLows(double high, double low) {
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);
            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        //pulling out data from JSON string

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {
        //names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
            // using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;
                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                //Converting date/time into human-readable interface
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);
                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }
       @Override
        protected String[] doInBackground(String... params) {

           if (params.length == 0) {
                return null;
                }


                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;


                String forecastJsonStr = null;

           String format = "json";
           String units = "metric";
           int numDays = 7;


                //Receiving of JSON bu URL-qerry for city "Lviv"
                try {


                    final String FORECAST_BASE_URL =
                             "http://api.openweathermap.org/data/2.5/forecast/daily?";
                    final String QUERY_PARAM = "q";
                    final String FORMAT_PARAM = "mode";
                    final String UNITS_PARAM = "units";
                    final String DAYS_PARAM = "cnt";

                            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                             .appendQueryParameter(QUERY_PARAM, params[0])
                             .appendQueryParameter(FORMAT_PARAM, format)
                             .appendQueryParameter(UNITS_PARAM, units)
                             .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                             .build();

                             URL url = new URL(builtUri.toString());




                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();
                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        return null;
                    }


                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        //new line for debuging
                        buffer.append(line + "\n");
                    }
                    if (buffer.length() == 0) {
                        //empty stream, no parcing
                        return null;
                    }
                    forecastJsonStr = buffer.toString();



                } catch (IOException e) {
                    Log.e(LOG_TAG, e.getMessage(), e);
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
           try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
                } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
                }
                //error when forecast parceing failed
           return null;


        }
        @Override
         protected void onPostExecute(String[] result) {
             if (result != null) {
                weatherAdapter.clear();
                 for(String dayForecastStr : result) {
                     weatherAdapter.add(dayForecastStr);
                     }
                 //getting new data from server
                         }
             }
    }

}
