package com.example.bestbymanager.UI.activities;

import android.Manifest;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.data.database.Converters;
import com.example.bestbymanager.data.entities.User;
import com.example.bestbymanager.databinding.ActivityUserDetailsBinding;
import com.example.bestbymanager.viewmodel.UserDetailsViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class UserDetails extends AppCompatActivity {
    private UserDetailsViewModel userViewModel;
    private static final String TAG = "UserDetails";
    private static final int REQ_CAMERA = 10;
    private EditText userID, firstName, lastName, username, password;
    private TextView adminLabel;
    Button saveButton;
    Button clearButton;
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
        setTitle(R.string.user_details);
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> {
                    if (ok) {
                        handleImage(tempCameraUri);
                    }
                });

        ActivityUserDetailsBinding binding = ActivityUserDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userID = binding.textUserId;
        firstName = binding.editFirstName;
        lastName = binding.editLastName;
        username = binding.editUsername;
        password = binding.editPassword;
        preview = binding.imagePreview;
        modeSwitch = binding.adminToggle;
        saveButton = binding.saveUserButton;
        clearButton = binding.clearUserButton;
        adminLabel = binding.administratorLabel;
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
        boolean isAdmin = Session.get().currentUserIsAdmin();
        menu.findItem(R.id.adminPage).setVisible(isAdmin);
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
        } else if (item.getItemId() == R.id.productSearch) {
            Intent intent = new Intent(this, ProductSearch.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.productList) {
            Intent intent = new Intent(this, ProductList.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.deleteProduct) {
            if (currentUser != null) {
                userViewModel.delete(currentUser);
                clearForm();
                Toast.makeText(this, "Product deleted.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Error deleting product.", Toast.LENGTH_LONG).show();
            }
            this.finish();
            return true;
        } else if (item.getItemId() == R.id.adminPage) {
            Intent intent = new Intent(this, AdministratorActivity.class);
            startActivity(intent);
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
        userID.setText(String.valueOf(user.getUserID()));
        firstName.setText(user.getFirstName());
        lastName.setText(user.getLastName());
        username.setText(user.getUserName());
        password.setText(user.getHash());
        updateAdmin(user.isAdmin());
        if (user.getThumbnail() != null) {
            Bitmap bmp = Converters.toBitmap(user.getThumbnail());
            preview.setImageBitmap(bmp);
        } else {
            preview.setBackgroundResource(R.drawable.image_placeholder_border);
            preview.setImageResource(R.drawable.ic_add_photo);
        }
    }

    private void clearForm() {
        userID.setText("");
        firstName.setText("");
        lastName.setText("");
        username.setText("");
        password.setText("");
        preview.setBackgroundResource(R.drawable.image_placeholder_border);
        preview.setImageResource(R.drawable.ic_add_photo);
        thumbBlob = null;
        imageUri = null;
        currentUser = null;
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
            userViewModel.addUser(user, password.getText().toString().trim());
        } else {
            userViewModel.update(user, password.getText().toString().trim().isEmpty() ? null : password.getText().toString().trim());
        }
        Toast.makeText(this, username.getText().toString().trim() + " saved.", Toast.LENGTH_SHORT).show();
    }

    private void updateAdmin(boolean isAdmin) {
        saveButton.setText(isAdmin ? R.string.save_admin : R.string.save_user);
        adminLabel.setText(isAdmin ? R.string.make_admin : R.string.deny_admin);
    }

    private boolean validForm() {
        String firstNameTxt = firstName.getText().toString().trim();
        if (firstNameTxt.isEmpty()) {
            firstName.requestFocus();
            Toast.makeText(this, "Please enter a first name.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String lastNameTxt = lastName.getText().toString().trim();
        if (lastNameTxt.isEmpty()) {
            lastName.requestFocus();
            Toast.makeText(this, "Please enter a last name.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String usernameTxt = username.getText().toString().trim();
        if (usernameTxt.isEmpty()) {
            username.requestFocus();
            Toast.makeText(this, "Please enter a username.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String passwordTxt = password.getText().toString().trim();
        if (passwordTxt.isEmpty()) {
            password.requestFocus();
            Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}