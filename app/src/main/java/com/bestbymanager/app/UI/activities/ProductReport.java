package com.bestbymanager.app.UI.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.adapter.ProductReportAdapter;
import com.bestbymanager.app.UI.authentication.BaseEmployeeRequiredActivity;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.data.pojo.ProductReportRow;
import com.bestbymanager.app.databinding.ActivityProductReportBinding;
import com.bestbymanager.app.databinding.DialogDiscardExpiredBinding;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.viewmodel.ProductReportViewModel;
import java.util.List;

public class ProductReport extends BaseEmployeeRequiredActivity {
    private ProductReportViewModel prViewModel;

    private static final String EXTRA_START_DATE   = "startDate";
    private static final String EXTRA_END_DATE     = "endDate";
    private static final String EXTRA_MODE         = "mode";
    private static final String EXTRA_BARCODE      = "barcode";
    private static final String EXTRA_ALL_PRODUCTS = "allProducts";

    private boolean emptyToastShown = false;
    private ProductReportAdapter prAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // non-UI init only (safe before gate)
        Intent in = getIntent();
        Bundle args = new Bundle();
        args.putString(EXTRA_START_DATE, in.getStringExtra(EXTRA_START_DATE));
        args.putString(EXTRA_END_DATE, in.getStringExtra(EXTRA_END_DATE));
        args.putString(EXTRA_MODE, in.getStringExtra(EXTRA_MODE));
        args.putString(EXTRA_BARCODE, in.getStringExtra(EXTRA_BARCODE));
        args.putBoolean(EXTRA_ALL_PRODUCTS, in.getBooleanExtra(EXTRA_ALL_PRODUCTS, false));

        prViewModel = new ViewModelProvider(
                this,
                new SavedStateViewModelFactory(getApplication(), this, args)
        ).get(ProductReportViewModel.class);
    }

    @Override
    protected void onGatePassed() {
        setTitle(R.string.results);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityProductReportBinding binding = ActivityProductReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final View rootView = binding.getRoot();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prAdapter = new ProductReportAdapter(
                id -> startActivity(new Intent(this, ProductDetails.class).putExtra("productID", id)),
                row -> {
                    boolean expiredOrToday = !row.expirationDate.isAfter(java.time.LocalDate.now());
                    if (!expiredOrToday) { Toast.makeText(this, "Not expired yet.", Toast.LENGTH_SHORT).show(); return; }
                    if (row.quantity <= 0) { Toast.makeText(this, "No on-hand quantity to discard.", Toast.LENGTH_SHORT).show(); return; }
                    showDiscardDialog(row, prViewModel);
                }
        );

        binding.reportRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.reportRecycler.setAdapter(prAdapter);

        prViewModel.getReport().observe(this, rows -> {
            prAdapter.setReportList(rows);

            if ((rows == null || rows.isEmpty()) && !emptyToastShown) {
                emptyToastShown = true;
                Toast.makeText(this, "No results.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_report, menu);
        AdminMenu.inflateIfAdmin(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean activeIsAdmin = ActiveEmployeeManager.isActiveEmployeeAdmin(this);
        MenuItem adminItem = menu.findItem(R.id.adminPage);

        if (adminItem != null) adminItem.setVisible(activeIsAdmin);
        AdminMenu.setVisibility(this, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { this.finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.productSearch) { startActivity(new Intent(this, ProductSearch.class)); return true; }
        if (item.getItemId() == R.id.employeeDetails) { startActivity(new Intent(this, ProductDetails.class)); return true; }
        if (item.getItemId() == R.id.productList) { startActivity(new Intent(this, ProductList.class)); return true; }
        if (item.getItemId() == R.id.action_copy) { copyToClipboard(); return true; }
        if (item.getItemId() == R.id.action_share) { shareReport(); return true; }
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
        if (rows.isEmpty()) return "No results.";

        StringBuilder sb = new StringBuilder();
        for (ProductReportRow r : rows) {
            int discarded = r.discardedQuantity == null ? 0 : r.discardedQuantity;
            sb.append("============================\n");
            sb.append("Brand: ").append(r.brand).append("\n");
            sb.append("Product: ").append(r.productName).append("\n");
            sb.append("Barcode: ").append(r.barcode).append("\n");
            sb.append("Purchase Date: ").append(r.purchaseDate).append("\n");
            sb.append("Expiration Date: ").append(r.expirationDate).append("\n");
            sb.append("Quantity: ").append(r.quantity).append("\n");
            sb.append("Discarded: ").append(discarded).append("\n");
            if (r.lastDiscardNote != null) sb.append("Discard Note: ").append(r.lastDiscardNote).append("\n");
            sb.append("Entered By: ").append(r.enteredBy).append("\n");
            sb.append("-------------------------------------------------------\n\n");
        }
        return sb.toString();
    }

    private void showDiscardDialog(ProductReportRow row, ProductReportViewModel viewModel) {
        if (row.quantity <= 0) {
            Toast.makeText(this, "No on-hand quantity to discard.", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogDiscardExpiredBinding b = DialogDiscardExpiredBinding.inflate(getLayoutInflater());
        b.discardQuantity.setText(String.valueOf(row.quantity));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Discard expired items")
                .setView(b.getRoot())
                .setNegativeButton(android.R.string.cancel, (d, w) -> {})
                .setPositiveButton("Discard", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String qtyRaw = b.discardQuantity.getText() == null ? "" : b.discardQuantity.getText().toString().trim();
            if (TextUtils.isEmpty(qtyRaw)) {
                b.discardQuantityLayout.setError("Quantity required.");
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(qtyRaw);
            } catch (NumberFormatException ex) {
                b.discardQuantityLayout.setError("Invalid quantity.");
                return;
            }

            if (qty <= 0) {
                b.discardQuantityLayout.setError("Quantity must be greater than 0.");
                return;
            }
            if (qty > row.quantity) {
                b.discardQuantityLayout.setError("Cannot discard more than on-hand.");
                return;
            }

            b.discardQuantityLayout.setError(null);

            String reason = b.discardReason.getText() == null ? null : b.discardReason.getText().toString().trim();
            if (reason != null && reason.isEmpty()) reason = null;

            long employeeId = ActiveEmployeeManager.getActiveEmployeeId(ProductReport.this);
            viewModel.discardExpiredProduct(row.productID, qty, reason, employeeId);
            dialog.dismiss();
        }));

        dialog.show();
    }

    private String resolveReportLabel(List<ProductReportRow> rows) {
        Intent in = getIntent();
        String mode = in.getStringExtra("mode");
        String barcode = in.getStringExtra("barcode");
        String start = in.getStringExtra("startDate");
        String end = in.getStringExtra("endDate");
        boolean allProducts = in.getBooleanExtra(EXTRA_ALL_PRODUCTS, false);

        if ("expired".equals(mode)) return "Expired Products Report";
        if ("expiring".equals(mode)) return "Products Expiring Soon";
        if (allProducts || "allProducts".equals(mode)) return "All Products – Full Inventory";

        if (barcode != null && start == null && end == null) {
            String name = rows.isEmpty() ? barcode : rows.get(0).productName;
            return "Product Report - " + name;
        }
        if (barcode == null && start != null && end != null) return "Product Report (" + start + " to " + end + ")";
        if (barcode != null && start != null && end != null) {
            String name = rows.isEmpty() ? barcode : rows.get(0).productName;
            return "Product Report – " + name + " (" + start + " to " + end + ")";
        }
        return "Product Report";
    }

    @Override
    protected void onResume() {
        super.onResume();
        prViewModel.refresh();
    }
}