package com.bestbymanager.app.UI.activities;

import static com.bestbymanager.app.utilities.PasswordUtil.generateTempPassword;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseAdminActivity;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.data.database.Converters;
import com.bestbymanager.app.data.entities.User;
import com.bestbymanager.app.databinding.ActivityUserDetailsBinding;
import com.bestbymanager.app.utilities.AdminMenu;
import com.bestbymanager.app.viewmodel.UserDetailsViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserDetails extends BaseAdminActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private UserDetailsViewModel userViewModel;
    private static final String TAG = "UserDetails";
    private static final int REQ_CAMERA = 10;
    private EditText firstName, lastName, username;
    private TextView adminLabel;
    Button saveButton, clearButton, password;
    SwitchMaterial modeSwitch;
    private User currentUser;
    private ImageView preview;
    private Uri imageUri;
    private byte[] thumbBlob;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private static final String STATE_IMAGE_URI = "IMAGE_URI";
    @androidx.annotation.Nullable
    private static Uri restoreImageUri(@androidx.annotation.Nullable Bundle s) {
        return (s == null) ? null : BundleCompat.getParcelable(s, STATE_IMAGE_URI, Uri.class);
    }

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        // BaseAdminActivity already enforces admin; this extra check only displays the custom toast/message.
        if (Session.get().isLoggedOut() || !Session.get().currentUserIsAdmin()) {
            Toast.makeText(this, "Owner admin required.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        imageUri = restoreImageUri(s);
        setTitle(R.string.employee_details);
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> {
                    if (!ok || imageUri == null) return;
                    handleImage(imageUri);
                });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityUserDetailsBinding binding = ActivityUserDetailsBinding.inflate(getLayoutInflater());
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

        firstName = binding.editFirstName;
        lastName = binding.editLastName;
        username = binding.editUsername;
        preview = binding.imagePreview;
        modeSwitch = binding.adminToggle;
        saveButton = binding.saveUserButton;
        clearButton = binding.clearEmployeeButton;
        adminLabel = binding.administratorLabel;
        password = binding.generateTempPwd;
        binding.imagePreview.setOnClickListener(v -> { if (ensureCameraPermission()) { launchCamera(); } });
        modeSwitch.setOnCheckedChangeListener((btn, isChecked) -> updateAdmin(isChecked));
        clearButton.setOnClickListener(v -> clearForm());
        saveButton.setOnClickListener(v -> saveUser());

        if (imageUri != null) { handleImage(imageUri); }

        password.setOnClickListener(v -> {
            if (currentUser == null) {
                toast("Save user first");
                return;
            }
            userViewModel.resetPassword(currentUser.getUserID())
                    .observe(this, temp -> {
                if (temp != null) {
                    showPasswordResetDialog(temp, currentUser);
                } else {
                    toast("Could not generate password.");
                }
            });
        });

        userViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(UserDetailsViewModel.class);

        userViewModel.getUser().observe(this, user -> {
            if (user == null) { return; }
            currentUser = user;
            populateForm(user);
            boolean isAdmin = user.isAdmin();
            modeSwitch.setChecked(isAdmin);
            thumbBlob = user.getThumbnail();
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_details, menu);
        AdminMenu.inflateIfAdmin(this, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.deleteEmployee).setVisible(currentUser != null);
        AdminMenu.setVisibility(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AdminMenu.handle(this, item)) { return true; }
        if (item.getItemId() == android.R.id.home) { this.finish(); return true; }
        if (item.getItemId() == R.id.mainScreen) { startActivity(new Intent(this, MainActivity.class)); return true; }
        if (item.getItemId() == R.id.employeeSearch) { startActivity(new Intent(this, UserSearch.class)); return true; }
        if (item.getItemId() == R.id.employeeList) { startActivity(new Intent(this, UserList.class)); return true; }
        if (item.getItemId() == R.id.deleteEmployee) {
            if (currentUser != null) {
                userViewModel.delete(currentUser);
                clearForm();
                toast("Employee deleted.");
            } else {
                toast("Error deleting employee.");
            }
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{ Manifest.permission.CAMERA },
                REQ_CAMERA);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(
    int req, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);

        if (req == REQ_CAMERA && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }

    private void launchCamera()  {
        try {
            File photoFile = File.createTempFile("user_", ".jpg", getCacheDir());
            Uri outUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", photoFile);

            imageUri = outUri;
            takePictureLauncher.launch(outUri);

        } catch (IOException e) {
            Log.e(TAG, "Could not create temporary image file", e);
        }
    }

    private void handleImage(@NonNull Uri uri) {
        imageUri = uri;

        io.execute(() -> {
            try {
                // 1) Read EXIF orientation (needs its own stream)
                int orient = ExifInterface.ORIENTATION_NORMAL;
                try (InputStream exifIn = getContentResolver().openInputStream(uri)) {
                    if (exifIn != null) {
                        ExifInterface exif = new ExifInterface(exifIn);
                        orient = exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL
                        );
                    }
                }

                // 2) Bounds decode to compute sample size
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream boundsIn = getContentResolver().openInputStream(uri)) {
                    if (boundsIn == null) throw new IOException("openInputStream returned null (bounds)");
                    BitmapFactory.decodeStream(boundsIn, null, bounds);
                }

                // target preview size
                final int reqW = 1200;
                final int reqH = 1200;

                int inSampleSize = 1;
                int h = bounds.outHeight;
                int w = bounds.outWidth;
                while (h / inSampleSize > reqH || w / inSampleSize > reqW) {
                    inSampleSize *= 2;
                }

                // 3) Actual decode (sampled)
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = inSampleSize;
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

                Bitmap decoded;
                try (InputStream imgIn = getContentResolver().openInputStream(uri)) {
                    if (imgIn == null) throw new IOException("openInputStream returned null (decode)");
                    decoded = BitmapFactory.decodeStream(imgIn, null, opts);
                }

                if (decoded == null) throw new IOException("Bitmap decode returned null");

                Bitmap rotated = rotateIfNeeded(decoded, orient);

                if (rotated != decoded) decoded.recycle();

                // 4) Create/store thumbnail bytes off-main-thread
                byte[] bytes = Converters.fromBitmap(rotated);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || preview == null) return;
                    preview.setImageBitmap(rotated);
                    thumbBlob = bytes;
                });

            } catch (Exception e) {
                Log.e(TAG, "Reading bitmap failed", e);
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) toast("Could not load image."); });
            }
        });
    }

    private static Bitmap rotateIfNeeded(Bitmap src, int orientation) {
        Matrix m = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:  m.postRotate(90);  break;
            case ExifInterface.ORIENTATION_ROTATE_180: m.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: m.postRotate(270); break;
            default: return src;
        }
        return Bitmap.createBitmap(src, 0, 0,
                src.getWidth(), src.getHeight(), m, true);
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (imageUri != null) out.putParcelable(STATE_IMAGE_URI, imageUri);
    }

    private void populateForm(User user) {
        currentUser = user;
        invalidateOptionsMenu();

        firstName.setText(user.getFirstName());
        lastName.setText(user.getLastName());
        username.setText(user.getUserName());
        updateAdmin(user.isAdmin());
        if (user.getThumbnail() != null) {
            Bitmap bmp = Converters.toBitmap(user.getThumbnail());
            preview.setImageBitmap(bmp);
        } else {
            preview.setBackgroundResource(R.drawable.image_placeholder_border);
            preview.setImageResource(R.drawable.ic_add_photo);
        }
        password.setEnabled(true);
    }

    private void clearForm() {
        firstName.setText("");
        lastName.setText("");
        username.setText("");
        preview.setBackgroundResource(R.drawable.image_placeholder_border);
        preview.setImageResource(R.drawable.ic_add_photo);
        thumbBlob = null;
        imageUri = null;
        currentUser = null;
        password.setEnabled(false);
        invalidateOptionsMenu();
    }

    private void saveUser() {
        boolean adminFlag = modeSwitch.isChecked();
        boolean isNew = currentUser == null;

        User user = isNew ? new User() : currentUser;
        if (!validForm()) { return; }

        user.setFirstName(firstName.getText().toString().trim());
        user.setLastName(lastName.getText().toString().trim());
        user.setUserName(username.getText().toString().trim());
        user.setAdmin(adminFlag);
        user.setThumbnail(thumbBlob);

        if (thumbBlob == null && currentUser != null) {
            thumbBlob = currentUser.getThumbnail();
        }

        user.setThumbnail(thumbBlob);

        if (isNew) {
            String temp = generateTempPassword();
            userViewModel.addUser(user, temp)
                    .observe(this, newUser -> {
                        if (newUser != null) {
                            currentUser = newUser;
                            populateForm(currentUser);
                            toast(currentUser.getUserName() + " saved.");
                            showPasswordResetDialog(temp, currentUser);
                        } else {
                            toast("Username already taken.");
                        }
                    });
        } else {
            userViewModel.update(user);
            toast(user.getUserName() + " saved.");
        }
    }

    private void updateAdmin(boolean isAdmin) {
        saveButton.setText(isAdmin ? R.string.save_admin : R.string.save_employee);
        adminLabel.setText(isAdmin ? R.string.make_admin : R.string.deny_admin);
    }

    private boolean validForm() {
        String firstNameTxt = firstName.getText().toString().trim();
        if (firstNameTxt.isEmpty()) {
            firstName.requestFocus();
            toast("Please enter a first name.");
            return false;
        }

        String lastNameTxt = lastName.getText().toString().trim();
        if (lastNameTxt.isEmpty()) {
            lastName.requestFocus();
            toast("Please enter a last name.");
            return false;
        }

        String usernameTxt = username.getText().toString().trim();
        if (usernameTxt.isEmpty()) {
            username.requestFocus();
            toast("Please enter a username.");
            return false;
        }
        return true;
    }

    private void showPasswordResetDialog(@NonNull String temp, @Nullable User user) {

        final String message;

        if (user == null) {
            message = getString(R.string.temp_pwd_dialog, temp);
        } else {
            String fullName = user.getFirstName() + " " + user.getLastName();
            message = getString(R.string.temp_pwd_body, user.getUserName(), fullName, temp);
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(R.string.temp_password_created)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.copying, (d,w) -> {
                    ClipboardManager cm = getSystemService(ClipboardManager.class);
                    cm.setPrimaryClip(ClipData.newPlainText("temp-password", temp));
                    toast("Copied to clipboard");
                })

                .setNegativeButton(R.string.share, (d,w) -> {
                    Intent share = buildShareIntent(message);
                    startActivity(Intent.createChooser(share, getString(R.string.share_with)));
                });
        b.setNeutralButtonIcon(AppCompatResources.getDrawable(this, R.drawable.ic_content_copy));
        b.setNegativeButtonIcon(AppCompatResources.getDrawable(this, R.drawable.ic_share));
        b.show();
    }

    private Intent buildShareIntent(String body) {

        String subject = getString(R.string.temp_pwd_subject);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");

        // universal text
        share.putExtra(Intent.EXTRA_TEXT, body);

        // subject – e-mail
        share.putExtra(Intent.EXTRA_SUBJECT, subject);

        // sms_body – SMS / RCS apps
        share.putExtra("sms_body", body);

        return share;
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        preview = null;
        io.shutdownNow();
    }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}