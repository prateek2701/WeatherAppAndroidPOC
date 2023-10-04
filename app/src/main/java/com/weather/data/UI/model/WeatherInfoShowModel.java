package com.weather.data.UI.model;

import com.weather.data.RequestCompleteListener;
import com.weather.data.UI.model.data.City;
import com.weather.data.UI.model.data.CityGeoData;
import com.weather.data.UI.model.data.WeatherInfoResponse;

import java.util.List;

public interface WeatherInfoShowModel {
    void getCityList(RequestCompleteListener<List<City>> callback);
    void getWeatherInfo(String city , RequestCompleteListener<WeatherInfoResponse> callback);

    void getGeoDataInfo(String city,RequestCompleteListener<List<CityGeoData>> callback);


    void getWeatherInfoWithGeoCoordinate(double lat, double lon, RequestCompleteListener<WeatherInfoResponse> callback);
}
