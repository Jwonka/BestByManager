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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.adapter.UserAdapter;
import com.bestbymanager.app.UI.authentication.BaseAdminActivity;
import com.bestbymanager.app.data.entities.User;
import com.bestbymanager.app.databinding.ActivityUserListBinding;
import com.bestbymanager.app.viewmodel.UserListViewModel;

import java.util.List;

public class UserList extends BaseAdminActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.employee_list);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityUserListBinding binding = ActivityUserListBinding.inflate(getLayoutInflater());
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

        boolean showOnlyAdmins = getIntent().getBooleanExtra("admin_only", false);

        LiveData<List<User>> liveList = showOnlyAdmins ? userListViewModel.loadAdmins() : userListViewModel.getUsers();

        liveList.observe(this, list -> {
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
