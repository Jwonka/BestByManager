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
        args.putString("endDate",   getIntent().getStringExtra("endDate"));
        args.putString("mode",      getIntent().getStringExtra("mode"));
        args.putString("barcode",   getIntent().getStringExtra("barcode"));

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
        String text = buildReportText(rows);

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Report", text));
        Toast.makeText(this, "Report copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private String buildReportText(List<? extends UserReportRow> rows) {
        StringBuilder sb = new StringBuilder();

        if (rows.isEmpty()) {
            return "No results.";
        }

        if (rows.get(0) != null) {
            for (UserReportRow r : rows) {
                sb.append("UserID: ").append(r.userID).append("\n")
                        .append("Username: ").append(r.userName).append("\n")
                        .append("Count: ").append(r.count).append("\n").append("\n");
            }
        }
        return sb.toString();
    }

    private void shareReport() {
        List<? extends UserReportRow> rows = userAdapter.getCurrentUserList();
        String text = buildReportText(rows);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.product_report));
        share.putExtra(Intent.EXTRA_EMAIL, new String[]{"manager@store.com"});
        startActivity(Intent.createChooser(share, "Share report via"));
    }
}
