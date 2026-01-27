package com.bestbymanager.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Product;
import java.time.LocalDate;
import java.util.List;

public class ProductListViewModel extends AndroidViewModel {
    private final LiveData<List<Product>> products;

    public ProductListViewModel(@NonNull Application app) {
        super(app);
        Repository repository = new Repository(app);
        this.products = repository.getProducts(LocalDate.now());
    }
    public LiveData<List<Product>> getProducts(LocalDate ignoredToday) { return products; }
}