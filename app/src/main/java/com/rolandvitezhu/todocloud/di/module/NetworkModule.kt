package com.rolandvitezhu.todocloud.di.module

import com.google.gson.GsonBuilder
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import com.rolandvitezhu.todocloud.helper.BooleanTypeAdapter
import dagger.Module
import dagger.Provides
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

@Module
class NetworkModule {

//  val BASE_URL = "http://192.168.1.100/";  // LAN IP
//  val BASE_URL = "http://192.168.56.1/";  // Genymotion IP
//  val BASE_URL = "http://169.254.50.78/";  // Genymotion IP - Current
//  val BASE_URL = "http://10.0.2.2/";  // AVD IP
//  val BASE_URL = "http://192.168.173.1/";  // ad hoc network IP
    val BASE_URL = "http://todocloud.000webhostapp.com/" // 000webhost IP

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val clientBuilder = OkHttpClient.Builder()

        val loggingInterceptor = HttpLoggingInterceptor()

        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        clientBuilder.addInterceptor(loggingInterceptor)
        clientBuilder.addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            val apiKey = DbLoader.getInstance().apiKey
            val headersBuilder = Headers.Builder()

            // Add Authorization header
            if (apiKey != null) {
                headersBuilder.add("authorization", apiKey)
                // Remove every headers to prevent issues and add new headers only after that
                requestBuilder.headers(headersBuilder.build())
            }
            val request = requestBuilder.build()
            chain.proceed(request)
        }

        val gsonBuilder = GsonBuilder()
                .setLenient()
                .serializeNulls()
                .disableHtmlEscaping()
                .registerTypeAdapter(Boolean::class.javaObjectType, BooleanTypeAdapter())

        return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
    }
}