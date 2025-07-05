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
import com.example.bestbymanager.utilities.BarcodeUtil;
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

        String barcode = savedState.get("barcode");
        if (barcode != null) {
            try {
                barcode = BarcodeUtil.toCanonical(barcode);
                //source = repository.getReportRowsByBarcode(barcode);
            } catch (IllegalArgumentException ex) {
                source = new MutableLiveData<>(Collections.emptyList());
            }
        } else {
            String mode = savedState.get("mode");
            String startString = savedState.get("startDate");
            String endString = savedState.get("endDate");

            if ("expired".equals(mode)) {
                //source = repository.getExpired(LocalDate.now());
            } else if (startString != null && endString != null) {
                LocalDate start = parseOrToday(startString);
                LocalDate end = parseOrToday(endString);
                //source = repository.getExpiring(start, end);
            } else {
                //source = repository.getExpiring(LocalDate.now(), LocalDate.now().plusDays(7));
            }
        }
        source = new MutableLiveData<>(Collections.emptyList());
        this.report = source;
    }
}