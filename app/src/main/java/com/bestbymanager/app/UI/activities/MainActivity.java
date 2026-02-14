package com.bestbymanager.app.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseEmployeeRequiredActivity;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.databinding.ActivityMainBinding;
import com.bestbymanager.app.utilities.AdminMenu;

public class MainActivity extends BaseEmployeeRequiredActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session.get().preload(this);

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

        binding.productDetailsButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProductDetails.class)));
        binding.productSearchButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProductSearch.class)));
        binding.productListButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProductList.class)));
        binding.employeeListButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, UserList.class).putExtra("selectMode", true)));
        binding.aboutButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AboutActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        AdminMenu.inflateIfAdmin(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isAdmin = Session.get().currentUserIsAdmin();
        menu.findItem(R.id.adminPage).setVisible(isAdmin);
        AdminMenu.setVisibility(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) {
            return true;
        } else if (item.getItemId() == R.id.adminPage) {
            startActivity(new Intent(this, AdministratorActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}