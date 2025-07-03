package com.example.bestbymanager.UI.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.adapter.ProductReportAdapter;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.databinding.ActivityProductReportBinding;
import com.example.bestbymanager.viewmodel.ProductReportViewModel;

public class ProductReport extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.product_report);
        ActivityProductReportBinding binding = ActivityProductReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Bundle args = new Bundle();
        args.putString("startDate", getIntent().getStringExtra("startDate"));
        args.putString("endDate",   getIntent().getStringExtra("endDate"));
        args.putString("mode",      getIntent().getStringExtra("mode"));
        args.putString("barcode",   getIntent().getStringExtra("barcode"));

        ProductReportViewModel prViewModel = new ViewModelProvider(
                this,
                new SavedStateViewModelFactory(getApplication(), this, args)
        ).get(ProductReportViewModel.class);

        ProductReportAdapter prAdapter = new ProductReportAdapter(id ->
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
        } else if (item.getItemId() == R.id.productList) {
            Intent intent = new Intent(this, ProductList.class);
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
}
