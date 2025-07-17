package com.example.bestbymanager.viewmodel;

import static com.example.bestbymanager.utilities.LocalDateBinder.parseOrToday;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.data.pojo.UserReportRow;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class UserReportViewModel extends AndroidViewModel {
    private final LiveData<List<UserReportRow>> report;
    public LiveData<List<UserReportRow>> getReport() { return report; }

    public UserReportViewModel(@NonNull Application app, @NonNull SavedStateHandle savedState) {
        super(app);
        Repository repository = new Repository(app);

        LiveData<List<UserReportRow>> source;

        String mode = savedState.get("mode");
        if (mode == null) mode = "";
        String barcode = savedState.get("barcode");
        if (barcode == null) barcode = "";
        String start = savedState.get("startDate");
        String end = savedState.get("endDate");
        Long userIdLong = savedState.get("user");
        long userID = userIdLong != null ? userIdLong : -1;
        LocalDate from = start == null ? LocalDate.now() : parseOrToday(start);
        LocalDate to = end == null ? LocalDate.now() : parseOrToday(end);
        LocalDate today = LocalDate.now();

        switch (mode) {
            case "allEntries":
                source = repository.getAllEntries(today);
                break;
            case "user":
                source = repository.getEntriesByEmployee(userID, today);
                break;
            case "barcode":
                source = repository.getEntriesForBarcode(barcode, today);
                break;
            case "range":
                source = repository.getEntriesByDateRange(from, to, today);
                break;
            case "barcode-range":
                source = repository.getEntriesByBarcodeForRange(barcode, from, to, today);
                break;
            case "barcode-user":
                source = repository.getEntriesForEmployeeAndBarcode(userID, barcode, today);
                break;
            case "range-user":
                source = repository.getEntriesForEmployeeInRange(userID, from, to, today);
                break;
            case "barcode-range-user":
                source = repository.getEntriesByBarcodeForEmployeeInRange(userID, barcode, from, to, today);
                break;
            default:
                source = new MutableLiveData<>(Collections.emptyList());
                break;
        }
        this.report = source;
    }
}