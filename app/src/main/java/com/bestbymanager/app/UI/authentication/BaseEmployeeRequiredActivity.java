package com.bestbymanager.app.UI.authentication;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.bestbymanager.app.UI.activities.LoginActivity;
import com.bestbymanager.app.UI.activities.UserList;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.session.Session;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseEmployeeRequiredActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onResume() {
        super.onResume();

        Session.get().preload(this);

        io.execute(() -> {
            Repository repo = new Repository(getApplication());
            int count = repo.userCountBlocking();

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (count == 0) {
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return;
                }

                if (!ActiveEmployeeManager.hasActiveEmployee(this)) {
                    startActivity(new Intent(this, UserList.class)
                            .putExtra("selectMode", true)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
