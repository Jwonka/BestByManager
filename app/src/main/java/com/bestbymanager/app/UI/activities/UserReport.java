package com.bestbymanager.app.UI.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.adapter.UserReportAdapter;
import com.bestbymanager.app.UI.authentication.BaseAdminActivity;
import com.bestbymanager.app.data.pojo.UserReportRow;
import com.bestbymanager.app.databinding.ActivityUserReportBinding;
import com.bestbymanager.app.viewmodel.UserReportViewModel;
import java.util.List;

public class UserReport extends BaseAdminActivity {
    private static final String EXTRA_START_DATE   = "startDate";
    private static final String EXTRA_END_DATE     = "endDate";
    private static final String EXTRA_MODE         = "mode";
    private static final String EXTRA_BARCODE      = "barcode";
    private static final String EXTRA_ALL_ENTRIES  = "allEntries";
    private static final String EXTRA_USER         = "user";
    private static final String MODE_ALL_ENTRIES         = "allEntries";
    private static final String MODE_RANGE               = "range";
    private static final String MODE_USER                = "user";
    private static final String MODE_RANGE_USER           = "range-user";
    private static final String MODE_BARCODE             = "barcode";
    private static final String MODE_BARCODE_USER         = "barcode-user";
    private static final String MODE_BARCODE_RANGE        = "barcode-range";
    private static final String MODE_BARCODE_RANGE_USER   = "barcode-range-user";
    private static final String MODE_UNKNOWN              = "unknown";

    private UserReportAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.results);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityUserReportBinding binding = ActivityUserReportBinding.inflate(getLayoutInflater());
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

        // Build args ONCE, using the same keys the ViewModel reads from SavedStateHandle
        Intent intent = getIntent();
        Bundle args = new Bundle();
        args.putString(EXTRA_START_DATE, intent.getStringExtra(EXTRA_START_DATE));
        args.putString(EXTRA_END_DATE, intent.getStringExtra(EXTRA_END_DATE));
        args.putString(EXTRA_MODE, intent.getStringExtra(EXTRA_MODE));
        args.putString(EXTRA_BARCODE, intent.getStringExtra(EXTRA_BARCODE));
        args.putString(EXTRA_ALL_ENTRIES, intent.getStringExtra(EXTRA_ALL_ENTRIES));
        args.putLong(EXTRA_USER, intent.getLongExtra(EXTRA_USER, -1));

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
            finish();
            return true;
        } else if (item.getItemId() == R.id.mainScreen) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        } else if (item.getItemId() == R.id.employeeSearch) {
            startActivity(new Intent(this, UserSearch.class));
            return true;
        } else if (item.getItemId() == R.id.employeeDetails) {
            startActivity(new Intent(this, UserDetails.class));
            return true;
        } else if (item.getItemId() == R.id.employeeList) {
            startActivity(new Intent(this, UserList.class));
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
        String title = generateReportTitle(rows);
        String text = title + "\n\n" + buildReadableUserReport(rows);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        startActivity(Intent.createChooser(share, "Share report via"));
    }

    private String buildReadableUserReport(List<? extends UserReportRow> rows) {
        if (rows.isEmpty()) return "No results.";

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
                    int discarded = r.discardedCount == null ? 0 : r.discardedCount;
                    int lots = r.lotCount == null ? 0 : r.lotCount;

                    sb.append("------------------- Summary -------------------\n");
                    sb.append("Good Units: ").append(r.goodCount).append("\n");
                    sb.append("Expired Units: ").append(r.expiredCount).append("\n");
                    sb.append("Discarded Units: ").append(discarded).append("\n");
                    if (r.lastDiscardNote != null) sb.append("Discard Note: ").append(r.lastDiscardNote).append("\n");
                    sb.append("Total Units: ").append(r.totalCount).append(" (Lots: ").append(lots).append(")\n");
                    sb.append("============================\n\n");
                }
            } else {
                int discarded = r.discardedCount == null ? 0 : r.discardedCount;
                int lots = r.lotCount == null ? 0 : r.lotCount;

                sb.append("Product: ").append(r.productName).append("\n");
                sb.append("Brand: ").append(r.brand).append("\n");
                sb.append("Good Units: ").append(r.goodCount).append("\n");
                sb.append("Expired Units: ").append(r.expiredCount).append("\n");
                sb.append("Discarded Units: ").append(discarded).append("\n");
                if (r.lastDiscardNote != null) sb.append("Discard Note: ").append(r.lastDiscardNote).append("\n");
                sb.append("Total Units: ").append(r.totalCount).append(" (Lots: ").append(lots).append(")\n\n");
            }
        }

        return sb.toString();
    }

    private String generateReportTitle(List<? extends UserReportRow> rows) {
        Intent intent = getIntent();

        String mode = intent.getStringExtra(EXTRA_MODE);
        if (mode == null && intent.hasExtra(EXTRA_ALL_ENTRIES)) mode = MODE_ALL_ENTRIES;
        if (mode == null) mode = MODE_UNKNOWN;

        String barcode = intent.getStringExtra(EXTRA_BARCODE);
        String startDate = intent.getStringExtra(EXTRA_START_DATE);
        String endDate = intent.getStringExtra(EXTRA_END_DATE);

        String fullName = "selected employee";
        String productName = "";

        for (UserReportRow row : rows) {
            if (row.isHeader && (row.firstName != null || row.lastName != null)) {
                String fn = row.firstName == null ? "" : row.firstName;
                String ln = row.lastName == null ? "" : row.lastName;
                fullName = (fn + " " + ln).trim();
            } else if (!row.isFooter && productName.isEmpty()) {
                productName = row.productName != null ? row.productName : "";
            }
        }

        StringBuilder title = new StringBuilder("Employee Report");

        switch (mode) {
            case MODE_ALL_ENTRIES:
                title.append(" for all employees (all-time)");
                break;
            case MODE_RANGE:
                title.append(" from ").append(startDate).append(" to ").append(endDate);
                break;
            case MODE_USER:
                title.append(" for ").append(fullName);
                break;
            case MODE_RANGE_USER:
                title.append(" for ").append(fullName)
                        .append(" from ").append(startDate).append(" to ").append(endDate);
                break;
            case MODE_BARCODE:
                title.append(" for product: ").append(productName).append(" ~ ").append(barcode);
                break;
            case MODE_BARCODE_USER:
                title.append(" for ").append(fullName)
                        .append(" on product: ").append(productName).append(" ~ ").append(barcode);
                break;
            case MODE_BARCODE_RANGE:
                title.append(" for product: ").append(productName).append(" ~ ").append(barcode)
                        .append(" from ").append(startDate).append(" to ").append(endDate);
                break;
            case MODE_BARCODE_RANGE_USER:
                title.append(" for ").append(fullName).append(" on product: ")
                        .append(productName).append(" ~ ").append(barcode)
                        .append(" from ").append(startDate).append(" to ").append(endDate);
                break;
            default:
                break;
        }

        return title.toString();
    }
}