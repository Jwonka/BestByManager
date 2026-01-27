package com.bestbymanager.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import com.bestbymanager.app.data.api.ProductResponse;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Product;
import retrofit2.Callback;

public class ProductDetailsViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<Product> product;

    public LiveData<Product> getProduct() { return product; }

    public ProductDetailsViewModel(@NonNull Application app, SavedStateHandle handle) {
        super(app);
        repository = new Repository(app);

        LiveData<Long> idLive = handle.getLiveData("productID", -1L);

        product = Transformations.switchMap(idLive, id -> {
            if (id == null || id <= 0) {
                return new MutableLiveData<>(null);
            } else {
                return repository.getProduct(id);
            }
        });
    }

    public void save(Product product) {
        if (product.getProductID() == 0) {
            repository.insertProduct(product);
        } else {
            repository.updateProduct(product);
        }
    }
    public Product getRecentExpiringProduct(String code) { return repository.getRecentExpirationByBarcode(code); }
    public void delete(Product product) { repository.deleteProduct(product); }
    public void fetchProduct(String barcode, Callback<ProductResponse> cb) { repository.fetchProduct(barcode, cb); }
    public void discardExpiredProduct(long productID, int quantity, @Nullable String reason, @Nullable Long userId) {
        repository.discardExpiredProduct(productID, quantity, reason, userId);
    }
}