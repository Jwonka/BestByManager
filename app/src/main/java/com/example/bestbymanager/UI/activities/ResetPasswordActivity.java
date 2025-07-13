package com.example.bestbymanager.UI.activities;

import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bestbymanager.UI.authentication.ResetPasswordAction;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.databinding.ActivityResetPasswordBinding;

public class ResetPasswordActivity  extends AppCompatActivity {
    private ActivityResetPasswordBinding binding;
    private ResetPasswordAction action;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long userID = getIntent().getLongExtra("userID", -1);
        if (userID < 0) { finish(); return; }

        Repository repository = new Repository(getApplication());

        repository.getUser(userID).observe(this, user -> {
            if (user != null) binding.usernameLabel.setText(user.getUserName());
        });

        action = new ResetPasswordAction(this, binding.usernameLabel, binding.passwordInput, binding.passwordConfirmationInput, userID, repository);

        binding.passwordConfirmationInput.setOnEditorActionListener(
                (v,id,e)->{
                    if(id==EditorInfo.IME_ACTION_DONE){
                        action.run();
                        return true;
                    }
                    return false;
                });
        binding.resetButton.setOnClickListener(v -> action.run());
    }
}

