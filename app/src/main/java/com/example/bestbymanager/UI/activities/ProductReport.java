package com.example.bestbymanager.UI.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.adapter.ProductReportAdapter;
import com.example.bestbymanager.data.pojo.ProductReportRow;
import com.example.bestbymanager.databinding.ActivityProductReportBinding;
import com.example.bestbymanager.viewmodel.ProductReportViewModel;
import java.util.List;

public class ProductReport extends AppCompatActivity {
    private ProductReportAdapter prAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.results);
        ActivityProductReportBinding binding = ActivityProductReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle args = new Bundle();
        args.putString("startDate", getIntent().getStringExtra("startDate"));
        args.putString("endDate",   getIntent().getStringExtra("endDate"));
        args.putString("mode",      getIntent().getStringExtra("mode"));
        args.putString("barcode",   getIntent().getStringExtra("barcode"));
        Log.d("REPORT-EXTRA", "barcode=" + getIntent().getStringExtra("barcode"));
        ProductReportViewModel prViewModel = new ViewModelProvider(
                this,
                new SavedStateViewModelFactory(getApplication(), this, args)
        ).get(ProductReportViewModel.class);

        prAdapter = new ProductReportAdapter(id ->
                startActivity(new Intent(this, ProductDetails.class)
                        .putExtra("productID", id)));

        binding.reportRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.reportRecycler.setAdapter(prAdapter);

        prViewModel.getReport().observe(this, prAdapter::setReportList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_report, menu);
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
        } else if (item.getItemId() == R.id.productSearch) {
            Intent intent = new Intent(this, ProductSearch.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.employeeDetails) {
            Intent intent = new Intent(this, ProductDetails.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.productList) {
            Intent intent = new Intent(this, ProductList.class);
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
        List<ProductReportRow> rows = prAdapter.getCurrentProductList();
        String header = resolveReportLabel(rows);
        String text = header + "\n\n" + buildReportText(rows);

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Report", text));
        Toast.makeText(this, "Copied: " + header, Toast.LENGTH_SHORT).show();
    }

    private String buildReportText(List<? extends ProductReportRow> rows) {
        StringBuilder sb = new StringBuilder();

        if (rows.isEmpty()) {
            return "No results.";
        }

        if (rows.get(0) != null) {
            sb.append("Brand\tProduct Name\tBarcode\tCategory\tExpiration Date\tQuantity\tEmployee\n");
            for (ProductReportRow r : rows) {
                sb.append(escape(r.brand)).append("\t")
                        .append(escape(r.productName)).append("\t")
                        .append(escape(r.barcode)).append('\t')
                        .append(escape(r.category)).append('\t')
                        .append(r.expirationDate).append("\t")
                        .append(r.quantity).append("\t")
                        .append(escape(r.enteredBy)).append("\n");
            }
        }
        return sb.toString();
    }
    private static String escape(String s) {
        return s == null ? "" : s.replace("\t", " ").replace("\n", " ");
    }

    private void shareReport() {
        List<ProductReportRow> rows = prAdapter.getCurrentProductList();
        String subject = resolveReportLabel(rows);
        String body = buildReportText(rows);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, subject);
        share.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(share, "Share..."));
    }

    private String resolveReportLabel(List<ProductReportRow> rows) {
        Intent in   = getIntent();
        String mode = in.getStringExtra("mode");
        String barcode    = in.getStringExtra("barcode");
        String start   = in.getStringExtra("startDate");
        String end    = in.getStringExtra("endDate");

        if ("expired".equals(mode))    return "Expired Products Report";
        if ("expiring".equals(mode))   return "Products Expiring Soon";

        if (barcode != null && start == null && end == null) {
            String name = rows.isEmpty() ? barcode : rows.get(0).productName;
            return "Product Report - " + name;
        }

        if (barcode == null && start != null && end != null)
            return "Product Report (" + start + " to " + end + ")";

        if (barcode != null && start != null && end != null) {
            String name = rows.isEmpty() ? barcode : rows.get(0).productName;
            return "Product Report – " + name + " (" + start + " to " + end + ")";
        }

        return "Product Report";
    }
}
