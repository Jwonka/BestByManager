package com.example.bestbymanager.UI.activities;

import static com.example.bestbymanager.utilities.LocalDateBinder.bindFutureDateField;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.database.Converters;
import com.example.bestbymanager.viewmodel.ProductDetailsViewModel;
import java.io.File;
import java.io.IOException;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class ProductDetails extends AppCompatActivity {
    private ProductDetailsViewModel productViewModel;
    private TextInputLayout barcodeLayout;
    private static final String TAG = "ProductDetails";
    private static final int REQ_CAMERA = 10;
    private static final int REQ_READ_MEDIA = 20;
    private EditText name, qty, weight, brand, barcode;
    Button editExp;
    private Spinner category;
    private ImageView preview;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri imageUri;
    private Uri tempCameraUri;
    private byte[] thumbBlob;
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    barcode.setText(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (s != null) {
            imageUri = s.getParcelable("IMAGE_URI");
        }

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> {
                    if (ok) {
                        handleImage(tempCameraUri);
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleImage(uri);
                    }
                });

        setContentView(R.layout.activity_product_details);
        preview = findViewById(R.id.image_preview);
        preview.setOnClickListener(v -> showImageSourceDialog());

        name = findViewById(R.id.edit_product_name);
        editExp = findViewById(R.id.edit_expiration);
        qty = findViewById(R.id.edit_quantity);
        weight = findViewById(R.id.edit_weight);
        brand = findViewById(R.id.edit_brand);
        barcode = findViewById(R.id.edit_barcode);
        category = findViewById(R.id.spinner_category);
        preview = findViewById(R.id.image_preview);
        barcodeLayout = findViewById(R.id.barcode_input_layout);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item_center_bold,
                getResources().getStringArray(R.array.product_categories)) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }
        };

        adapter.setDropDownViewResource(R.layout.spinner_item_center_bold);
        category.setAdapter(adapter);
        category.setSelection(0, false);

        if (imageUri != null) {
            preview.setImageURI(imageUri);
            try {
                thumbBlob = Converters.fromBitmap(
                        MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri));
            } catch (IOException e) {
                Log.e(TAG, "Reading bitmap failed", e);
            }
        }

        productViewModel = new ViewModelProvider(this,
                new SavedStateViewModelFactory(getApplication(), this))
                .get(ProductDetailsViewModel.class);

        productViewModel.getProduct().observe(this, product -> {

        });

        bindFutureDateField(editExp, this);

        barcodeLayout.setEndIconOnClickListener(v -> {
            if (ensureCameraPermission()) {
                ScanOptions options = new ScanOptions();
                options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
                options.setPrompt("Align the barcode inside the box");
                options.setBeepEnabled(false);
                options.setOrientationLocked(true);

                barcodeLauncher.launch(options);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_details, menu);
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
        } else if (item.getItemId() == R.id.saveProduct) {
            this.finish();
            return true;
        } else if (item.getItemId() == R.id.deleteProduct) {
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

    private boolean ensureGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {     // API 34+
            return requireOrRequest(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {      // API 33
            return requireOrRequest(Manifest.permission.READ_MEDIA_IMAGES);
        }
        // API ≤ 32 – no runtime permission needed
        return true;
    }

    // @return true  if the permission is already granted, false if asked for
    private boolean requireOrRequest(String permission) {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return true;                                    // nothing else to do
        }
        requestPermissions(new String[]{permission}, REQ_READ_MEDIA);
        return false;                                       // wait for callback
    }

    @Override
    public void onRequestPermissionsResult(
            int req, @NonNull String[] perms, @NonNull int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);

        if (req == REQ_CAMERA && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        }
    }

    private void showImageSourceDialog() {
        String[] choices = { getString(R.string.camera), getString(R.string.gallery) };

        new AlertDialog.Builder(this)
                .setTitle(R.string.select_photo_source)
                .setItems(choices, (d, which) -> {
                    if (which == 0) {
                        if (ensureCameraPermission()) {
                            launchCamera();
                        }
                    } else {
                        pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void launchCamera()  {
        try {
            File photoFile = File.createTempFile("product_", ".jpg", getCacheDir());
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

        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            thumbBlob  = Converters.fromBitmap(bmp);
        } catch (IOException e) {
            Log.e(TAG, "Reading bitmap failed", e);
        }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (imageUri != null) out.putParcelable("IMAGE_URI", imageUri);
    }
}

