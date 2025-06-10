package com.example.bestbymanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.data.entities.Product;

public class ProductDetailsViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<Product> product;

    public LiveData<Product> getProduct() { return product; }

    public ProductDetailsViewModel(@NonNull Application app, SavedStateHandle handle) {
        super(app);
        repository = new Repository(app);

        LiveData<Integer> idLive = handle.getLiveData("productID", -1);

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

    public void delete(Product product) { repository.deleteProduct(product); }
}