package com.bestbymanager.app.UI.activities;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.AuthenticationAction;
import com.bestbymanager.app.UI.authentication.LoginAction;
import com.bestbymanager.app.UI.authentication.RegisterAction;
import com.bestbymanager.app.UI.authentication.Session;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.database.ProductDatabaseBuilder;
import com.bestbymanager.app.databinding.ActivityLoginBinding;
import com.google.android.material.button.MaterialButton;
import android.view.inputmethod.EditorInfo;
import java.io.File;

public class LoginActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "app_prefs";
    private static final int REQ_POST_NOTIF = 42;
    private static final String DB_NAME = "MyProductDatabase.db";

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
        ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
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

        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean imeDone = actionId == EditorInfo.IME_ACTION_DONE;
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;

            if (imeDone || enter) {
                // ensure IME follows whatever the button currently does (register vs login)
                binding.loginButton.performClick();
                return true;
            }
            return false;
        });
    }

    private void refreshFirstRunStateAndWireUi() {
        final MaterialButton button = binding.loginButton;

        // Disable until we know state
        button.setEnabled(false);

        new Thread(() -> {
            boolean anyUsers = false;
            try {
                anyUsers = repository.userCountBlocking() > 0;
            } catch (Throwable ignored) {}

            final boolean computedFirstRun = !anyUsers;
            runOnUiThread(() -> {
                firstRun = computedFirstRun;

                if (firstRun) {
                    button.setText(R.string.register);
                    button.setOnClickListener(v -> {
                        registerAction.run();

                        // after registration attempt, re-check DB and rewire (only flips to login if a user exists)
                        refreshFirstRunStateAndWireUi();
                    });
                } else {
                    button.setText(R.string.login);
                    button.setOnClickListener(v -> loginAction.run());
                }

                button.setEnabled(true);
                invalidateOptionsMenu(); // hide/show overflow item based on firstRun
            });
        }).start();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login_activity, menu);
        return true;
    }

    // Hide overflow item on first run (your preference)
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem reset = menu.findItem(R.id.menu_reset_app_data);
        if (reset != null) reset.setVisible(!firstRun);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_reset_app_data) {
            showResetDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResetDialog() {
        final EditText input = new EditText(this);
        input.setHint("Type delete to confirm");

        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_app_title)
                .setMessage(R.string.reset_app_message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.continue_label, (d, w) -> {
                    String val = input.getText() == null ? "" : input.getText().toString().trim();
                    if (!"delete".equalsIgnoreCase(val)) {
                        Toast.makeText(this, R.string.reset_app_mismatch, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.reset_app_final_title)
                            .setMessage(R.string.reset_app_final_message)
                            .setNegativeButton(android.R.string.cancel, (d2, w2) -> d2.dismiss())
                            .setPositiveButton(R.string.reset_app_wipe_now, (d2, w2) -> performFactoryReset())
                            .show();
                })
                .show();
    }

    private void performFactoryReset() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            boolean requested = am.clearApplicationUserData();
            if (requested) {
                // force a cold restart so we don’t sit in stale UI state
                restartFresh();
                return;
            }
        }

        try {
            try {
                ProductDatabaseBuilder db = ProductDatabaseBuilder.getDatabase(getApplicationContext());
                if (db != null) db.close();
            } catch (Throwable ignored) {}

            deleteDatabase(DB_NAME);
            deleteDatabase(DB_NAME + "-wal");
            deleteDatabase(DB_NAME + "-shm");

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
            getSharedPreferences("bestby_session", MODE_PRIVATE).edit().clear().apply();
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().clear().apply();

            deleteRecursively(getCacheDir());

            restartFresh();
        } catch (Exception e) {
            Toast.makeText(this, "Reset failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restartFresh() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(launch);
        } else {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
        finishAffinity();
        Process.killProcess(Process.myPid());
        System.exit(0);
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursively(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
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
