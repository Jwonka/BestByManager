package com.example.bestbymanager.UI.activities;

import static com.example.bestbymanager.utilities.LocalDateBinder.bindDateField;
import static com.example.bestbymanager.utilities.LocalDateBinder.format;
import static com.example.bestbymanager.utilities.LocalDateBinder.parseOrToday;
import static com.example.bestbymanager.utilities.LocalDateBinder.stripString;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.data.database.ProductDatabaseBuilder;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.databinding.ActivityReportSelectionBinding;
import com.journeyapps.barcodescanner.ScanOptions;
import java.time.LocalDate;
import com.journeyapps.barcodescanner.ScanContract;

public class ProductSearch extends AppCompatActivity {
    private static final int REQ_CAMERA = 42;
    private Repository repository;
    private ActivityReportSelectionBinding binding;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new Repository(getApplication());
        setTitle(R.string.product_search);
        binding = ActivityReportSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String code = result.getContents();
                binding.editBarcode.setText(code);
                showProductsForBarcode(code);                  }
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
                String code = view.getText().toString().trim();
                if (code.isEmpty()) {
                    Toast.makeText(this, "Enter or scan a barcode.", Toast.LENGTH_SHORT).show();
                } else {
                    showProductsForBarcode(code);
                }
                return true;
            }
            return false;
        });

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        ProductDatabaseBuilder.getDatabase(this);

        bindDateField(binding.startDate, this);
        bindDateField(binding.endDate, this);

        binding.searchButton.setOnClickListener(v -> {
            String startDateString = stripString(binding.startDate);
            String endDateString = stripString(binding.endDate);
            String code = TextUtils.isEmpty(binding.editBarcode.getText()) ? "" : binding.editBarcode.getText().toString().trim();
            boolean hasBarcode   = !code.isEmpty();
            boolean hasDateRange = !startDateString.equals("Start Date") && !endDateString.equals("End Date");

            if (!hasBarcode && !hasDateRange) {
                Toast.makeText(this, "Enter a barcode OR pick a date range.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (hasBarcode && hasDateRange) {
                Toast.makeText(this, "Enter a barcode OR a date range, not both.", Toast.LENGTH_LONG).show();
                return;
            }

            if (hasBarcode) {
                showProductsForBarcode(code);
                return;
            }

            if (hasDateRange) {
                LocalDate start = parseOrToday(startDateString);
                LocalDate end = parseOrToday(endDateString);

                if (end.isBefore(start)) {
                    Toast.makeText(this, "End date must be after the start date.", Toast.LENGTH_LONG).show();
                    return;
                }
                showProductsForDateRange(start, end);
            }
        });

        binding.expiringSoonButton.setOnClickListener(v -> {
            LocalDate today = LocalDate.now();
            LocalDate sevenDaysLater = today.plusDays(7);
            showProductsForDateRange(today, sevenDaysLater);
        });

        binding.expiredButton.setOnClickListener(v -> showExpiredProducts());
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

    private void showProductsForBarcode(String code) {
        repository.getProductsByBarcode(code).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                Toast.makeText(this,
                        "No products in the database for that barcode.", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(this, ProductReport.class).putExtra("barcode", code);
                startActivity(intent);
            }
        });
    }

    private void showProductsForDateRange(LocalDate start, LocalDate end) {
        repository.getProductsByDateRange(start, end).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                Toast.makeText(this,
                        "No products in the database for that date range.", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(ProductSearch.this, ProductReport.class)
                        .putExtra("startDate", format(start))
                        .putExtra("endDate", format(end));
                startActivity(intent);
            }
        });
    }

    private void showExpiredProducts() {
        repository.getExpired(LocalDate.now()).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                Toast.makeText(this,
                        "No expired products in the database.", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(ProductSearch.this, ProductReport.class)
                        .putExtra("mode", "expired");
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_search, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isAdmin = Session.get().currentUserIsAdmin();
        menu.findItem(R.id.adminPage).setVisible(isAdmin);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        } else if (item.getItemId() == R.id.mainScreen) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.productList) {
            Intent intent = new Intent(this, ProductList.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.productDetails) {
            Intent intent = new Intent(this, ProductDetails.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.adminPage) {
            Intent intent = new Intent(this, AdministratorActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
