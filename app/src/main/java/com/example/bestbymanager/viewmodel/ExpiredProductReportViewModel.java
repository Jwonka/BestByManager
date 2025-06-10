package com.example.bestbymanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.pojo.ExpiredProductReportRow;
import java.time.LocalDate;
import java.util.List;

public class ExpiredProductReportViewModel extends AndroidViewModel {
    private final Repository repository;
    private final LiveData<List<Product>> expiringSoon;
    public LiveData<List<Product>> getExpiringSoon() { return expiringSoon; }
    private final LiveData<List<ExpiredProductReportRow>> rows;
    public LiveData<List<ExpiredProductReportRow>> getResults() { return rows; }

    public ExpiredProductReportViewModel(@NonNull Application app, @NonNull SavedStateHandle savedState) {
        super(app);
        repository = new Repository(app);
        LocalDate from = savedState.get("expirationDate");
        rows = repository.searchReport(from);
        expiringSoon = repository.getExpiringSoon(LocalDate.now(), LocalDate.now().plusDays(7));
    }
    public LiveData<List<Product>> loadExpiringSoon(int days) {
        return repository.getExpiringSoon(LocalDate.now(), LocalDate.now().plusDays(days));
    }
}