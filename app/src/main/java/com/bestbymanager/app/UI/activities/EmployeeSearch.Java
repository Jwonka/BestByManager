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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseAdminActivity;
import com.bestbymanager.app.data.database.BestByManagerDatabase;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.databinding.ActivityEmployeeSearchBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.viewmodel.EmployeeListViewModel;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.time.LocalDate;

public class EmployeeSearch extends BaseAdminActivity {
    private static final int REQ_CAMERA = 42;
    private Repository repository;
    private Employee selectedEmployee;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new Repository(getApplication());
        setTitle(R.string.employee_search);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityEmployeeSearchBinding binding = ActivityEmployeeSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final View rootView = binding.getRoot();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });

        BestByManagerDatabase.getDatabase(this);

        AutoCompleteTextView employeeDropdown = binding.employeeDropdown;
        EmployeeListViewModel employeeViewModel = new ViewModelProvider(this).get(EmployeeListViewModel.class);

        employeeViewModel.getEmployees().observe(this, employees -> {
            ArrayAdapter<Employee> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, employees);
            employeeDropdown.setAdapter(adapter);
        });

        employeeDropdown.setThreshold(0);
        employeeDropdown.setOnClickListener(v -> employeeDropdown.showDropDown());
        employeeDropdown.setOnItemClickListener((parent, view, pos, id) -> selectedEmployee = (Employee) parent.getItemAtPosition(pos));

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String rawCode = result.getContents();
                binding.editBarcode.setText(rawCode);
            }
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
            String code = TextUtils.isEmpty(binding.editBarcode.getText()) ? "" : binding.editBarcode.getText().toString().trim();
            boolean hasBarcode   = !code.isEmpty();
            boolean hasDateRange = !startDateString.equals("Start Date") && !endDateString.equals("End Date");
            LocalDate start = startDateString == null ? LocalDate.now() : parseOrToday(startDateString);
            LocalDate end = endDateString == null ? LocalDate.now() : parseOrToday(endDateString);
            LocalDate today = LocalDate.now();

            if (!hasBarcode && !hasDateRange && selectedEmployee == null) {
                showAllEntries(today);
            } else if (hasBarcode && hasDateRange && selectedEmployee != null) {
                showEmployeeEnteredProductsByBarcodeForRange(selectedEmployee.getEmployeeID(), code, start, end, today);
            } else if (hasBarcode && hasDateRange) {
                showProductsByBarcodeForRange(code, start, end, today);
            } else if (hasBarcode && selectedEmployee != null) {
                showEmployeeEnteredProductsForBarcode(selectedEmployee.getEmployeeID(), code, today);
            } else if (hasDateRange && selectedEmployee != null) {
                showEmployeeEnteredProductsForRange(selectedEmployee.getEmployeeID(), start, end, today);
            } else if (hasBarcode) {
                showProductsForBarcode(code, today);
            } else if (hasDateRange) {
                showProductsForRange(start, end, today);
            } else if (selectedEmployee != null) {
                showEmployeeEnteredProducts(selectedEmployee.getEmployeeID(), today);
            }
        });

        binding.employeeButton.setOnClickListener(v -> repository.getEmployees().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that barcode.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeList.class)
                        .putExtra("admin_only", true);
                startActivity(intent);
            }
        }));

        binding.clearButton.setOnClickListener(v -> clearForm(binding));
    }

    private void clearForm(ActivityEmployeeSearchBinding binding) {
        binding.employeeDropdown.setText("");
        selectedEmployee = null;
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

    private void showAllEntries(LocalDate today) {
        Intent intent = new Intent(this, EmployeeReport.class);
        intent.putExtra("today", today.toString());
        intent.putExtra("mode", "allEntries");
        startActivity(intent);
    }

    private void showProductsForBarcode(String barcode, LocalDate today) {
        repository.getEntriesForBarcode(barcode, today).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that barcode.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeReport.class)
                        .putExtra("mode", "barcode")
                        .putExtra("barcode", barcode);
                startActivity(intent);
            }
        });
    }

    private void showProductsForRange(LocalDate start, LocalDate end, LocalDate today) {
        repository.getEntriesByDateRange(start, end, today).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that date range.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeReport.class)
                        .putExtra("mode", "range")
                        .putExtra("startDate", format(start))
                        .putExtra("endDate", format(end));
                startActivity(intent);
            }
        });
    }
    private void showEmployeeEnteredProducts(long employeeID, LocalDate today) {
        repository.getEntriesByEmployee(employeeID, today).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that employee.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeReport.class)
                        .putExtra("mode", "employee")
                        .putExtra("employee", employeeID);
                startActivity(intent);
            }
        });
    }

    private void showEmployeeEnteredProductsForRange(long employeeID, LocalDate start, LocalDate end, LocalDate today) {
        repository.getEntriesForEmployeeInRange(employeeID, start, end, today).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that date range.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeReport.class)
                        .putExtra("mode", "range-employee")
                        .putExtra("employee", employeeID)
                        .putExtra("startDate", format(start))
                        .putExtra("endDate", format(end));
                startActivity(intent);
            }
        });
    }
    private void showEmployeeEnteredProductsForBarcode(long employeeID, String barcode, LocalDate today) {
        repository.getEntriesForEmployeeAndBarcode(employeeID, barcode, today).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that barcode.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeReport.class)
                        .putExtra("mode", "barcode-employee")
                        .putExtra("Employee", employeeID)
                        .putExtra("barcode", barcode);
                startActivity(intent);
            }
        });
    }

    private void showProductsByBarcodeForRange(String barcode, LocalDate start, LocalDate end, LocalDate today) {
        repository.getEntriesByBarcodeForRange(barcode, start, end, today).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that date range.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeReport.class)
                        .putExtra("mode", "barcode-range")
                        .putExtra("barcode", barcode)
                        .putExtra("startDate", format(start))
                        .putExtra("endDate", format(end));
                startActivity(intent);
            }
        });
    }

    private void showEmployeeEnteredProductsByBarcodeForRange(long employeeID, String barcode, LocalDate start, LocalDate end, LocalDate today) {
        repository.getEntriesByBarcodeForEmployeeInRange(employeeID, barcode, start, end, today).observe(this, list -> {
            if (list == null || list.isEmpty()) {
                toast("No products in the database for that date range.");
            } else {
                Intent intent = new Intent(EmployeeSearch.this, EmployeeReport.class)
                        .putExtra("mode", "barcode-range-employee")
                        .putExtra("employee", employeeID)
                        .putExtra("barcode", barcode)
                        .putExtra("startDate", format(start))
                        .putExtra("endDate", format(end));
                startActivity(intent);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_employee_search, menu);
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
        if (item.getItemId() == R.id.employeeDetails) { startActivity(new Intent(this, EmployeeDetails.class)); return true; }
        if (item.getItemId() == R.id.employeeList) { startActivity(new Intent(this, EmployeeList.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
