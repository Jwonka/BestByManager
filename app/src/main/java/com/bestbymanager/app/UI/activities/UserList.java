package com.bestbymanager.app.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.adapter.UserAdapter;
import com.bestbymanager.app.data.entities.User;
import com.bestbymanager.app.databinding.ActivityUserListBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.viewmodel.UserListViewModel;
import java.util.List;

public class UserList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);

        setTitle(selectMode ? R.string.select_employee : R.string.employee_list);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityUserListBinding binding = ActivityUserListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final View rootView = binding.getRoot();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.employeeDetailsButton.setVisibility(selectMode ? View.GONE : View.VISIBLE);

        binding.employeeDetailsButton.setOnClickListener(v -> {
            startActivity(new Intent(UserList.this, UserDetails.class));
        });

        final UserAdapter userAdapter = new UserAdapter((userID) -> {
            if (selectMode) {
                // TEMP: until PIN flow is wired, just select and return
                ActiveEmployeeManager.setActiveEmployeeId(UserList.this, userID);
                Toast.makeText(UserList.this, "Employee selected.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                startActivity(new Intent(UserList.this, UserDetails.class).putExtra("userID", userID));
            }
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
        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);
        if (selectMode) return false; // no admin menu while selecting
        getMenuInflater().inflate(R.menu.menu_user_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.employeeSearch) { startActivity(new Intent(this, UserSearch.class)); return true; }
        if (item.getItemId() == R.id.employeeDetails) { startActivity(new Intent(this, UserDetails.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}
