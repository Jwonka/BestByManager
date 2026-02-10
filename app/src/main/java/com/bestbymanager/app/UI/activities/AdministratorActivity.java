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
import com.bestbymanager.app.databinding.ActivityAdministratorBinding;
import com.bestbymanager.app.utilities.AppResetUtil;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executor;

public class AdministratorActivity extends BaseAdminActivity {

    private static final String SECURITY_PREFS = "security_prefs";
    private static final String KEY_RECOVERY_ENABLED = "recovery_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.admin);

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
            Intent intent = new Intent(AdministratorActivity.this, UserList.class);
            startActivity(intent);
        });

        binding.employeeDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, UserDetails.class);
            startActivity(intent);
        });

        binding.employeeSearchButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, UserSearch.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_administrator_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }

        if (item.getItemId() == R.id.menu_enable_recovery) { promptAuthThenEnableRecovery(); return true; }
        if (item.getItemId() == R.id.menu_reset_app_data) { promptAuthThenConfirmReset(); return true; }

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
        if (isDeviceAuthUnavailable()) { Toast.makeText(this, R.string.recovery_auth_unavailable, Toast.LENGTH_SHORT).show(); return; }

        Executor ex = ContextCompat.getMainExecutor(this);
        BiometricPrompt bp = new BiometricPrompt(this, ex, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
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
}
