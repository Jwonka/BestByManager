package com.bestbymanager.app.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bestbymanager.app.R;
import com.bestbymanager.app.databinding.ActivityAboutBinding;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.utilities.AdminMenu;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.about);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
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

        TextView email = findViewById(R.id.dev_email);
        TextView linkedin = findViewById(R.id.dev_linkedin);
        TextView license = findViewById(R.id.license_footer);

        email.setText(Html.fromHtml(getString(R.string.dev_email), Html.FROM_HTML_MODE_LEGACY));
        email.setMovementMethod(LinkMovementMethod.getInstance());

        linkedin.setText(Html.fromHtml(getString(R.string.dev_linkedin), Html.FROM_HTML_MODE_LEGACY));
        linkedin.setMovementMethod(LinkMovementMethod.getInstance());

        license.setText(Html.fromHtml(getString(R.string.license_footer), Html.FROM_HTML_MODE_LEGACY));
        license.setMovementMethod(LinkMovementMethod.getInstance());

        email.setLinkTextColor(ContextCompat.getColor(this, R.color.dark_green));
        linkedin.setLinkTextColor(ContextCompat.getColor(this, R.color.dark_green));
        license.setLinkTextColor(ContextCompat.getColor(this, R.color.dark_green));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about_activity, menu);
        AdminMenu.inflateIfAdmin(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isAdmin = Session.get().currentUserIsAdmin();
        menu.findItem(R.id.adminPage).setVisible(isAdmin);
        AdminMenu.setVisibility(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { this.finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}
