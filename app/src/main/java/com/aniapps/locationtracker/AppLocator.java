package com.aniapps.locationtracker;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.StrictMode;

import androidx.multidex.MultiDexApplication;


/**
 * AppLocator, does the following tasks:
 * 1)Initialization
 */

public class AppLocator extends MultiDexApplication {
    @SuppressLint("StaticFieldLeak")
    public static Context appctx;
    public static final String PRIMARY_CHANNEL = "default";

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate() {
        super.onCreate();
        appctx = getApplicationContext();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }


}




