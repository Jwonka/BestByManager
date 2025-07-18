package com.example.bestbymanager.UI.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.bestbymanager.R;
import com.example.bestbymanager.databinding.ActivityAboutBinding;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.about);
        ActivityAboutBinding binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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
        }
        return super.onOptionsItemSelected(item);
    }
}
