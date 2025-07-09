package com.example.bestbymanager.UI.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.activities.MainActivity;

public abstract class BaseAdminActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.get().currentUserIsAdmin()) {
            Toast.makeText(this, R.string.not_authorized, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }
}