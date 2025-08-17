package com.example.bestbymanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.main_screen);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
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

        binding.productDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProductDetails.class);
            startActivity(intent);
        });

        binding.productSearchButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProductSearch.class);
            startActivity(intent);
        });

        binding.productListButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProductList.class);
            startActivity(intent);
        });

        binding.logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        binding.aboutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isAdmin = Session.get().currentUserIsAdmin();
        menu.findItem(R.id.adminPage).setVisible(isAdmin);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.adminPage) {
            Intent intent = new Intent(this, AdministratorActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}