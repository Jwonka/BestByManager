package com.bestbymanager.app.UI.activities;

import static com.bestbymanager.app.utilities.LocalDateBinder.bindDateField;
import static com.bestbymanager.app.utilities.LocalDateBinder.format;
import static com.bestbymanager.app.utilities.LocalDateBinder.parseOrToday;
import static com.bestbymanager.app.utilities.LocalDateBinder.stripString;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseEmployeeRequiredActivity;
import com.bestbymanager.app.data.database.BestByManagerDatabase;
import com.bestbymanager.app.databinding.ActivityProductSearchBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.utilities.AdminMenu;
import com.journeyapps.barcodescanner.ScanOptions;
import java.time.LocalDate;
import com.journeyapps.barcodescanner.ScanContract;

public class ProductSearch extends BaseEmployeeRequiredActivity {
    private static final int REQ_CAMERA = 42;

    private static final String EXTRA_START_DATE   = "startDate";
    private static final String EXTRA_END_DATE     = "endDate";
    private static final String EXTRA_MODE         = "mode";
    private static final String EXTRA_BARCODE      = "barcode";
    private static final String EXTRA_ALL_PRODUCTS = "allProducts";
    private static final String MODE_ALL_PRODUCTS  = "allProducts";
    private static final String MODE_EXPIRED       = "expired";
    private static final String MODE_EXPIRING      = "expiring";

    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // non-UI init only (safe before gate)
        BestByManagerDatabase.getDatabase(this);
    }

    @Override
    protected void onGatePassed() {
        setTitle(R.string.product_search);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityProductSearchBinding binding = ActivityProductSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final View rootView = binding.getRoot();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) binding.editBarcode.setText(result.getContents());
        });

        binding.barcodeInputLayout.setEndIconOnClickListener(v -> {
            if (ensureCameraPermission()) {
                ScanOptions options = new ScanOptions();
                options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
                options.setPrompt("Align the barcode inside the box");
                options.setBeepEnabled(false);
                options.setOrientationLocked(true);
                barcodeLauncher.launch(options);
            }
        });

        binding.editBarcode.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return true;
            }
            return false;
        });

        bindDateField(binding.startDate, this);
        bindDateField(binding.endDate, this);

        binding.searchButton.setOnClickListener(v -> {
            String startDateString = stripString(binding.startDate);
            String endDateString = stripString(binding.endDate);

            String barcode = TextUtils.isEmpty(binding.editBarcode.getText())
                    ? ""
                    : binding.editBarcode.getText().toString().trim();

            boolean hasBarcode = !barcode.isEmpty();
            boolean hasDateRange =
                    !startDateString.equals(getString(R.string.start_date)) &&
                            !endDateString.equals(getString(R.string.end_date));

            if (!hasBarcode && !hasDateRange) {
                startActivity(new Intent(this, ProductReport.class)
                        .putExtra(EXTRA_MODE, MODE_ALL_PRODUCTS)
                        .putExtra(EXTRA_ALL_PRODUCTS, true));
                return;
            }

            LocalDate start = hasDateRange ? parseOrToday(startDateString) : null;
            LocalDate end   = hasDateRange ? parseOrToday(endDateString) : null;

            if (hasDateRange && end.isBefore(start)) {
                Toast.makeText(this, "End date must be after the start date.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (hasBarcode && !hasDateRange) {
                startActivity(new Intent(this, ProductReport.class)
                        .putExtra(EXTRA_BARCODE, barcode));
                return;
            }

            if (hasBarcode) {
                startActivity(new Intent(this, ProductReport.class)
                        .putExtra(EXTRA_BARCODE, barcode)
                        .putExtra(EXTRA_START_DATE, format(start))
                        .putExtra(EXTRA_END_DATE, format(end)));
                return;
            }

            startActivity(new Intent(this, ProductReport.class)
                    .putExtra(EXTRA_START_DATE, format(start))
                    .putExtra(EXTRA_END_DATE, format(end)));
        });

        binding.expiringSoonButton.setOnClickListener(v -> {
            LocalDate today = LocalDate.now();
            LocalDate sevenDaysLater = today.plusDays(7);
            startActivity(new Intent(this, ProductReport.class)
                    .putExtra(EXTRA_MODE, MODE_EXPIRING)
                    .putExtra(EXTRA_START_DATE, format(today))
                    .putExtra(EXTRA_END_DATE, format(sevenDaysLater)));
        });

        binding.expiredButton.setOnClickListener(v ->
                startActivity(new Intent(this, ProductReport.class)
                        .putExtra(EXTRA_MODE, MODE_EXPIRED)));

        binding.clearButton.setOnClickListener(v -> clearForm(binding));
    }

    private void clearForm(ActivityProductSearchBinding binding) {
        binding.editBarcode.setText("");
        binding.startDate.setText(R.string.start_date);
        binding.endDate.setText(R.string.end_date);
    }

    private boolean ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(
                this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);
        if (req == REQ_CAMERA && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            ScanOptions opts = new ScanOptions();
            opts.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
            opts.setPrompt("Align the barcode inside the box");
            opts.setBeepEnabled(false);
            opts.setOrientationLocked(true);
            barcodeLauncher.launch(opts);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_search, menu);
        AdminMenu.inflateIfAdmin(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean activeIsAdmin = ActiveEmployeeManager.isActiveEmployeeAdmin(this);
        MenuItem adminItem = menu.findItem(R.id.adminPage);

        if (adminItem != null) adminItem.setVisible(activeIsAdmin);
        AdminMenu.setVisibility(this, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { this.finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.employeeDetails) { startActivity(new Intent(this, ProductDetails.class)); return true;  }
        if (item.getItemId() == R.id.productList) { startActivity(new Intent(this, ProductList.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}
