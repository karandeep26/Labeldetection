package com.karan.labeldetection.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by stpl on 3/23/2017.
 */

public class ApiClient {
    private static final String BASE_URL = "https://vision.googleapis.com/";
    private static Retrofit retrofit = null;


    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client;
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            Gson gson = new GsonBuilder().setLenient().create();
            client = builder.build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(client)
                    .build();

        }
        return retrofit;
    }
}