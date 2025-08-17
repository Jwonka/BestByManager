package com.example.bestbymanager.UI.activities;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
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
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.AuthenticationAction;
import com.example.bestbymanager.UI.authentication.LoginAction;
import com.example.bestbymanager.UI.authentication.RegisterAction;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.databinding.ActivityLoginBinding;
import com.google.android.material.button.MaterialButton;
import android.view.inputmethod.EditorInfo;

public class LoginActivity extends AppCompatActivity {
    private static final String PREFS_NAME         = "app_prefs";
    private static final int REQ_POST_NOTIF = 42;
    private static final String KEY_FIRST_RUN_DONE = "first_run_done";
    private AuthenticationAction loginAction;
    private AuthenticationAction registerAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.POST_NOTIFICATIONS },
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
        boolean firstRun = !prefs.getBoolean(KEY_FIRST_RUN_DONE, false);

        MaterialButton button = binding.loginButton;

        if (firstRun) {
            button.setText(R.string.register);
            button.setOnClickListener(v -> {
                registerAction.run();
                prefs.edit().putBoolean(KEY_FIRST_RUN_DONE, true).apply();
            });
        } else {
            button.setText(R.string.login);
            button.setOnClickListener(v -> loginAction.run());
        }

        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                loginAction.run();
                return true;
            }
            return false;
        });
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

