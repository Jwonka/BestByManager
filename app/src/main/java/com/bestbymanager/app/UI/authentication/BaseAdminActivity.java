package com.bestbymanager.app.UI.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.activities.MainActivity;
import com.bestbymanager.app.session.Session;

public abstract class BaseAdminActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session.get().preload(this);

        if (!Session.get().isAdmin()) {
            Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        }
    }
}