package com.example.bestbymanager.UI.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
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

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityProductReportBinding binding = ActivityProductReportBinding.inflate(getLayoutInflater());
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

        Bundle args = new Bundle();
        args.putString("startDate", getIntent().getStringExtra("startDate"));
        args.putString("endDate",   getIntent().getStringExtra("endDate"));
        args.putString("mode",      getIntent().getStringExtra("mode"));
        args.putString("barcode",   getIntent().getStringExtra("barcode"));
        args.putString("allProducts",   getIntent().getStringExtra("allProducts"));

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
        String text = resolveReportLabel(rows) + "\n\n" + buildReadableReport(rows);

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Report", text));
        Toast.makeText(this, "Copied: " + resolveReportLabel(rows), Toast.LENGTH_SHORT).show();
    }

    private void shareReport() {
        List<ProductReportRow> rows = prAdapter.getCurrentProductList();
        String text = resolveReportLabel(rows) + "\n\n" + buildReadableReport(rows);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, resolveReportLabel(rows));
        share.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(share, "Share report via"));
    }

    private String buildReadableReport(List<? extends ProductReportRow> rows) {
        if (rows.isEmpty()) { return "No results."; }

        StringBuilder sb = new StringBuilder();
        for (ProductReportRow r : rows) {
            sb.append("============================\n");
            sb.append("Brand: ").append(r.brand).append("\n");
            sb.append("Product: ").append(r.productName).append("\n");
            sb.append("Barcode: ").append(r.barcode).append("\n");
            sb.append("Purchase Date: ").append(r.purchaseDate).append("\n");
            sb.append("Expiration Date: ").append(r.expirationDate).append("\n");
            sb.append("Quantity: ").append(r.quantity).append("\n");
            sb.append("Entered By: ").append(r.enteredBy).append("\n");
            sb.append("-------------------------------------------------------\n\n");
        }
        return sb.toString();
    }

    private String resolveReportLabel(List<ProductReportRow> rows) {
        Intent in = getIntent();
        String mode = in.getStringExtra("mode");
        String barcode = in.getStringExtra("barcode");
        String start = in.getStringExtra("startDate");
        String end = in.getStringExtra("endDate");

        if ("expired".equals(mode)) { return "Expired Products Report"; }
        if ("expiring".equals(mode)) { return "Products Expiring Soon"; }
        if ("allProducts".equals(mode)) return "All Products – Full Inventory";

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
