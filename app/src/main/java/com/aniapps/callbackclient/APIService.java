package com.aniapps.callbackclient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by NagRaj_Pilla on 4/13/2017.
 * Service call
 */

public interface APIService {
    @FormUrlEncoded
    @POST
    Call<String> getApiResult(@Url String url, @FieldMap Map<String, String> fields, @Header("Access-Key1") String accessToken);


}
