package com.bestbymanager.app.UI.activities;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseEmployeeRequiredActivity;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.databinding.ActivityMainBinding;
import com.bestbymanager.app.utilities.AdminMenu;

public class MainActivity extends BaseEmployeeRequiredActivity {

    @Override
    protected void onGatePassed() {
        setTitle(R.string.main_screen);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            var systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.productDetailsButton.setOnClickListener(v -> startActivity(new Intent(this, ProductDetails.class)));
        binding.productSearchButton.setOnClickListener(v -> startActivity(new Intent(this, ProductSearch.class)));
        binding.productListButton.setOnClickListener(v -> startActivity(new Intent(this, ProductList.class)));
        binding.employeeListButton.setOnClickListener(v -> startActivity(new Intent(this, EmployeeList.class).putExtra("selectMode", true)));
        binding.aboutButton.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        AdminMenu.inflateKioskActions(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem adminItem = menu.findItem(R.id.adminPage);
        if (adminItem != null) adminItem.setVisible(ActiveEmployeeManager.isActiveEmployeeAdmin(this));
        AdminMenu.setVisibility(this, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == R.id.adminPage) { startActivity(new Intent(this, AdministratorActivity.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}