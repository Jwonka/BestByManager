package com.example.bestbymanager.UI.activities;

import static com.example.bestbymanager.utilities.LocalDateBinder.bindDateField;
import static com.example.bestbymanager.utilities.LocalDateBinder.parseOrToday;
import static com.example.bestbymanager.utilities.LocalDateBinder.stripString;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.database.ProductDatabaseBuilder;
import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    Button enterStartDate;
    Button enterEndDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        ProductDatabaseBuilder.getDatabase(this);

        enterStartDate = findViewById(R.id.start_date);
        enterEndDate = findViewById(R.id.end_date);

        LocalDate today = LocalDate.now();

        bindDateField(enterStartDate, this, today);
        bindDateField(enterEndDate, this, today);

        Button searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            String startDateString = stripString(enterStartDate);
            String endDateString = stripString(enterEndDate);

            if (startDateString.equals("Start Date") || endDateString.equals("End Date")) {
                Toast.makeText(this, "Please choose a start and end date.", Toast.LENGTH_SHORT).show();
                return;
            }

            LocalDate start = parseOrToday(startDateString);
            LocalDate end = parseOrToday(endDateString);

            if (end.isBefore(start)) {
                Toast.makeText(this, "End date must be after the start date.", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, ExpiredProductReport.class)
                    .putExtra("startDate", startDateString)
                    .putExtra("endDate", endDateString);
            startActivity(intent);
        });

        Button productListButton = findViewById(R.id.product_list_button);
        productListButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProductList.class);
            startActivity(intent);
        });

        Button logOutButton = findViewById(R.id.logout_button);
        logOutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}