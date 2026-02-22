package com.bestbymanager.app.UI.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseAdminActivity;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.databinding.ActivityAdministratorBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.utilities.AppResetUtil;
import com.bestbymanager.app.session.DeviceOwnerManager;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class AdministratorActivity extends BaseAdminActivity {
    private static final String SECURITY_PREFS = "security_prefs";
    private static final String KEY_RECOVERY_ENABLED = "recovery_enabled";
    private static final String KEY_RECOVERY_OWNER_ID = "recovery_owner_id";
    private Repository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.admin);
        
        repo = new Repository(getApplication());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityAdministratorBinding binding = ActivityAdministratorBinding.inflate(getLayoutInflater());
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

        binding.employeeListButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, EmployeeList.class);
            startActivity(intent);
        });

        binding.employeeDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, EmployeeDetails.class);
            startActivity(intent);
        });

        binding.employeeSearchButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, EmployeeSearch.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_administrator_activity, menu);
        AdminMenu.inflateKioskActions(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem adminItem = menu.findItem(R.id.adminPage);
        if (adminItem != null) adminItem.setVisible(ActiveEmployeeManager.isActiveEmployeeAdmin(this));
        AdminMenu.setVisibility(this, menu);
        boolean isOwner = DeviceOwnerManager.isActiveEmployeeOwner(this);

        MenuItem recovery = menu.findItem(R.id.menu_enable_recovery);
        if (recovery != null) recovery.setVisible(isOwner);

        MenuItem transfer = menu.findItem(R.id.menu_transfer_ownership);
        if (transfer != null) transfer.setVisible(isOwner);

        MenuItem wipe = menu.findItem(R.id.menu_reset_app_data);
        if (wipe != null) wipe.setVisible(isOwner);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.menu_enable_recovery) { promptAuthThenEnableRecovery(); return true; }
        if (item.getItemId() == R.id.menu_reset_app_data) { promptAuthThenConfirmReset(); return true; }
        if (item.getItemId() == R.id.menu_transfer_ownership) { promptAuthThenTransferOwnership(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private boolean isDeviceAuthUnavailable() {
        int canAuth = BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        | BiometricManager.Authenticators.BIOMETRIC_STRONG
        );
        return canAuth != BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void promptAuthThenEnableRecovery() {
        if (!DeviceOwnerManager.isActiveEmployeeOwner(this)) { Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show(); return; }
        if (isDeviceAuthUnavailable()) { Toast.makeText(this, R.string.recovery_auth_unavailable, Toast.LENGTH_SHORT).show(); return; }

        Executor ex = ContextCompat.getMainExecutor(this);
        BiometricPrompt bp = new BiometricPrompt(this, ex, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                long ownerId = DeviceOwnerManager.getOwnerEmployeeId(AdministratorActivity.this);
                getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_RECOVERY_ENABLED, true)
                        .putLong(KEY_RECOVERY_OWNER_ID, ownerId)
                        .apply();
                getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_RECOVERY_ENABLED, true).apply();
                Toast.makeText(AdministratorActivity.this, R.string.recovery_enabled_toast, Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo pi = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.enable_password_recovery))
                .setSubtitle(getString(R.string.verify_device))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                | BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .build();

        bp.authenticate(pi);
    }

    private void promptAuthThenConfirmReset() {
        if (isDeviceAuthUnavailable()) { Toast.makeText(this, R.string.recovery_auth_unavailable, Toast.LENGTH_SHORT).show(); return; }

        Executor ex = ContextCompat.getMainExecutor(this);
        BiometricPrompt bp = new BiometricPrompt(this, ex, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                final EditText input = new EditText(AdministratorActivity.this);
                input.setHint("Type delete to confirm");

                new AlertDialog.Builder(AdministratorActivity.this)
                        .setTitle(R.string.reset_app_title)
                        .setMessage(R.string.reset_app_message)
                        .setView(input)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.continue_label, (d,w) -> {
                            String val = input.getText() == null ? "" : input.getText().toString().trim();
                            if (!"delete".equalsIgnoreCase(val)) {
                                Toast.makeText(AdministratorActivity.this, R.string.reset_app_mismatch, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            new AlertDialog.Builder(AdministratorActivity.this)
                                    .setTitle(R.string.reset_app_final_title)
                                    .setMessage(R.string.reset_app_final_message)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(R.string.reset_app_wipe_now, (d2,w2) ->
                                            AppResetUtil.wipeAllAndRestart(AdministratorActivity.this))
                                    .show();
                        })
                        .show();
            }
        });

        BiometricPrompt.PromptInfo pi = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.reset_app_menu))
                .setSubtitle(getString(R.string.verify_device))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                | BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .build();

        bp.authenticate(pi);
    }

    private void promptAuthThenTransferOwnership() {
        if (!DeviceOwnerManager.isActiveEmployeeOwner(this)) {
            Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isDeviceAuthUnavailable()) {
            Toast.makeText(this, R.string.recovery_auth_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        Executor ex = ContextCompat.getMainExecutor(this);
        BiometricPrompt bp = new BiometricPrompt(this, ex, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                showAdminPickerAndTransfer();
            }
        });

        BiometricPrompt.PromptInfo pi = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.transfer_ownership))
                .setSubtitle(getString(R.string.verify_device))
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                | BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .build();

        bp.authenticate(pi);
    }

    private void showAdminPickerAndTransfer() {
        repo.getAdmins().observe(this, admins -> {
            if (admins == null || admins.isEmpty()) {
                Toast.makeText(this, "No admins found.", Toast.LENGTH_SHORT).show();
                return;
            }

            long currentOwnerId = DeviceOwnerManager.getOwnerEmployeeId(this);

            List<Employee> choices = new ArrayList<>();
            for (Employee e : admins) {
                if (e != null && e.getEmployeeID() != currentOwnerId) choices.add(e);
            }

            if (choices.isEmpty()) {
                Toast.makeText(this, "No other admin available.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] labels = new String[choices.size()];
            for (int i = 0; i < choices.size(); i++) labels[i] = choices.get(i).getEmployeeName();

            new AlertDialog.Builder(this)
                    .setTitle(R.string.transfer_ownership)
                    .setItems(labels, (d, which) -> {
                        Employee target = choices.get(which);
                        confirmTransfer(target);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        });
    }

    private void confirmTransfer(@NonNull Employee target) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.transfer_ownership)
                .setMessage("Transfer ownership to " + target.getEmployeeName() + "?\n\nRecovery will be disabled until the new owner re-enrolls.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Transfer", (d,w) -> {
                    DeviceOwnerManager.setOwnerEmployeeId(this, target.getEmployeeID());

                    getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_RECOVERY_ENABLED, false)
                            .remove(KEY_RECOVERY_OWNER_ID)
                            .apply();

                    invalidateOptionsMenu();
                    Toast.makeText(this, "Ownership transferred.", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
