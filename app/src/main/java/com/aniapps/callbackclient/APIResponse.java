package com.aniapps.callbackclient;

/**
 * Created by NagRaj_Pilla on 4/15/2017.
 * final response from apis
 */

public interface APIResponse {

    void onSuccess(String res);

    void onFailure(String res);
}
