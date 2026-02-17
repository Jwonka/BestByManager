package com.bestbymanager.app.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.adapter.EmployeeAdapter;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.databinding.ActivityEmployeeListBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.viewmodel.EmployeeListViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class EmployeeList extends AppCompatActivity {
    private Repository repo;
    private boolean pinFlowInFlight = false;
    @Nullable
    private AlertDialog pinDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repo = new Repository(getApplication());

        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);
        boolean alreadySelected = com.bestbymanager.app.session.ActiveEmployeeManager.hasActiveEmployee(this);

        setTitle(selectMode ? R.string.select_employee : R.string.employee_list);
        if (getSupportActionBar() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(!selectMode || alreadySelected); }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                boolean alreadySelectedNow = com.bestbymanager.app.session.ActiveEmployeeManager.hasActiveEmployee(EmployeeList.this);
                if (selectMode && !alreadySelectedNow) return; // force selection only on first-time
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityEmployeeListBinding binding = ActivityEmployeeListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final View rootView = binding.getRoot();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.employeeDetailsButton.setVisibility(selectMode ? View.GONE : View.VISIBLE);
        binding.employeeDetailsButton.setOnClickListener(v -> startActivity(new Intent(EmployeeList.this, EmployeeDetails.class)));

        final EmployeeAdapter employeeAdapter = new EmployeeAdapter((employeeID) -> {
            if (!selectMode) {
                startActivity(new Intent(EmployeeList.this, EmployeeDetails.class).putExtra("employeeID", employeeID));
                return;
            }

            if (pinFlowInFlight) { return; }
            pinFlowInFlight = true;

            // Non-admin: enforce PIN (set on first selection, then verify on future selections)
            repo.getEmployeePinState(employeeID).observe(this, state -> {
                if (state == null) { pinFlowInFlight = false; return; }

                if (state.locked) {
                    Long until = state.lockedUntilMs;
                    long msLeft = Math.max(0, (until == null ? 0L : until) - System.currentTimeMillis());
                    long sec = msLeft / 1000;
                    Toast.makeText(this, "Locked. Try again in " + sec + "s.", Toast.LENGTH_SHORT).show();
                    pinFlowInFlight = false;
                    return;
                }

                if (!state.hasPin)  showSetPinDialog(employeeID);
                else showVerifyPinDialog(employeeID);
            });
        });

        binding.employeeListRecyclerView.setAdapter(employeeAdapter);
        binding.employeeListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        EmployeeListViewModel employeeListViewModel = new ViewModelProvider(this).get(EmployeeListViewModel.class);
        employeeListViewModel.getEmployees().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                binding.noEmployeeCard.setVisibility(View.VISIBLE);
            } else {
                binding.noEmployeeCard.setVisibility(View.GONE);
                employeeAdapter.setEmployees(list);
            }
        });
    }

    private void onEmployeeSelected(long employeeId) {
        repo.getEmployee(employeeId).observe(this, e -> {
            if (e == null) return;
            ActiveEmployeeManager.setActiveEmployeeId(this, employeeId);
            ActiveEmployeeManager.setActiveEmployeeIsAdmin(this, e.isAdmin());

            Toast.makeText(this, "Employee selected.", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    private void showSetPinDialog(long employeeId) {
        if (pinDialog != null && pinDialog.isShowing()) { return; }
        if (pinDialog != null) { pinDialog.dismiss(); pinDialog = null; }

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("4-8 digit PIN");

        pinDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Set employee PIN")
                .setMessage("PIN is required for this employee.")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Set PIN", (d, which) -> {
                    String pin = input.getText() == null ? "" : input.getText().toString().trim();
                    repo.setEmployeePin(employeeId, pin).observe(this, ok -> {
                        if (Boolean.TRUE.equals(ok)) {
                            showVerifyPinDialog(employeeId);
                        } else {
                            Toast.makeText(this, "PIN must be 4-8 digits.", Toast.LENGTH_SHORT).show();
                            // keep gate true; re-prompt
                            showSetPinDialog(employeeId);
                        }
                    });
                })
                .create();

        pinDialog.setOnDismissListener(d -> pinDialog = null);
        pinDialog.show();
    }

    private void showVerifyPinDialog(long employeeId) {
        if (pinDialog != null && pinDialog.isShowing()) { return; }
        if (pinDialog != null) { pinDialog.dismiss(); pinDialog = null; }

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");

        pinDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Employee PIN")
                .setView(input)
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> {
                    pinFlowInFlight = false;
                    pinDialog = null; // dialog will dismiss right after this
                })
                .setPositiveButton("Unlock", (d, w) -> {
                    String pin = input.getText() == null ? "" : input.getText().toString().trim();
                    repo.verifyEmployeePin(employeeId, pin).observe(this, res -> {
                        if (res == null) return;

                        switch (res.code) {
                            case OK:
                                pinFlowInFlight = false;
                                pinDialog = null;
                                onEmployeeSelected(employeeId);
                                break;

                            case NO_PIN_SET:
                                // keep gate true while we move to set-pin
                                showSetPinDialog(employeeId);
                                break;

                            case LOCKED: {
                                long msLeft = Math.max(0, (res.lockedUntilMs == null ? 0 : res.lockedUntilMs) - System.currentTimeMillis());
                                long sec = msLeft / 1000;
                                Toast.makeText(this, "Locked. Try again in " + sec + "s.", Toast.LENGTH_SHORT).show();
                                pinFlowInFlight = false;
                                // dialog will close after this click; onDismiss will clear pinDialog
                                break;
                            }

                            case BAD_PIN:
                            default:
                                Toast.makeText(this, "Bad PIN. Attempts: " + res.failedAttempts, Toast.LENGTH_SHORT).show();
                                // keep gate true and re-prompt
                                showVerifyPinDialog(employeeId);
                                break;
                        }
                    });
                })
                .create();

        pinDialog.setOnDismissListener(d -> {
            pinDialog = null;
            // if user somehow dismisses (system), don’t leave the gate stuck
            pinFlowInFlight = false;
        });
        pinDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);
        boolean alreadySelected = ActiveEmployeeManager.hasActiveEmployee(this);
        // If selecting and nothing selected yet no menu.
        if (selectMode && !alreadySelected) return false;
        getMenuInflater().inflate(R.menu.menu_employee_list, menu);
        AdminMenu.inflateKioskActions(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean selectMode = getIntent().getBooleanExtra("selectMode", false);
        boolean alreadySelected = ActiveEmployeeManager.hasActiveEmployee(this);
        // If selecting and nothing selected yet no menu.
        if (selectMode && !alreadySelected) return false;
        AdminMenu.setVisibility(this, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.employeeSearch) { startActivity(new Intent(this, EmployeeSearch.class)); return true; }
        if (item.getItemId() == R.id.employeeDetails) { startActivity(new Intent(this, EmployeeDetails.class)); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        pinFlowInFlight = false;
        if (pinDialog != null) {
            pinDialog.dismiss();
            pinDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        pinFlowInFlight = false;
        if (pinDialog != null) {
            pinDialog.dismiss();
            pinDialog = null;
        }
        super.onDestroy();
    }
}
