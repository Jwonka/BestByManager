package com.bestbymanager.app.UI.activities;

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
import com.bestbymanager.app.R;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.databinding.ActivityMainBinding;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private boolean selectingEmployee = false;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Session.get().preload(this);

        setTitle(R.string.main_screen);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
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
            if (ActiveEmployeeManager.getActiveEmployeeId(this) <= 0) {
                Toast.makeText(this, "Select an employee first.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, UserList.class).putExtra("selectMode", true));
                return;
            }
            startActivity(new Intent(MainActivity.this, ProductDetails.class));
        });

        binding.productSearchButton.setOnClickListener(v -> {
            if (ActiveEmployeeManager.getActiveEmployeeId(this) <= 0) {
                Toast.makeText(this, "Select an employee first.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, UserList.class).putExtra("selectMode", true));
                return;
            }
            startActivity(new Intent(MainActivity.this, ProductSearch.class));
        });

        binding.productListButton.setOnClickListener(v -> {
            if (ActiveEmployeeManager.getActiveEmployeeId(this) <= 0) {
                Toast.makeText(this, "Select an employee first.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, UserList.class).putExtra("selectMode", true));
                return;
            }
            startActivity(new Intent(MainActivity.this, ProductList.class));
        });

        binding.employeeListButton.setOnClickListener(v -> startActivity(new Intent(this, UserList.class).putExtra("selectMode", true)));

        binding.aboutButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
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
        if (item.getItemId() == R.id.adminPage) {
            Intent intent = new Intent(this, AdministratorActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        io.execute(() -> {
            Repository repo = new Repository(getApplication());
            int count = repo.userCountBlocking();

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (count == 0) {
                    selectingEmployee = false;
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                long activeId = ActiveEmployeeManager.getActiveEmployeeId(this);
                if (activeId <= 0 && !selectingEmployee) {
                    selectingEmployee = true;
                    startActivity(new Intent(this, UserList.class).putExtra("selectMode", true));
                } else if (activeId > 0) {
                    selectingEmployee = false;
                }
            });
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}