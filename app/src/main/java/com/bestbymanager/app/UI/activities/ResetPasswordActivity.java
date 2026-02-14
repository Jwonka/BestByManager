package com.bestbymanager.app.UI.activities;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bestbymanager.app.UI.authentication.ResetPasswordAction;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.databinding.ActivityResetPasswordBinding;

public class ResetPasswordActivity  extends AppCompatActivity {
    private ResetPasswordAction action;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        Session.get().preload(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityResetPasswordBinding binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
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

        boolean recoveryMode = getIntent().getBooleanExtra("recovery_mode", false);

        long userID = getIntent().getLongExtra("userId", -1);

        Repository repository = new Repository(getApplication());

        if (recoveryMode) {
            // recovery ALWAYS targets the first admin only
            new Thread(() -> {
                Long adminId = repository.getFirstAdminIdBlocking();
                runOnUiThread(() -> {
                    if (adminId == null || adminId <= 0) {
                        Toast.makeText(this, "No admin account found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    initUi(binding, repository, adminId);
                });
            }).start();
            return;
        }

        if (userID < 0) {
            Long sid = Session.get().userId();
            if (sid != null && sid > 0) userID = sid;
            else { finish(); return; }
        }
        initUi(binding, repository, userID);
    }

    private void initUi(ActivityResetPasswordBinding binding, Repository repository, long userID) {
        repository.getUser(userID).observe(this, user -> {
            if (user != null) binding.usernameLabel.setText(user.getUserName());
        });

        action = new ResetPasswordAction(
                this,
                binding.usernameLabel,
                binding.passwordInput,
                binding.passwordConfirmationInput,
                userID,
                repository
        );

        binding.passwordConfirmationInput.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_DONE) { action.run(); return true; }
            return false;
        });

        binding.resetButton.setOnClickListener(v -> action.run());
    }
}

