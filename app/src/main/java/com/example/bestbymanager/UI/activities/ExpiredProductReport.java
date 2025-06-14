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
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.adapter.ExpiredProductReportAdapter;
import com.example.bestbymanager.viewmodel.ExpiredProductReportViewModel;

public class ExpiredProductReport extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_expired_product_report);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        String from = getIntent().getStringExtra("startDate");
        String to = getIntent().getStringExtra("endDate");

        Bundle args = new Bundle();
        args.putString("startDate", from);
        args.putString("endDate", to);

        ExpiredProductReportViewModel eprViewModel = new ViewModelProvider(
                this,
                new SavedStateViewModelFactory(getApplication(), this, args)
        ).get(ExpiredProductReportViewModel.class);

        ExpiredProductReportAdapter eprAdapter = new ExpiredProductReportAdapter(id ->
                startActivity(new Intent(this, ProductDetails.class)
                        .putExtra("productID", id)));

        RecyclerView recyclerView = findViewById(R.id.report_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(eprAdapter);

        eprViewModel.getResults().observe(this, eprAdapter::setReportList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_expired_product_report, menu);
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
        }
        return super.onOptionsItemSelected(item);
    }
}
