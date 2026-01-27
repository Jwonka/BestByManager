package com.bestbymanager.app.viewmodel;

import static com.bestbymanager.app.utilities.LocalDateBinder.parseOrToday;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.pojo.ProductReportRow;
import java.time.LocalDate;
import java.util.List;

public class ProductReportViewModel extends AndroidViewModel {
    private final LiveData<List<ProductReportRow>> report;
    public LiveData<List<ProductReportRow>> getReport() { return report; }

    public ProductReportViewModel(@NonNull Application app, @NonNull SavedStateHandle savedState) {
        super(app);
        Repository repository = new Repository(app);

        LiveData<List<ProductReportRow>> source;

        String barcode = savedState.get("barcode");
        String mode = savedState.get("mode");
        String startString = savedState.get("startDate");
        String endString = savedState.get("endDate");

        if ("allProducts".equals(mode)) {
            source = repository.getAllProducts();
        } else if ("expired".equals(mode)) {
            source = repository.getExpired(LocalDate.now());
        } else if (barcode != null && !barcode.isEmpty()) {
            if (startString != null && endString != null) {
                LocalDate start = parseOrToday(startString);
                LocalDate end = parseOrToday(endString);
                source = repository.getProductsByBarcodeAndDateRange(barcode, start, end);
            } else {
                source= repository.getReportRowsByBarcode(barcode);
            }
        } else if (startString != null && endString != null) {
                LocalDate start = parseOrToday(startString);
                LocalDate end = parseOrToday(endString);
                source = repository.getExpiring(start, end);
        } else {
            source = repository.getExpiring(LocalDate.now(), LocalDate.now().plusDays(7));
        }
        this.report = source;
    }
}