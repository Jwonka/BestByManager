package com.bestbymanager.app.UI.activities;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bestbymanager.app.UI.authentication.ResetPasswordAction;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.databinding.ActivityResetPasswordBinding;

public class ResetPasswordActivity extends AppCompatActivity {
    private ResetPasswordAction action;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        Session.get().preload(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityResetPasswordBinding binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(),  (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        final boolean recoveryMode = getIntent().getBooleanExtra("recovery_mode", false);

       long idFromIntent = getIntent().getLongExtra("employeeId", -1L);

        if (!recoveryMode && idFromIntent < 0) {
            Long sid = Session.get().limitedEmployeeId();
            idFromIntent = (sid != null && sid > 0) ? sid : -1L;
        }

        final long targetId = idFromIntent;
        final Repository repository = new Repository(getApplication());

        if (recoveryMode) {
            if (targetId <= 0) {
                Toast.makeText(this, "Missing admin target.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            repository.getEmployee(targetId).observe(this, employee -> {
                if (employee == null || !employee.isAdmin()) {
                    Toast.makeText(this, "Invalid admin target.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                initUi(binding, repository, targetId);
            });
            return;
        }

        if (targetId <= 0) {
            finish();
            return;
        }
        initUi(binding, repository, targetId);
    }

    private void initUi(ActivityResetPasswordBinding binding, Repository repository, long employeeID) {
        action = new ResetPasswordAction(
                this,
                binding.employeeNameLabel,
                binding.passwordInput,
                binding.passwordConfirmationInput,
                employeeID,
                repository
        );

        repository.getEmployee(employeeID).observe(this, employee -> {
            if (employee != null) {
                binding.employeeNameLabel.setText(employee.getEmployeeName());
                action.setEmployee(employee); // action is always assigned before observe
            }
        });

        binding.passwordConfirmationInput.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_DONE) { action.run(); return true; }
            return false;
        });

        binding.resetButton.setOnClickListener(v -> action.run());
    }
}