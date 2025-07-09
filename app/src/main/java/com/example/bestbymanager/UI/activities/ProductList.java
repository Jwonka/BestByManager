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
import com.example.bestbymanager.UI.adapter.ProductAdapter;
import com.example.bestbymanager.databinding.ActivityProductListBinding;
import com.example.bestbymanager.viewmodel.ProductListViewModel;
import java.time.LocalDate;
import java.util.Collections;

public class ProductList extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.product_list);
        ActivityProductListBinding binding = ActivityProductListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.productDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ProductList.this, ProductDetails.class);
            startActivity(intent);
        });

        final ProductAdapter productAdapter = new ProductAdapter((productID) -> {
            Intent intent = new Intent(this, ProductDetails.class)
                    .putExtra("productID", productID);
            startActivity(intent);
        });
        binding.productListRecyclerView.setAdapter(productAdapter);
        binding.productListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ProductListViewModel productListViewModel = new ViewModelProvider(this).get(ProductListViewModel.class);

        productListViewModel.getProducts(LocalDate.now()).observe(this, list -> {
            if (list == null) { return; }
            if (list.isEmpty()) {
                binding.noProductsMessage.setVisibility(View.VISIBLE);
                productAdapter.setProducts(Collections.emptyList());
                return;
            }
            binding.noProductsMessage.setVisibility(View.GONE);
            productAdapter.setProducts(list);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_list, menu);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() { super.onResume(); }
}
