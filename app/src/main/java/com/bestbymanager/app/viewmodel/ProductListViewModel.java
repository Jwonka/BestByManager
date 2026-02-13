package com.bestbymanager.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Product;
import java.time.LocalDate;
import java.util.List;

public class ProductListViewModel extends AndroidViewModel {
    private final Repository repository;
    private final MutableLiveData<Integer> refresh = new MutableLiveData<>(0);
    private final LiveData<List<Product>> products;

    public ProductListViewModel(@NonNull Application app) {
        super(app);
        repository = new Repository(app);
        products = Transformations.switchMap(refresh, tick -> repository.getProducts(LocalDate.now()));
    }
    public LiveData<List<Product>> getProducts(LocalDate ignoredToday) { return products; }
    public void refresh() {
        Integer v = refresh.getValue();
        refresh.setValue(v == null ? 1 : v + 1);
    }
}