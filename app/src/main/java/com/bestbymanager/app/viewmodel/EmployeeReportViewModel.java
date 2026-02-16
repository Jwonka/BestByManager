package com.bestbymanager.app.viewmodel;

import static com.bestbymanager.app.utilities.LocalDateBinder.parseOrToday;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.pojo.EmployeeReportRow;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class EmployeeReportViewModel extends AndroidViewModel {
    private final LiveData<List<EmployeeReportRow>> report;
    public LiveData<List<EmployeeReportRow>> getReport() { return report; }

    public EmployeeReportViewModel(@NonNull Application app, @NonNull SavedStateHandle savedState) {
        super(app);
        Repository repository = new Repository(app);

        LiveData<List<EmployeeReportRow>> source;

        String mode = savedState.get("mode");
        if (mode == null) mode = "";
        String barcode = savedState.get("barcode");
        if (barcode == null) barcode = "";
        String start = savedState.get("startDate");
        String end = savedState.get("endDate");
        Long employeeIdLong = savedState.get("employee");
        long employeeID = employeeIdLong != null ? employeeIdLong : -1;
        LocalDate from = start == null ? LocalDate.now() : parseOrToday(start);
        LocalDate to = end == null ? LocalDate.now() : parseOrToday(end);
        LocalDate today = LocalDate.now();

        switch (mode) {
            case "allEntries":
                source = repository.getAllEntries(today);
                break;
            case "employee":
                source = repository.getEntriesByEmployee(employeeID, today);
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
            case "barcode-employee":
                source = repository.getEntriesForEmployeeAndBarcode(employeeID, barcode, today);
                break;
            case "range-employee":
                source = repository.getEntriesForEmployeeInRange(employeeID, from, to, today);
                break;
            case "barcode-range-employee":
                source = repository.getEntriesByBarcodeForEmployeeInRange(employeeID, barcode, from, to, today);
                break;
            default:
                source = new MutableLiveData<>(Collections.emptyList());
                break;
        }
        this.report = source;
    }
}