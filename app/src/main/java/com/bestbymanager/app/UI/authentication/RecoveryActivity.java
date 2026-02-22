package com.bestbymanager.app.UI.authentication;

import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.concurrent.Executor;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.activities.ResetPasswordActivity;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.session.DeviceOwnerManager;

public class RecoveryActivity extends AppCompatActivity {
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private static final String SECURITY_PREFS = "security_prefs";
    private static final String KEY_RECOVERY_ENABLED = "recovery_enabled";
    private static final String KEY_RECOVERY_OWNER_ID = "recovery_owner_id";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery);

        final View btn = findViewById(R.id.btn_verify_device);
        final TextView err = findViewById(R.id.recovery_error);

        var sp = getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE);
        boolean enabled = sp.getBoolean(KEY_RECOVERY_ENABLED, false);
        long enrolledOwnerId = sp.getLong(KEY_RECOVERY_OWNER_ID, -1L);
        long currentOwnerId = DeviceOwnerManager.getOwnerEmployeeId(this);

        if (btn != null) btn.setEnabled(false);

        if (!enabled || enrolledOwnerId <= 0 || enrolledOwnerId != currentOwnerId) {
            if (err != null) {
                err.setVisibility(View.VISIBLE);
                err.setText(R.string.recovery_not_enabled);
            }
            Toast.makeText(this, R.string.recovery_not_enabled, Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricManager bm = BiometricManager.from(this);
        int canAuth = bm.canAuthenticate(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        | BiometricManager.Authenticators.BIOMETRIC_STRONG
        );

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            err.setVisibility(View.VISIBLE);
            err.setText(R.string.recover_account);
            err.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)));
            Toast.makeText(this, "Screen lock required for recovery", Toast.LENGTH_LONG).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        showAdminPickerThenReset();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        Toast.makeText(RecoveryActivity.this, errString, Toast.LENGTH_SHORT).show();
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify device to reset password")
                .setSubtitle("Use fingerprint/face or device PIN")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                | BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .build();

        if (btn != null) {
            btn.setEnabled(true);
            btn.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
        }
    }

    private void showAdminPickerThenReset() {
        Repository repo = new Repository(getApplication());

        new Thread(() -> {
            List<Employee> admins = repo.getAdminsBlocking();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (admins == null || admins.isEmpty()) {
                    Toast.makeText(this, "No admin account found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] labels = new String[admins.size()];
                for (int i = 0; i < admins.size(); i++) labels[i] = admins.get(i).getEmployeeName();

                new AlertDialog.Builder(this)
                        .setTitle("Select admin to reset")
                        .setItems(labels, (d, which) -> {
                            long adminId = admins.get(which).getEmployeeID();
                            Intent i = new Intent(this, ResetPasswordActivity.class);
                            i.putExtra("recovery_mode", true);
                            i.putExtra("employeeId", adminId);
                            startActivity(i);
                            finish();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });
        }).start();
    }
}