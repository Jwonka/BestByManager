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
import com.bestbymanager.app.UI.authentication.LoginAction;
import com.bestbymanager.app.UI.authentication.RegisterAction;
import com.bestbymanager.app.databinding.ActivityLoginBinding;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.UI.authentication.Session;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {
    private static final int REQ_POST_NOTIF = 42;

    private AuthenticationAction loginAction;
    private AuthenticationAction registerAction;

    private Repository repository;
    private ActivityLoginBinding binding;
    private volatile boolean firstRun = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Session.get().preload(this);
        if (!Session.get().isLoggedOut() && !Session.get().requiresPasswordReset()) {
            startActivity(new Intent(this, MainActivity.class));
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

        setTitle(R.string.login);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
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
        loginAction = new LoginAction(this, binding.userNameInput, binding.passwordInput, repository);
        registerAction = new RegisterAction(this, binding.userNameInput, binding.passwordInput, repository);

        refreshFirstRunStateAndWireUi();

        findViewById(R.id.link_forgot_password).setOnClickListener(v -> {
            // Hide on first run (no account to recover yet)
            if (firstRun) return;

            // Require that recovery has been enabled in-app (after admin setup)
            boolean enabled = getSharedPreferences("security_prefs", MODE_PRIVATE)
                    .getBoolean("recovery_enabled", false);

            if (!enabled) {
                Toast.makeText(this, "Password recovery not set up yet. Ask an admin to enable it in Settings.", Toast.LENGTH_LONG).show();
                return;
            }

            // Require device credential/biometric enrolled
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
                binding.loginButton.performClick();
                return true;
            }
            return false;
        });
    }

    private void refreshFirstRunStateAndWireUi() {
        final MaterialButton button = binding.loginButton;
        button.setEnabled(false);

        new Thread(() -> {
            boolean anyUsers = false;
            try {
                anyUsers = repository.userCountBlocking() > 0;
            } catch (Throwable ignored) {}

            final boolean computedFirstRun = !anyUsers;
            runOnUiThread(() -> {
                firstRun = computedFirstRun;
                binding.linkForgotPassword.setVisibility(firstRun ? View.GONE : View.VISIBLE);

                if (firstRun) {
                    button.setText(R.string.register);
                    button.setOnClickListener(v -> {
                        registerAction.run();
                        refreshFirstRunStateAndWireUi();
                    });
                } else {
                    button.setText(R.string.login);
                    button.setOnClickListener(v -> loginAction.run());
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