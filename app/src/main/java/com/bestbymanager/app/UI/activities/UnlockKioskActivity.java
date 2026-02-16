package com.bestbymanager.app.UI.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.biometric.BiometricManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.AuthenticationAction;
import com.bestbymanager.app.UI.authentication.SetupOwnerAction;
import com.bestbymanager.app.UI.authentication.UnlockKioskAction;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.databinding.ActivityUnlockKioskBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import com.google.android.material.button.MaterialButton;

public class UnlockKioskActivity extends AppCompatActivity {
    private static final int REQ_POST_NOTIF = 42;
    private AuthenticationAction unlockAction;
    private AuthenticationAction setupOwnerAction;
    private Repository repository;
    private ActivityUnlockKioskBinding binding;
    private volatile boolean firstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Session.get().preload(this);

        // If kiosk already unlocked (and not in forced reset)
        if (Session.get().isUnlocked() && !Session.get().requiresPasswordReset()) {
                // Active employee navigates to home screen
            if (ActiveEmployeeManager.hasActiveEmployee(this)) {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            } else {  // go straight to employee switch/select
                startActivity(new Intent(this, EmployeeList.class)
                        .putExtra("selectMode", true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            }

            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIF);
        }

        // Title should map to "Unlock kiosk" or "Set up owner"
        setTitle(R.string.unlock_kiosk);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityUnlockKioskBinding.inflate(getLayoutInflater());
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

        repository = new Repository(getApplication());

        unlockAction = new UnlockKioskAction(this, binding.ownerNameInput, binding.passwordInput, repository);
        setupOwnerAction = new SetupOwnerAction(this, binding.ownerNameInput, binding.passwordInput, repository);

        refreshFirstRunStateAndWireUi();

        findViewById(R.id.link_forgot_password).setOnClickListener(v -> {
            if (firstRun) return;

            boolean enabled = getSharedPreferences("security_prefs", MODE_PRIVATE)
                    .getBoolean("recovery_enabled", false);

            if (!enabled) {
                Toast.makeText(this, "Password recovery not set up yet. Ask an admin to enable it in Settings.", Toast.LENGTH_LONG).show();
                return;
            }

            BiometricManager bm = BiometricManager.from(this);
            int canAuth = bm.canAuthenticate(
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            | BiometricManager.Authenticators.BIOMETRIC_STRONG
            );

            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                Toast.makeText(this, "Screen lock required for recovery. Set a PIN/pattern in Android settings.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
                return;
            }

            startActivity(new Intent(this, com.bestbymanager.app.UI.authentication.RecoveryActivity.class));
        });

        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean imeDone = actionId == EditorInfo.IME_ACTION_DONE;
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;

            if (imeDone || enter) {
                binding.unlockButton.performClick();
                return true;
            }
            return false;
        });
    }

    private void refreshFirstRunStateAndWireUi() {
        final MaterialButton button = binding.unlockButton;
        button.setEnabled(false);

        new Thread(() -> {
            boolean anyEmployees = false;
            try {
                anyEmployees = repository.employeeCountBlocking() > 0;
            } catch (Throwable ignored) {}

            final boolean computedFirstRun = !anyEmployees;
            runOnUiThread(() -> {
                firstRun = computedFirstRun;
                binding.linkForgotPassword.setVisibility(firstRun ? View.GONE : View.VISIBLE);

                if (firstRun) {
                    setTitle(R.string.setup_owner);
                    button.setText(R.string.setup_owner);
                    button.setOnClickListener(v -> {
                        setupOwnerAction.run();
                        refreshFirstRunStateAndWireUi();
                    });
                } else {
                    setTitle(R.string.unlock_kiosk);
                    button.setText(R.string.unlock_kiosk);
                    button.setOnClickListener(v -> unlockAction.run());
                }

                button.setEnabled(true);
            });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] perms, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, perms, grantResults);
        if (requestCode == REQ_POST_NOTIF &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.notif_enabled, Toast.LENGTH_SHORT).show();
        }
    }
}