package com.example.bestbymanager.data.api;

import com.google.gson.annotations.SerializedName;

public class ProductData {
    @SerializedName("product_name") public String productName;
    @SerializedName("brands")       public String brands;
    @SerializedName("quantity")     public String weight;
    @SerializedName("code")     public String barcode;
    @SerializedName("categories")     public String category;
    @SerializedName("image_front_small_url")     public String imageUri;
}
