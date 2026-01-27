package com.bestbymanager.app.UI.activities;

import static com.bestbymanager.app.utilities.PasswordUtil.generateTempPassword;
import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.BaseAdminActivity;
import com.bestbymanager.app.UI.authentication.Session;
import com.bestbymanager.app.data.database.Converters;
import com.bestbymanager.app.data.entities.User;
import com.bestbymanager.app.databinding.ActivityUserDetailsBinding;
import com.bestbymanager.app.viewmodel.UserDetailsViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class UserDetails extends BaseAdminActivity {
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
    private Uri tempCameraUri;
    private byte[] thumbBlob;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Session.get().isLoggedOut()) {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("deepLink", getIntent());
            startActivity(i);
            finish();
        }

        if (s != null) {
            imageUri = s.getParcelable("IMAGE_URI");
        }
        setTitle(R.string.employee_details);
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> {
                    if (ok) {
                        handleImage(tempCameraUri);
                    }
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
        binding.imagePreview.setOnClickListener(v -> {
            if (ensureCameraPermission()) {
                launchCamera();
            }
        });
        modeSwitch.setOnCheckedChangeListener((btn, isChecked) -> updateAdmin(isChecked));
        clearButton.setOnClickListener(v -> clearForm());
        saveButton.setOnClickListener(v -> saveUser());

        if (imageUri != null) {
            handleImage(imageUri);
        }

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
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.deleteEmployee).setVisible(currentUser != null);
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
        } else if (item.getItemId() == R.id.employeeSearch) {
            Intent intent = new Intent(this, UserSearch.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.employeeList) {
            Intent intent = new Intent(this, UserList.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.deleteEmployee) {
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
            tempCameraUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", photoFile);

            takePictureLauncher.launch(tempCameraUri);

        } catch (IOException e) {
            Log.e(TAG, "Could not create temporary image file", e);
        }
    }

    private void handleImage(@NonNull Uri uri)  {
        imageUri = uri;
        preview.setImageURI(uri);

        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in != null) {
                ExifInterface exif = new ExifInterface(in);
                int orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Bitmap raw = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Bitmap bmp = rotateIfNeeded(raw, orient);
                preview.setImageBitmap(bmp);
                thumbBlob = Converters.fromBitmap(bmp);
            }
        } catch (IOException e) {
            Log.e(TAG, "Reading bitmap failed", e);
        }
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
        if (imageUri != null) out.putParcelable("IMAGE_URI", imageUri);
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
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}