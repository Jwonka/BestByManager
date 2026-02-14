package com.bestbymanager.app.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
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
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.User;
import com.bestbymanager.app.databinding.ActivityUserListBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.viewmodel.UserListViewModel;
import java.util.List;

public class UserList extends AppCompatActivity {

    private Repository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = new Repository(getApplication());
        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);

        setTitle(selectMode ? R.string.select_employee : R.string.employee_list);
        if (getSupportActionBar() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(!selectMode); }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (selectMode) return; // force selection
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

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
        binding.employeeDetailsButton.setOnClickListener(v -> startActivity(new Intent(UserList.this, UserDetails.class)));

        final UserAdapter userAdapter = new UserAdapter((userID) -> {
            if (!selectMode) {
                startActivity(new Intent(UserList.this, UserDetails.class).putExtra("userID", userID));
                return;
            }

            // Admin override: no PIN needed to select anyone
            if (Session.get().currentUserIsAdmin()) {
                ActiveEmployeeManager.setActiveEmployeeId(UserList.this, userID);
                Toast.makeText(UserList.this, "Employee selected.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Non-admin: enforce PIN (set on first selection, then verify on future selections)
            repo.getEmployeePinState(userID).observe(this, state -> {
                if (state == null) return;

                if (state.locked) {
                    Long until = state.lockedUntilMs;
                    long msLeft = Math.max(0, (until == null ? 0L : until) - System.currentTimeMillis());
                    long sec = msLeft / 1000;
                    Toast.makeText(this, "Locked. Try again in " + sec + "s.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!state.hasPin)  showSetPinDialog(userID);
                else showVerifyPinDialog(userID);
            });
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

    private void showSetPinDialog(long userId) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("4-8 digit PIN");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Set employee PIN")
                .setMessage("PIN is required for this employee.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Set PIN", (d, which) -> {
                    String pin = input.getText() == null ? "" : input.getText().toString().trim();
                    repo.setEmployeePin(userId, pin).observe(this, ok -> {
                        if (Boolean.TRUE.equals(ok)) {
                            // Immediately verify (forced on first selection rule)
                            showVerifyPinDialog(userId);
                        } else {
                            Toast.makeText(this, "PIN must be 4-8 digits.", Toast.LENGTH_SHORT).show();
                            showSetPinDialog(userId);
                        }
                    });
                })
                .show();
    }

    private void showVerifyPinDialog(long userId) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Employee PIN")
                .setView(input)
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> { /* stay here */ })
                .setPositiveButton("Unlock", (d, w) -> {
                    String pin = input.getText() == null ? "" : input.getText().toString().trim();
                    repo.verifyEmployeePin(userId, pin).observe(this, res -> {
                        if (res == null) return;

                        switch (res.code) {
                            case OK:
                                ActiveEmployeeManager.setActiveEmployeeId(this, userId);
                                Toast.makeText(this, "Employee selected.", Toast.LENGTH_SHORT).show();
                                finish();
                                break;

                            case NO_PIN_SET:
                                showSetPinDialog(userId);
                                break;

                            case LOCKED: {
                                long msLeft = Math.max(0, (res.lockedUntilMs == null ? 0 : res.lockedUntilMs) - System.currentTimeMillis());
                                long sec = msLeft / 1000;
                                Toast.makeText(this, "Locked. Try again in " + sec + "s.", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            case BAD_PIN:
                            default:
                                Toast.makeText(this, "Bad PIN. Attempts: " + res.failedAttempts, Toast.LENGTH_SHORT).show();
                                showVerifyPinDialog(userId);
                                break;
                        }
                    });
                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);
        if (selectMode) return false; // no admin menu while selecting
        getMenuInflater().inflate(R.menu.menu_user_list, menu);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { if (selectMode) return true; finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.employeeSearch) { startActivity(new Intent(this, UserSearch.class)); return true; }
        if (item.getItemId() == R.id.employeeDetails) { startActivity(new Intent(this, UserDetails.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}
