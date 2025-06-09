package com.aniapps.db;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.aniapps.locationtracker.AppLocator;


/**
 * Created by NagRaj_Pilla on 9/16/2017.
 * all shared Preference values
 */

public class Pref {
    private static Pref uniqInstance;
    private static SharedPreferences pref;
    public static SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    public static Pref getIn() {
        if (uniqInstance == null) {
            uniqInstance = new Pref();
            pref = PreferenceManager.getDefaultSharedPreferences(AppLocator.appctx);
        }
        editor = pref.edit();
        return uniqInstance;
    }



    public void setService(String service) {
        editor.putString("service", service);
        editor.apply();
    }
    public String getService() {
        return pref.getString("service", "yes");
    }

    public void setName(String name) {
        editor.putString("name", name);
        editor.apply();
    }
    public String getName() {
        return pref.getString("name", "");
    }



    public int getSequence() {
        return pref.getInt("sequence", 0);
    }

    public void setSequence(int sequence) {
        editor.putInt("sequence", sequence);
        editor.apply();
    }

    public int getExpiry() {
        return pref.getInt("expiry", 360);

    }

    public void setExpiry(int expiry) {
        editor.putInt("expiry", expiry);
        editor.apply();
    }

    public int getFrequency() {
        return pref.getInt("frequency", 1);
    }

    public void setFrequency(int frequency) {
        editor.putInt("frequency", frequency);
        editor.apply();
    }




    public boolean isService_running() {
        return pref.getBoolean("service_running",false);
    }

    public void setService_running(boolean service_running) {
        editor.putBoolean("service_running", service_running);
        editor.apply();
    }



    public String getApp_code() {
        return pref.getString("app_code", "");
    }

    public void setApp_code(String app_code) {
        editor.putString("app_code", app_code);
        editor.apply();
    }


    public String getLatitude() {
        return pref.getString("latitude","");
    }

    public void setLatitude(String latitude) {
        editor.putString("latitude", latitude);
        editor.apply();
    }

    public String getLongitude() {
        return pref.getString("longitude","");
    }

    public void setLongitude(String longitude) {
        editor.putString("longitude", longitude);
        editor.apply();
    }


    public String getTimestamp() {
        return pref.getString("timestamp","");
    }

    public void setTimestamp(String timestamp) {
        editor.putString("timestamp", timestamp);
        editor.apply();
    }

    public String getDeviceDetails() {
        return pref.getString("devicedetails","");
    }

    public void setDeviceDetails(String devicedetails) {
        editor.putString("devicedetails", devicedetails);
        editor.apply();
    }
}
