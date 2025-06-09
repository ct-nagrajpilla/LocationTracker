package com.aniapps.callbackclient;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.BuildConfig;

import com.aniapps.db.Pref;
import com.aniapps.locationtracker.R;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;



/*
 * Created by NagRaj_Pilla on 4/12/2017.
 * REST STFC Service
 */

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static RetrofitClient uniqInstance;
    private boolean flagNoCrypt;
    private APIService apiService;
  //  ProgressDialog progress;
    private static String TAG = "##Retrofit##";
    String from = "";

    public static RetrofitClient getInstance() {
        if (uniqInstance == null) {
            uniqInstance = new RetrofitClient();
        }
        return uniqInstance;
    }

    private static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(90, TimeUnit.SECONDS)
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(context.getResources().getString(R.string.base))
                    .addConverterFactory(new RetrofitConverter())
                    .client(okHttpClient)
                    .build();

        }
        return retrofit;
    }


    public void doBackProcess(final Context context, String from, Map<String, String> postParams,
                              APIResponse api_res) {
        this.from = from;
       // progress = new ProgressDialog(context);
       /* if (null != context) {
            try {
                runOnUiThread(() -> {
                    progress.setTitle("Loading...");
                    progress.setMessage("Please Wait while loading...");
                    progress.setCancelable(false);
                    progress.show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
        getNoCryptCoreRes(context, from, postParams, api_res);

    }


    //NoCryptCore
    @SuppressLint("HardwareIds")
    private void getNoCryptCoreRes(final Context context, final String from,
                                   final Map<String, String> postParams,
                                   final APIResponse api_res) {
        apiService = RetrofitClient.getClient(context).create(APIService.class);
        postParams.put("version_code", "" + BuildConfig.VERSION_CODE);
        postParams.put("device_id", "" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        postParams.put("app_code", "" + Pref.getIn().getApp_code());
        Log.e("#####","Post: "+postParams);
        apiService.getApiResult(context.getResources().getString(R.string.host_name), postParams,"nee-yabba-orey").enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, final Response<String> res) {
                if (res.isSuccessful()) {

                    try {
                        if (null != res.body() && !res.body().isEmpty()) {
                            api_res.onSuccess(res.body().trim());
                        } else {
                            Toast.makeText(context, "Oops! Somthing went wrong!!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Oops! Somthing went wrong!!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "Oops! Somthing went wrong!!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                retrofit = null;
                Toast.makeText(context, "Oops! Connection Failurettt" + t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }


}
