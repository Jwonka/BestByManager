package com.bestbymanager.app.UI.authentication;

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
import java.util.concurrent.Executor;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.activities.ResetPasswordActivity;

public class RecoveryActivity extends AppCompatActivity {
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private static final String SECURITY_PREFS = "security_prefs";
    private static final String KEY_RECOVERY_ENABLED = "recovery_enabled";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery);

        final View btn = findViewById(R.id.btn_verify_device);
        final TextView err = findViewById(R.id.recovery_error);

        boolean enabled = getSharedPreferences(SECURITY_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_RECOVERY_ENABLED, false);
        if (!enabled) {
            if (btn != null) btn.setEnabled(false);
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
            btn.setEnabled(false);
            err.setVisibility(View.VISIBLE);
            err.setText(R.string.recover_account);
            err.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)));
            Toast.makeText(this, "Screen lock required for recovery", Toast.LENGTH_LONG).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        // enable password reset UI (show fields + confirm button)
                        showResetUi();
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

        findViewById(R.id.btn_verify_device).setOnClickListener(v ->
                biometricPrompt.authenticate(promptInfo)
        );
    }

    private void showResetUi() {
        Intent i = new Intent(this, ResetPasswordActivity.class);
        i.putExtra("recovery_mode", true);
        startActivity(i);
        finish();
    }
}