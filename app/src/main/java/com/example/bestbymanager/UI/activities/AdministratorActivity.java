package com.example.bestbymanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.BaseAdminActivity;
import com.example.bestbymanager.databinding.ActivityAdministratorBinding;

public class AdministratorActivity extends BaseAdminActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.admin);
        ActivityAdministratorBinding binding = ActivityAdministratorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.employeeListButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, UserList.class);
            startActivity(intent);
        });

        binding.employeeDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, UserDetails.class);
            startActivity(intent);
        });

        binding.employeeSearchButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdministratorActivity.this, UserSearch.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_administrator_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        } else if (item.getItemId() == R.id.mainScreen) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
