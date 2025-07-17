package com.example.bestbymanager.UI.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.adapter.UserReportAdapter;
import com.example.bestbymanager.UI.authentication.BaseAdminActivity;
import com.example.bestbymanager.data.pojo.UserReportRow;
import com.example.bestbymanager.databinding.ActivityUserReportBinding;
import com.example.bestbymanager.viewmodel.UserReportViewModel;
import java.util.List;
import java.util.Objects;

public class UserReport extends BaseAdminActivity {
    private UserReportAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.results);
        ActivityUserReportBinding binding = ActivityUserReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle args = new Bundle();
        args.putString("startDate", getIntent().getStringExtra("startDate"));
        args.putString("endDate", getIntent().getStringExtra("endDate"));
        args.putString("mode", getIntent().getStringExtra("mode"));
        args.putString("barcode", getIntent().getStringExtra("barcode"));
        args.putString("allEntries", getIntent().getStringExtra("allEntries"));
        args.putLong("user", getIntent().getLongExtra("user", -1));

        UserReportViewModel userViewModel = new ViewModelProvider(
                this,
                new SavedStateViewModelFactory(getApplication(), this, args)
        ).get(UserReportViewModel.class);

        userAdapter = new UserReportAdapter(id ->
                startActivity(new Intent(this, UserDetails.class)
                        .putExtra("userID", id)));

        binding.employeeReportRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.employeeReportRecycler.setAdapter(userAdapter);

        userViewModel.getReport().observe(this, userAdapter::setUserList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_report, menu);
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
        } else if (item.getItemId() == R.id.employeeList) {
            Intent intent = new Intent(this, UserList.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            copyToClipboard();
            return true;
        } else if (item.getItemId() == R.id.action_share) {
            shareReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void copyToClipboard() {
        List<? extends UserReportRow> rows = userAdapter.getCurrentUserList();
        String text = generateReportTitle(rows) + "\n\n" + buildReadableUserReport(rows);

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Report", text));
        Toast.makeText(this, "Report copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareReport() {
        List<? extends UserReportRow> rows = userAdapter.getCurrentUserList();
        String text = generateReportTitle(rows) + "\n\n" + buildReadableUserReport(rows);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        share.putExtra(Intent.EXTRA_SUBJECT, generateReportTitle(rows));
        startActivity(Intent.createChooser(share, "Share report via"));
    }

    private String buildReadableUserReport(List<? extends UserReportRow> rows) {
        if (rows.isEmpty()) { return "No results."; }

        StringBuilder sb = new StringBuilder();
        UserReportRow currentUser = null;

        for (UserReportRow r : rows) {
            if (r.isHeader) {
                currentUser = r;
                sb.append("============================\n");
                sb.append("Employee: ").append(r.firstName).append(" ").append(r.lastName).append("\n");
                sb.append("Username: ").append(r.userName).append("\n");
                sb.append("-------------------------------------------------------\n");
            } else if (r.isFooter) {
                if (currentUser != null) {
                    sb.append("------------------- Summary -------------------\n");
                    sb.append("Total Good: ").append(r.goodCount).append("\n");
                    sb.append("Total Expired: ").append(r.expiredCount).append("\n");
                    sb.append("Grand Total: ").append(r.totalCount).append("\n");
                    sb.append("============================\n\n");
                }
            } else {
                sb.append("Product: ").append(r.productName).append("\n");
                sb.append("Brand: ").append(r.brand).append("\n");
                sb.append("Good: ").append(r.goodCount).append("\n");
                sb.append("Expired: ").append(r.expiredCount).append("\n");
                sb.append("Total: ").append(r.totalCount).append("\n\n");
            }
        }

        return sb.toString();
    }

    private String generateReportTitle(List<? extends UserReportRow> rows) {
        String mode = getIntent().getStringExtra("mode");
        String barcode = getIntent().getStringExtra("barcode");
        String startDate = getIntent().getStringExtra("startDate");
        String endDate = getIntent().getStringExtra("endDate");
        String fullName = "selected employee";
        String productName = "";

        for (UserReportRow row : rows) {
            if (row.isHeader && (row.firstName != null || row.lastName != null)) {
                fullName = row.firstName + " " + row.lastName;
            } else if (!row.isFooter && productName.isEmpty()) {
                productName = row.productName != null ? row.productName : "";
            }
        }

        StringBuilder title = new StringBuilder("Employee Report");

        switch (Objects.requireNonNull(mode)) {
            case "allEntries":
                title.append(" for all employees (all-time)");
                break;
            case "range":
                title.append(" from ").append(startDate).append(" to ").append(endDate);
                break;
            case "user":
                title.append(" for ").append(fullName);
                break;
            case "range-user":
                title.append(" for ").append(fullName)
                        .append(" from ").append(startDate).append(" to ").append(endDate);
                break;
            case "barcode":
                title.append(" for product: ").append(productName).append(" ~ ").append(barcode);
                break;
            case "barcode-user":
                title.append(" for ").append(fullName)
                        .append(" on product: ").append(productName).append(" ~ ").append(barcode);
                break;
            case "barcode-range":
                title.append(" for product: ").append(productName).append(" ~ ").append(barcode)
                        .append(" from ").append(startDate).append(" to ").append(endDate);
                break;
            case "barcode-range-user":
                title.append(" for ").append(fullName).append(" on product: ")
                        .append(productName).append(" ~ ").append(barcode)
                        .append(" from ").append(startDate).append(" to ").append(endDate);
                break;
        }
        return title.toString();
    }
}
