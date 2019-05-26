package com.rolandvitezhu.todocloud.di.module;

import android.support.annotation.NonNull;

import com.google.gson.GsonBuilder;
import com.rolandvitezhu.todocloud.datastorage.DbLoader;
import com.rolandvitezhu.todocloud.datasynchronizer.CategoryDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.ListDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.TodoDataSynchronizer;
import com.rolandvitezhu.todocloud.helper.BooleanTypeAdapter;

import java.io.IOException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@Module
public class NetworkModule {

//  private static final String BASE_URL = "http://192.168.1.100/";  // LAN IP
//  private static final String BASE_URL = "http://192.168.56.1/";  // Genymotion IP
//  private static final String BASE_URL = "http://169.254.50.78/";  // Genymotion IP - Current
//  private static final String BASE_URL = "http://10.0.2.2/";  // AVD IP
//  private static final String BASE_URL = "http://192.168.173.1/";  // ad hoc network IP
  private static final String BASE_URL = "http://todocloud.000webhostapp.com/";  // 000webhost IP

  @Provides
  @Singleton
  TodoDataSynchronizer provideTodoDataSynchronizer() {
    return new TodoDataSynchronizer();
  }

  @Provides
  @Singleton
  ListDataSynchronizer provideListDataSynchronizer() {
    return new ListDataSynchronizer();
  }

  @Provides
  @Singleton
  CategoryDataSynchronizer provideCategoryDataSynchronizer() {
    return new CategoryDataSynchronizer();
  }

  @Provides
  @Singleton
  DataSynchronizer provideDataSynchronizer() {
    return new DataSynchronizer();
  }

  @Provides
  @Singleton
  Retrofit provideRetrofit() {
    OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();

    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

    clientBuilder.addInterceptor(loggingInterceptor);
    clientBuilder.addInterceptor(new Interceptor() {

      @NonNull
      @Override
      public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();

        Request.Builder requestBuilder = original.newBuilder();

        String apiKey = DbLoader.getInstance().getApiKey();

        Headers.Builder headersBuilder = new Headers.Builder();

        // Add Authorization header
        if (apiKey != null) {
          headersBuilder.add("authorization", apiKey);
          // Remove every headers to prevent issues and add new headers only after that
          requestBuilder.headers(headersBuilder.build());
        }

        Request request = requestBuilder.build();

        return chain.proceed(request);
      }

    });

    GsonBuilder gsonBuilder = new GsonBuilder()
        .setLenient()
        .serializeNulls()
        .disableHtmlEscaping()
        .registerTypeAdapter(Boolean.class, new BooleanTypeAdapter());

    return new Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(clientBuilder.build())
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build();
  }
}
