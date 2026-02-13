package com.bestbymanager.app.viewmodel;

import static com.bestbymanager.app.utilities.LocalDateBinder.parseOrToday;
import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.pojo.ProductReportRow;
import java.time.LocalDate;
import java.util.List;

public class ProductReportViewModel extends AndroidViewModel {
    private final SavedStateHandle savedState;
    private final MutableLiveData<Integer> refresh = new MutableLiveData<>(0);
    private final LiveData<List<ProductReportRow>> report;
    private final Repository repository;

    public LiveData<List<ProductReportRow>> getReport() { return report; }

    public ProductReportViewModel(@NonNull Application app, @NonNull SavedStateHandle savedState) {
        super(app);
        this.repository = new Repository(app);
        this.savedState = savedState;
        report = Transformations.switchMap(refresh, tick -> buildSource());
        refresh();
    }

    private LiveData<List<ProductReportRow>> buildSource() {
        String barcode = savedState.get("barcode");
        String mode = savedState.get("mode");
        String startString = savedState.get("startDate");
        String endString = savedState.get("endDate");

        Boolean allProductsFlag = savedState.get("allProducts");
        boolean allProducts = Boolean.TRUE.equals(allProductsFlag);

        if ("allProducts".equals(mode) || allProducts) {
            return repository.getAllProducts();
        } else if ("expired".equals(mode)) {
            return repository.getExpired(LocalDate.now());
        } else if (barcode != null && !barcode.isEmpty()) {
            if (startString != null && endString != null) {
                LocalDate start = parseOrToday(startString);
                LocalDate end = parseOrToday(endString);
                return repository.getProductsByBarcodeAndDateRange(barcode, start, end);
            } else {
                return repository.getReportRowsByBarcode(barcode);
            }
        } else if (startString != null && endString != null) {
            LocalDate start = parseOrToday(startString);
            LocalDate end = parseOrToday(endString);
            return repository.getExpiring(start, end);
        } else {
            return repository.getExpiring(LocalDate.now(), LocalDate.now().plusDays(7));
        }
    }

    public void refresh() {
        Integer v = refresh.getValue();
        refresh.setValue(v == null ? 1 : v + 1);
    }

    public void discardExpiredProduct(long productID, int quantity, @Nullable String reason, @Nullable Long userId) {
        repository.discardExpiredProduct(productID, quantity, reason, userId);
        refresh();
    }
}