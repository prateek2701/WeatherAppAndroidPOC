package com.weather.data.UI;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.weather.data.PreferenceHelper;
import com.weather.data.R;
import com.weather.data.UI.model.WeatherInfoShowModel;
import com.weather.data.UI.model.WeatherInfoShowModelImpl;
import com.weather.data.UI.model.data.City;
import com.weather.data.UI.model.data.CityGeoData;
import com.weather.data.UI.model.data.WeatherData;
import com.weather.data.UI.viewmodel.WeatherViewModel;
import com.weather.data.databinding.ActivityMainBinding;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private WeatherViewModel viewModel;
    private WeatherInfoShowModel model;
    private double latitude = 0.0;
    private double longitude = 0.0;

    private PreferenceHelper preferenceHelper;
    ActivityMainBinding binding;

    // Spinner spinner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        viewModel = ViewModelProviders.of(this).get(WeatherViewModel.class);
        model = new WeatherInfoShowModelImpl(getApplicationContext());
        preferenceHelper = PreferenceHelper.getPreferences(this);

        setLiveDataListeners();
        setViewClickListener();
        if (!TextUtils.isEmpty(preferenceHelper.getLLat()) && !TextUtils.isEmpty(preferenceHelper.getLon())) {
            setLastSelectedCity();
            viewModel.getWeatherInfoWithGeoData(Double.parseDouble(preferenceHelper.getLLat())
                    , Double.parseDouble(preferenceHelper.getLon()), model);
        }
        if (!checkPermission()) {
            requestPermission();
            Toast.makeText(this, getString(R.string.please_request_permission), Toast.LENGTH_SHORT).show();
        }

    }

    private void setViewClickListener() {
        binding.btnViewWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cityName = binding.cityValueET.getText().toString();
                if (cityName.trim().equals("") || cityName == "") {
                    binding.layoutWeatherBasic.setVisibility(View.GONE);
                    binding.layoutWeatherAdditional.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Character length should be 3 atleast", Toast.LENGTH_SHORT).show();
                } else {
                    viewModel.getGeoLocationData(cityName, model);
                }
            }
        });

    }

    //
    private void setLastSelectedCity() {
        //setting last search city name
        String city = preferenceHelper.getLastCity();
        if (city.trim().equals("")) {
        } else {
            binding.cityValueET.setText(city.toString());
        }
    }

    private void setLiveDataListeners() {

        /**
         * When ViewModel PUSH city list to LiveData then this `onChanged()`‍ method will be called.
         * Here we subscribe the LiveData of City list. We don't pull city list from ViewModel.
         * We subscribe to the data source for city list. When LiveData of city list is updated
         * inside ViewModel, below onChanged() method will triggered instantly.
         * City list is fetching from a small local JSON file. So we don't need any ProgressBar here.
         *
         * For better understanding, I didn't use lambda in this method call. Rather thant lambda I
         * implement `Observer` interface in general format. Hope you will understand the inline
         * implementation of `Observer` interface. Rest of the `observe()` method, I've used lambda
         * to short the code.
         */

        /**
         * ProgressBar visibility will be handled by this LiveData. ViewModel decides when Activity
         * should show ProgressBar and when hide.
         *
         * Here I've used lambda expression to implement Observer interface in second parameter.
         */

        viewModel.progressBarLiveData.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isShowLoader) {
                if (isShowLoader)
                    binding.progressBar.setVisibility(View.VISIBLE);
                else
                    binding.progressBar.setVisibility(View.GONE);
            }
        });

        /**
         * This method will be triggered when ViewModel successfully receive WeatherData from our
         * data source (I mean Model). Activity just observing (subscribing) this LiveData for showing
         * weather information on UI. ViewModel receives Weather data API response from Model via
         * Callback method of Model. Then ViewModel apply some business logic and manipulate data.
         * Finally ViewModel PUSH WeatherData to `weatherInfoLiveData`. After PUSHING into it, below
         * method triggered instantly! Then we set the data on UI.
         *
         * Here I've used lambda expression to implement Observer interface in second parameter.
         */

        viewModel.weatherInfoLiveData.observe(this, new Observer<WeatherData>() {
            @Override
            public void onChanged(WeatherData weatherData) {
                setWeatherInfo(weatherData);
            }
        });

        /**
         * If ViewModel faces any error during Weather Info fetching API call by Model, then PUSH the
         * error message into `weatherInfoFailureLiveData`. After that, this method will be triggered.
         * Then we will hide the output view and show error message on UI.
         *
         * Here I've used lambda expression to implement Observer interface in second parameter.
         */
        viewModel.weatherInfoFailureLiveData.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                binding.outputGroup.setVisibility(View.GONE);
                binding.tvErrorMessage.setVisibility(View.VISIBLE);
                binding.tvErrorMessage.setText(s);
            }
        });


        Observer<List<CityGeoData>> observer = new Observer<List<CityGeoData>>() {
            @Override
            public void onChanged(List<CityGeoData> geoData) {
                try {
                    Log.v("lat", "" + geoData.get(0).getLat());

                    latitude = geoData.get(0).getLat();
                    longitude = geoData.get(0).getLon();

                    binding.layoutWeatherBasic.setVisibility(View.VISIBLE);
                    binding.layoutWeatherAdditional.setVisibility(View.VISIBLE);

                    String selectedCity = binding.cityValueET.getText().toString();
                    preferenceHelper.setLastCity(selectedCity);
                    preferenceHelper.setLat(latitude);
                    preferenceHelper.setLon(longitude);

                    viewModel.getWeatherInfoWithGeoData(latitude, longitude, model);
                } catch (Exception e) {
                    preferenceHelper.setLastCity("No Data");
                    preferenceHelper.setLat(0.0);
                    preferenceHelper.setLon(0.0);
                    binding.layoutWeatherBasic.setVisibility(View.GONE);
                    binding.layoutWeatherAdditional.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "No Data Found", Toast.LENGTH_SHORT).show();
                }

            }
        };
        viewModel.geoCoderLiveData.observe(this, observer);
    }

    //
    private int getIndex(List<City> list, String city_name) {
        int index = 0;
        if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getName().equals(city_name)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public static String convertKelvinToCelsius(String kelvin) {
        return String.format("%.2f", (float) (Float.parseFloat(kelvin) - 273.15));
    }

    private void setWeatherInfo(WeatherData weatherData) {
        binding.outputGroup.setVisibility(View.VISIBLE);
        binding.tvErrorMessage.setVisibility(View.GONE);
        binding.tvDateTime.setText(unixTimestampToDateTimeString(weatherData.getDateTime()));
        binding.tvTemperature.setText(convertKelvinToCelsius(weatherData.getTemperature()));
        binding.tvCityCountry.setText(weatherData.getCityAndCountry());
        Glide.with(this).load(weatherData.getWeatherConditionIconUrl()).into(binding.ivWeatherCondition);
        binding.tvWeatherCondition.setText(weatherData.getWeatherConditionIconDescription());
        binding.tvHumidityValue.setText(weatherData.getHumidity());
        binding.tvPressureValue.setText(weatherData.getPressure());
        binding.tvSunriseTime.setText(unixTimestampToDateTimeString(weatherData.getSunrise()));
        binding.tvSunsetTime.setText(unixTimestampToDateTimeString(weatherData.getSunset()));
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);

        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted) {
                        setLiveDataListeners();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                                showMessageOKCancel(getString(R.string.need_to_allow_Access),
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{ACCESS_FINE_LOCATION},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        });
                                return;
                            }
                        }

                    }
                }


                break;
        }
    }

    String unixTimestampToDateTimeString(int time) {

        try {
            Instant instant = null;
            String formattedInstant = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DateTimeFormatter formatter = null;
                instant = Instant.ofEpochSecond(time);
                formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy- hh:mm a")
                        .withZone(ZoneId.systemDefault());
                formattedInstant = formatter.format(instant);
            }

            return formattedInstant;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return String.valueOf(time);
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), okListener)
                .setNegativeButton(getString(R.string.cancel), null)
                .create()
                .show();
    }
}