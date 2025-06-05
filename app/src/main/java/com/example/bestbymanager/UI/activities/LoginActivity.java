package com.example.bestbymanager.UI.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.AuthenticationAction;
import com.example.bestbymanager.UI.authentication.LoginAction;
import com.example.bestbymanager.UI.authentication.RegisterAction;
import com.example.bestbymanager.data.database.Repository;

public class LoginActivity extends AppCompatActivity {
    private AuthenticationAction loginAction;
    private AuthenticationAction registerAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        EditText userName = findViewById(R.id.user_name_input);
        EditText password = findViewById(R.id.password_input);

        Repository repository = new Repository(getApplication());

        loginAction = new LoginAction(this, userName, password, repository);
        registerAction = new RegisterAction(this, userName, password, repository);

        findViewById(R.id.login_button).setOnClickListener(v -> loginAction.run());
        findViewById(R.id.register_button).setOnClickListener(v -> registerAction.run());
    }
}

