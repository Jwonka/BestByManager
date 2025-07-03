package com.example.bestbymanager.data.api;

import com.google.gson.annotations.SerializedName;

public class ProductResponse {
    @SerializedName("code") public String code;
    @SerializedName("status") public int status;
    @SerializedName("product") public ProductData product;
}
