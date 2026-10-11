package com.styletrack.customer.data;

import com.google.gson.Gson;
import com.styletrack.customer.BuildConfig;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.java8.Java8CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds the Retrofit client. [token] supplies the current login token, [onUnauthorized] fires on a 401. */
public final class ApiClient {
    public static final Gson GSON = new Gson();

    private ApiClient() {}

    public static Api build(Supplier<String> token, Runnable onUnauthorized) {
        Interceptor auth = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();
            String t = token.get();
            if (t != null) builder.header("Authorization", "Bearer " + t);
            Response response = chain.proceed(builder.build());
            String path = original.url().encodedPath();
            // a rejected token on any call except sign-in/registration means the session is over
            if (response.code() == 401 && !path.endsWith("/auth/login") && !path.endsWith("/auth/register")) {
                onUnauthorized.run();
            }
            return response;
        };

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(auth)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            clientBuilder.addInterceptor(logging);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(GSON))
                .addCallAdapterFactory(Java8CallAdapterFactory.create())
                .build();
        return retrofit.create(Api.class);
    }
}
