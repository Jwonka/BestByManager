package com.bestbymanager.app.UI.activities;

import static com.bestbymanager.app.utilities.PasswordUtil.generateTempPassword;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseAdminActivity;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.databinding.ActivityEmployeeDetailsBinding;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.viewmodel.EmployeeDetailsViewModel;

public class EmployeeDetails extends BaseAdminActivity {
    private EmployeeDetailsViewModel employeeViewModel;
    private EditText name;
    private Button password;
    private Employee currentEmployee;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);

        // BaseAdminActivity should enforce admin; this is a hard-stop safety check.
        Session.get().preload(this);
        if (!Session.get().isUnlocked() || !Session.get().isAdmin()) {
            Toast.makeText(this, "Owner admin required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setTitle(R.string.employee_details);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityEmployeeDetailsBinding binding = ActivityEmployeeDetailsBinding.inflate(getLayoutInflater());
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

        name = binding.editEmployeeName;
        Button saveButton = binding.saveEmployeeButton;
        Button clearButton = binding.clearEmployeeButton;
        password = binding.generateTempPwd;

        clearButton.setOnClickListener(v -> clearForm());
        if (saveButton != null) { saveButton.setOnClickListener(v -> saveEmployee()); }

        password.setOnClickListener(v -> {
            if (currentEmployee == null) {
                Toast.makeText(this, "Save employee first", Toast.LENGTH_SHORT).show();
                return;
            }
            employeeViewModel.resetPassword(currentEmployee.getEmployeeID())
                    .observe(this, temp -> {
                        if (temp != null) {
                            showPasswordResetDialog(temp, currentEmployee);
                        } else {
                            Toast.makeText(this, "Could not generate password.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        employeeViewModel = new ViewModelProvider(
                this,
                new SavedStateViewModelFactory(getApplication(), this)
        ).get(EmployeeDetailsViewModel.class);

        employeeViewModel.getEmployee().observe(this, employee -> {
            if (employee == null) return;
            currentEmployee = employee;
            populateForm(employee);
        });

        password.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_employee_details, menu);
        AdminMenu.inflateKioskActions(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem adminItem = menu.findItem(R.id.adminPage);
        if (adminItem != null) adminItem.setVisible(ActiveEmployeeManager.isActiveEmployeeAdmin(this));
        AdminMenu.setVisibility(this, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) return true;
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.employeeSearch) { startActivity(new Intent(this, EmployeeSearch.class)); return true; }
        if (item.getItemId() == R.id.employeeList) { startActivity(new Intent(this, EmployeeList.class)); return true; }

        if (item.getItemId() == R.id.deleteEmployee) {
            if (currentEmployee != null) {
                employeeViewModel.delete(currentEmployee);
                clearForm();
                Toast.makeText(this, "Employee deleted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error deleting employee.", Toast.LENGTH_SHORT).show();
            }
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populateForm(Employee employee) {
        name.setText(employee.getEmployeeName());
        password.setEnabled(true);
        invalidateOptionsMenu();
    }

    private void clearForm() {
        name.setText("");
        currentEmployee = null;
        password.setEnabled(false);
        invalidateOptionsMenu();
    }

    private void saveEmployee() {
        boolean isNew = currentEmployee == null;

        if (!validForm()) return;

        Employee employee = isNew ? new Employee() : currentEmployee;
        employee.setEmployeeName(name.getText().toString().trim());
        employee.setAdmin(false);

        if (isNew) {
            String temp = generateTempPassword();
            employeeViewModel.addEmployee(employee, temp)
                    .observe(this, newEmployee -> {
                        if (newEmployee != null) {
                            currentEmployee = newEmployee;
                            populateForm(currentEmployee);
                            Toast.makeText(this, currentEmployee.getEmployeeName() + " saved.", Toast.LENGTH_SHORT).show();
                            showPasswordResetDialog(temp, currentEmployee);
                        } else {
                            Toast.makeText(this, "Name already taken.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            employeeViewModel.update(employee);
            Toast.makeText(this, employee.getEmployeeName() + " saved.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validForm() {
        String nameTxt = name.getText().toString().trim();
        if (nameTxt.isEmpty()) {
            name.requestFocus();
            Toast.makeText(this, "Please enter a name.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (nameTxt.length() > 64) {
            name.requestFocus();
            Toast.makeText(this, "Name must be less than 64 characters.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showPasswordResetDialog(@NonNull String temp, @Nullable Employee employee) {
        final String message = (employee == null)
                ? getString(R.string.temp_pwd_dialog, temp)
                : getString(R.string.temp_pwd_body, employee.getEmployeeName(), temp);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(R.string.temp_password_created)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.copying, (d, w) -> {
                    ClipboardManager cm = getSystemService(ClipboardManager.class);
                    cm.setPrimaryClip(ClipData.newPlainText("temp-password", temp));
                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.share, (d, w) -> {
                    Intent share = buildShareIntent(message);
                    startActivity(Intent.createChooser(share, getString(R.string.share_with)));
                });

        b.setNeutralButtonIcon(AppCompatResources.getDrawable(this, R.drawable.ic_content_copy));
        b.setNegativeButtonIcon(AppCompatResources.getDrawable(this, R.drawable.ic_share));
        b.show();
    }

    private Intent buildShareIntent(String body) {
        String subject = getString(R.string.temp_pwd_subject);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, body);
        share.putExtra(Intent.EXTRA_SUBJECT, subject);
        share.putExtra("sms_body", body);
        return share;
    }
}
