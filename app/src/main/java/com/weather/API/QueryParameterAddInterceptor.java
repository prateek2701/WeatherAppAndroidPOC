package com.weather.API;

import com.weather.constant.Constants;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class QueryParameterAddInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        HttpUrl url = chain.request().url().newBuilder()
                .addQueryParameter("appid", Constants.MapKey)
                .build();

        Request request = chain.request().newBuilder()
                // .addHeader("Authorization", "Bearer token")
                .url(url)
                .build();

        return chain.proceed(request);
    }
}
