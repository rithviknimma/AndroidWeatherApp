package com.example.myweatherapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements LocationListener {

    final String API_KEY = "aab0772e943021c0385fbcedca86bda5";

    TextView currentForecasts;
    TextView hourlyForecasts;
    TextView avgForecasts;

    TextView weeklyCast;
    TextView pastInfo;

    Button mButton;
    EditText mEdit;
    TextView pastTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        currentForecasts = findViewById(R.id.today);
        hourlyForecasts = findViewById(R.id.hourly);
        avgForecasts = findViewById(R.id.average);

        weeklyCast = findViewById(R.id.weeklyCast);

        pastInfo = findViewById(R.id.pastInfo);
        pastInfo.setText("Enter past date in YYYY/MM/DD/HH/MM format and press button to get temperature: ");
        mButton = findViewById(R.id.button);
        mEdit = findViewById(R.id.editText);
        pastTemp = findViewById(R.id.pastTemp);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    try {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 6000, this);
                        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    } catch (SecurityException e) {
                        Log.e("exception", e.toString());
                    }
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();
            getDataApi(latitude, longitude);
            getUserSpecifiedTemp(latitude, longitude);
        }
    }

    // gets temperature from user specified time
    public void getUserSpecifiedTemp(final double latitude, final double longitude){
        mButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        Log.v("EditText", mEdit.getText().toString());
                        try{
                            String datePattern = "\\d{4}/\\d{2}/\\d{2}/\\d{2}/\\d{2}";
                            if(mEdit.getText().toString().matches(datePattern)){ // parse and get data only if input matches
                                String[] dateStringArray = mEdit.getText().toString().split("/");
                                String dateString = dateStringArray[0]+"-"+dateStringArray[1]+"-"+dateStringArray[2]+"T"+dateStringArray[3]
                                        +":"+dateStringArray[4]+":00.000-0000";
                                //pastTemp.setText(dateString);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                                Date dt = sdf.parse(dateString);
                                long epoch = dt.getTime()/1000;
                                getFutureDataApi(latitude, longitude, epoch);
                            }
                            else{
                                pastTemp.setText("Wrong input, please try again.");
                            }

                        }catch(ParseException e){
                            Log.e("exception", e.toString());
                        }

                    }
                });
    }

    // gets data from dark sky api based on location
    public void getDataApi(double latitude, double longitude) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.darksky.net/forecast/" + API_KEY + "/" + latitude + "," + longitude;
        Log.d("URL", url);

        JsonObjectRequest weather_request = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        setCurrentData(response);
                        setHourlyData(response);
                        setAvgTemp(response, 48); // average temp for next 48 hours
                        setWeeklyTemp(response);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.e("ERROR", error.toString());

                    }
                });

        queue.add(weather_request);
    }

    // gets and sets data from dark sky api based on location and time
    public void getFutureDataApi(double latitude, double longitude, long unixTime) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.darksky.net/forecast/"+API_KEY+"/"+latitude+","+longitude+","+unixTime;
        Log.d("URL", url);

        JsonObjectRequest weather_request = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject currently = response.getJSONObject("currently");
                            double temperature = currently.getDouble("temperature");
                            pastTemp.setText("Temp: "+temperature);
                        }catch(Exception e){
                            Log.e("JSON PARSE ERROR", e.toString());
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        pastTemp.setText("Error getting data, please try a different date.");
                        Log.e("ERROR", error.toString());
                    }
                });
        queue.add(weather_request);
    }

    // outputs current location, temperature, humidity, wind speed, and precipitation
    public void setCurrentData(JSONObject response) {
        try {
            JSONObject currently = response.getJSONObject("currently");
            double latitude = response.getDouble("latitude");
            double longitude = response.getDouble("longitude");

            double temperature = currently.getDouble("temperature");
            double humidity = currently.getDouble("humidity");
            double windSpeed = currently.getDouble("windSpeed");
            double precipitation = currently.getDouble("precipProbability");

            String current_data = "";
            current_data += "Latitude: " + latitude;
            current_data += "\nLongitude: " + longitude;
            current_data += "\nTemperature: " + temperature;
            current_data += "\nHumidity: " + ((int) (humidity * 100)) + " %";
            current_data += "\nWind Speed: " + windSpeed;
            current_data += "\nPrecipitation: " + precipitation + " %";

            currentForecasts.setText(current_data);

        } catch (Exception e) {
            Log.e("JSON PARSE ERROR", e.toString());
        }
    }

    // outputs temperature for the next 5 hours
    public void setHourlyData(JSONObject response) {
        try {
            JSONObject hourly = response.getJSONObject("hourly");
            JSONArray data = hourly.getJSONArray("data");
            String hourlyForecast = "Hourly Forecast: (new five hours) \n";

            for (int i = 1; i <= 5; i++) {
                double temp = data.getJSONObject(i).getDouble("temperature");
                hourlyForecast += "Hour " + i + ": " + temp + "\n";
            }

            hourlyForecasts.setText(hourlyForecast);
        } catch (Exception e) {
            Log.e("JSON PARSE ERROR", e.toString());
        }
    }

    // outputs average temperature for the next 48 hours
    public void setAvgTemp(JSONObject response, int hours){
        try{
            JSONObject hourly = response.getJSONObject("hourly");
            JSONArray data = hourly.getJSONArray("data");

            double temp =0;
            double avg;

            for(int i=0; i<hours; i++){
                temp += data.getJSONObject(i).getDouble("temperature");
            }

            avg = temp/hours;
            String average = "Average Temp for next 48 hours: "+ avg;
            avgForecasts.setText(average);

        } catch(Exception e){
            Log.e("JSON PARSE ERROR", e.toString());
        }
    }

    // outputs day to day temperatures of upcoming week
    public void setWeeklyTemp(JSONObject response){
        ArrayList<String> dates = new ArrayList<String>();
        Date now = new Date();
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("EEEE"); // the day of the week spelled out completely

        dates.add("Sunday");dates.add("Monday");dates.add("Tuesday");dates.add("Wednesday");dates.add("Thursday");dates.add("Friday");dates.add("Saturday");
        dates.add("Sunday");dates.add("Monday");dates.add("Tuesday");dates.add("Wednesday");dates.add("Thursday");dates.add("Friday");dates.add("Saturday");

        int startDayIndex = dates.indexOf(simpleDateformat.format(now)); // get today's day's index from dates array list
        try {
            JSONObject daily = response.getJSONObject("daily");
            JSONArray data = daily.getJSONArray("data");

            for(int i = 1; i < data.length(); i++){ // get next week data
                double tempHigh = data.getJSONObject(i).getDouble("temperatureHigh");
                double tempLow = data.getJSONObject(i).getDouble("temperatureLow");
                weeklyCast.setText(weeklyCast.getText()+dates.get(startDayIndex + i)+"--> Temp High: "+ tempHigh + " Temp Low: " + tempLow + "\n" );
            }
        }catch(Exception e){
            Log.e("JSON PARSE ERROR", e.toString());
        }
    }


    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

}
