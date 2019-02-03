package com.rolandvitezhu.todocloud.di.module;

import com.google.gson.GsonBuilder;
import com.rolandvitezhu.todocloud.datasynchronizer.CategoryDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.DataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.ListDataSynchronizer;
import com.rolandvitezhu.todocloud.datasynchronizer.TodoDataSynchronizer;
import com.rolandvitezhu.todocloud.helper.BooleanTypeAdapter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
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
        .build();
  }
}
