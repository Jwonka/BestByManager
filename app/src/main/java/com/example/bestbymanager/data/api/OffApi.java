package com.example.bestbymanager.data.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface OffApi {
    @GET("api/v0/product/{barcode}.json")
    Call<ProductResponse> getByBarcode(@Path("barcode") String barcode);
}
