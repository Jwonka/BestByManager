package com.example.bestbymanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.adapter.UserAdapter;
import com.example.bestbymanager.UI.authentication.BaseAdminActivity;
import com.example.bestbymanager.databinding.ActivityUserListBinding;
import com.example.bestbymanager.viewmodel.UserListViewModel;

public class UserList extends BaseAdminActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.employee_list);
        ActivityUserListBinding binding = ActivityUserListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.employeeDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserList.this, UserDetails.class);
            startActivity(intent);
        });

        final UserAdapter userAdapter = new UserAdapter((userID) -> {
            Intent intent = new Intent(this, UserDetails.class)
                    .putExtra("userID", userID);
            startActivity(intent);
        });
        binding.employeeListRecyclerView.setAdapter(userAdapter);
        binding.employeeListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        UserListViewModel userListViewModel = new ViewModelProvider(this).get(UserListViewModel.class);

        userListViewModel.getUsers().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                binding.noEmployeeCard.setVisibility(View.VISIBLE);
            } else {
                binding.noEmployeeCard.setVisibility(View.GONE);
                userAdapter.setUsers(list);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_list, menu);
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
        } else if (item.getItemId() == R.id.employeeSearch) {
            Intent intent = new Intent(this, UserSearch.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.employeeDetails) {
            Intent intent = new Intent(this, UserDetails.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() { super.onResume(); }
}
