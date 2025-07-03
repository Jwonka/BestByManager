package com.example.bestbymanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.adapter.UserAdapter;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.databinding.ActivityUserListBinding;
import com.example.bestbymanager.viewmodel.UserListViewModel;

public class UserList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.user_list);
        ActivityUserListBinding binding = ActivityUserListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.userDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserList.this, UserDetails.class);
            startActivity(intent);
        });

        final UserAdapter userAdapter = new UserAdapter((userID) -> {
            Intent intent = new Intent(this, UserDetails.class)
                    .putExtra("userID", userID);
            startActivity(intent);
        });
        binding.userListRecyclerView.setAdapter(userAdapter);
        binding.userListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        UserListViewModel userListViewModel = new ViewModelProvider(this).get(UserListViewModel.class);

        userListViewModel.getUsers().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                binding.noUsersMessage.setVisibility(View.VISIBLE);
            } else {
                binding.noUsersMessage.setVisibility(View.GONE);
                userAdapter.setUsers(list);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_list, menu);
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
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        } else if (item.getItemId() == R.id.mainScreen) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.productDetails) {
            Intent intent = new Intent(this, ProductDetails.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.productSearch) {
            Intent intent = new Intent(this, ProductSearch.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.adminPage) {
            Intent intent = new Intent(this, AdministratorActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() { super.onResume(); }
}
