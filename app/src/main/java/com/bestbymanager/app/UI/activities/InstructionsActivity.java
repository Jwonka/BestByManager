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
import com.bestbymanager.app.databinding.ActivityInstructionsBinding;
import com.bestbymanager.app.session.ActiveEmployeeManager;
import com.bestbymanager.app.utilities.AdminMenu;

public class InstructionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.instructions);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityInstructionsBinding binding = ActivityInstructionsBinding.inflate(getLayoutInflater());
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

        // Wire the FAQ hyperlink
        TextView faqLink = binding.faqLink;
        faqLink.setText(Html.fromHtml(getString(R.string.instructions_faq_link), Html.FROM_HTML_MODE_LEGACY));
        faqLink.setMovementMethod(LinkMovementMethod.getInstance());
        faqLink.setLinkTextColor(ContextCompat.getColor(this, R.color.dark_green));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about_activity, menu);
        AdminMenu.inflateKioskActions(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem adminItem = menu.findItem(R.id.adminPage);
        if (adminItem != null) adminItem.setVisible(ActiveEmployeeManager.isActiveEmployeeAdmin(this));
        AdminMenu.setVisibility(this, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        return super.onOptionsItemSelected(item);
    }
}