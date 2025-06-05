package com.example.bestbymanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.data.pojo.ExpiredProductReportRow;
import java.util.List;

public class ExpiredProductReportViewModel extends AndroidViewModel {
    private final LiveData<List<ExpiredProductReportRow>> rows;
    public LiveData<List<ExpiredProductReportRow>> getResults() { return rows; }

    public ExpiredProductReportViewModel(@NonNull Application app, @NonNull SavedStateHandle savedState) {
        super(app);

        rows = new Repository(app).searchReport(savedState.get("expirationDate"));
    }
}