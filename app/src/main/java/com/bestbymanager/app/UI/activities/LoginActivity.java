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
    private static final String KEY_FIRST_RUN_DONE = "first_run_done";

    // Room DB filename from your builder:
    private static final String DB_NAME = "MyProductDatabase.db";

    private AuthenticationAction loginAction;
    private AuthenticationAction registerAction;

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

        Repository repository = new Repository(getApplication());

        loginAction = new LoginAction(this, binding.userNameInput, binding.passwordInput, repository);
        registerAction = new RegisterAction(this, binding.userNameInput, binding.passwordInput, repository);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final boolean[] firstRun = { !prefs.getBoolean(KEY_FIRST_RUN_DONE, false) };

        MaterialButton button = binding.loginButton;

        if (firstRun[0]) {
            button.setText(R.string.register);
            button.setOnClickListener(v -> {
                registerAction.run();
                prefs.edit().putBoolean(KEY_FIRST_RUN_DONE, true).apply();
                firstRun[0] = false; // important: fixes “IME still runs login”
                button.setText(R.string.login);
                button.setOnClickListener(x -> loginAction.run());
            });
        } else {
            button.setText(R.string.login);
            button.setOnClickListener(v -> loginAction.run());
        }

        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean imeDone = actionId == EditorInfo.IME_ACTION_DONE;
            boolean enter = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;

            if (imeDone || enter) {
                if (firstRun[0]) registerAction.run();
                else loginAction.run();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login_activity, menu);
        return true;
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
        input.setHint("Type DELETE to confirm");

        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_app_title)
                .setMessage(R.string.reset_app_message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.continue_label, (d, w) -> {
                    String val = input.getText() == null ? "" : input.getText().toString().trim();
                    if (!"DELETE".equals(val)) {
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
        // Preferred: full app-data wipe (same as Settings -> Clear storage)
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            boolean requested = am.clearApplicationUserData();
            if (requested) return; // system will kill/restart the app process
        }

        // Fallback: close DB + delete DB files + clear prefs + clear cache + restart
        try {
            try {
                ProductDatabaseBuilder db = ProductDatabaseBuilder.getDatabase(getApplicationContext());
                if (db != null) db.close();
            } catch (Throwable ignored) {}

            deleteDatabase(DB_NAME);
            deleteDatabase(DB_NAME + "-wal");
            deleteDatabase(DB_NAME + "-shm");

            // clear app_prefs (first_run_done etc)
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();

            // clear Session prefs + default prefs
            getSharedPreferences("bestby_session", MODE_PRIVATE).edit().clear().apply();
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().clear().apply();

            // clear cache dir (temp photos etc)
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
        // ignore return; best-effort
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
