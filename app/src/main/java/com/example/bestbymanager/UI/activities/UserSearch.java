package com.example.bestbymanager.UI.activities;

import static com.example.bestbymanager.utilities.LocalDateBinder.bindDateField;
import static com.example.bestbymanager.utilities.LocalDateBinder.format;
import static com.example.bestbymanager.utilities.LocalDateBinder.parseOrToday;
import static com.example.bestbymanager.utilities.LocalDateBinder.stripString;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.BaseAdminActivity;
import com.example.bestbymanager.data.database.ProductDatabaseBuilder;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.data.entities.User;
import com.example.bestbymanager.databinding.ActivityUserSearchBinding;
import com.example.bestbymanager.viewmodel.UserListViewModel;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.time.LocalDate;

public class UserSearch  extends BaseAdminActivity {
    private static final int REQ_CAMERA = 42;
    private Repository repository;
    private User selectedUser;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new Repository(getApplication());
        setTitle(R.string.employee_search);
        ActivityUserSearchBinding binding = ActivityUserSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ProductDatabaseBuilder.getDatabase(this);

        AutoCompleteTextView employeeDropdown = binding.employeeDropdown;
        UserListViewModel userViewModel = new ViewModelProvider(this).get(UserListViewModel.class);

        userViewModel.getUsers().observe(this, users -> {
            ArrayAdapter<User> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, users);
            employeeDropdown.setAdapter(adapter);
        });

        employeeDropdown.setThreshold(0);
        employeeDropdown.setOnClickListener(v -> employeeDropdown.showDropDown());
        employeeDropdown.setOnItemClickListener((parent, view, pos, id) -> selectedUser = (User) parent.getItemAtPosition(pos));

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String code = result.getContents();
                binding.editBarcode.setText(code);
                showUserEnteredProductsForBarcode(code);                  }
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
                    toast("Enter or scan a barcode.");
                } else {
                    showUserEnteredProductsForBarcode(code);
                }
                return true;
            }
            return false;
        });

        bindDateField(binding.startDate, this);
        bindDateField(binding.endDate, this);

        binding.searchButton.setOnClickListener(v -> {
            String startDateString = stripString(binding.startDate);
            String endDateString = stripString(binding.endDate);
            String code = TextUtils.isEmpty(binding.editBarcode.getText()) ? "" : binding.editBarcode.getText().toString().trim();
            boolean hasBarcode   = !code.isEmpty();
            boolean hasDateRange = !startDateString.equals("Start Date") && !endDateString.equals("End Date");

            if (selectedUser == null) {
                toast("Select an employee first.");
                return;
            }

            if (!hasBarcode && !hasDateRange) {
                toast("Enter a barcode OR pick a date range.");
                return;
            }

            if (hasBarcode && hasDateRange) {
                toast("Enter a barcode OR a date range, not both.");
                return;
            }

            if (hasBarcode) {
                showUserEnteredProductsForBarcode(code);
                return;
            }

            if (hasDateRange) {
                LocalDate start = parseOrToday(startDateString);
                LocalDate end = parseOrToday(endDateString);

                if (end.isBefore(start)) {
                    toast("End date must be after the start date.");
                    return;
                }
                showUserEnteredProductsForDateRange(start, end);
            }
        });
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

    private void showUserEnteredProductsForBarcode(String code) {
        repository.getProductsByBarcode(code).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that barcode.");
            } else {
                Intent intent = new Intent(this, ProductReport.class).putExtra("barcode", code);
                startActivity(intent);
            }
        });
    }

    private void showUserEnteredProductsForDateRange(LocalDate start, LocalDate end) {
        repository.getProductsByDateRange(start, end).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that date range.");
            } else {
                Intent intent = new Intent(UserSearch.this, UserReport.class)
                        .putExtra("startDate", format(start))
                        .putExtra("endDate", format(end));
                startActivity(intent);
            }
        });
    }

    private void showExpiredProducts() {
        repository.getExpired(LocalDate.now()).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No such user in the database.");
            } else {
                Intent intent = new Intent(UserSearch.this, UserReport.class)
                        .putExtra("mode", "expired");
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_search, menu);
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
        } else if (item.getItemId() == R.id.employeeDetails) {
            Intent intent = new Intent(this, UserDetails.class);
            startActivity(intent);
            return true;
        }  else if (item.getItemId() == R.id.employeeList) {
            Intent intent = new Intent(this, UserList.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
