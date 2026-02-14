package com.bestbymanager.app.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.adapter.ProductAdapter;
import com.bestbymanager.app.UI.authentication.BaseEmployeeRequiredActivity;
import com.bestbymanager.app.databinding.ActivityProductListBinding;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.viewmodel.ProductListViewModel;
import java.time.LocalDate;
import java.util.Collections;

public class ProductList extends BaseEmployeeRequiredActivity {

    private ProductListViewModel productListViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.product_list);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityProductListBinding binding = ActivityProductListBinding.inflate(getLayoutInflater());
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

        productListViewModel = new ViewModelProvider(this).get(ProductListViewModel.class);

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
        AdminMenu.inflateIfAdmin(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        AdminMenu.setVisibility(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { this.finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.productSearch) { startActivity(new Intent(this, ProductSearch.class)); return true; }
        if (item.getItemId() == R.id.productDetails) { startActivity(new Intent(this, ProductDetails.class)); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() { super.onResume(); productListViewModel.refresh();}
}
