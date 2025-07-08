package com.example.bestbymanager.UI.activities;

import static com.example.bestbymanager.utilities.BarcodeUtil.toCanonical;
import static com.example.bestbymanager.utilities.LocalDateBinder.bindFutureDateField;
import static com.example.bestbymanager.utilities.LocalDateBinder.format;
import static com.example.bestbymanager.utilities.LocalDateBinder.parseOrToday;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.bestbymanager.R;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.data.api.ProductData;
import com.example.bestbymanager.data.api.ProductResponse;
import com.example.bestbymanager.data.database.Converters;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.databinding.ActivityProductDetailsBinding;
import com.example.bestbymanager.utilities.BarcodeUtil;
import com.example.bestbymanager.viewmodel.ProductDetailsViewModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetails extends AppCompatActivity {
    private ProductDetailsViewModel productViewModel;
    private static final String TAG = "ProductDetails";
    private static final int REQ_CAMERA = 10;
    private EditText name, quantity, weight, brand, editExp, barcode;
    Button saveButton;
    Button clearButton;
    SwitchMaterial modeSwitch;
    private Product currentProduct;
    private Spinner category;
    private Spinner isle;
    private ImageView preview;
    private Uri imageUri;
    private Uri tempCameraUri;
    private byte[] thumbBlob;
    private ArrayAdapter<String> adapter;
    private Callback<ProductResponse> lookupCallback;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;

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
        setTitle(R.string.product_details);
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                ok -> {
                    if (ok) {
                        handleImage(tempCameraUri);
                    }
                });

        ActivityProductDetailsBinding binding = ActivityProductDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        brand = binding.editBrand;
        name = binding.editProductName;
        weight = binding.editWeight;
        quantity = binding.editQuantity;
        editExp = binding.editExpiration;
        barcode = binding.editBarcode;
        category = binding.spinnerCategory;
        isle = binding.spinnerIsle;
        preview = binding.imagePreview;
        TextInputLayout barcodeLayout = binding.barcodeInputLayout;
        modeSwitch = binding.switchMode;
        saveButton = binding.saveProductButton;
        clearButton = binding.clearProductButton;
        binding.imagePreview.setOnClickListener(v -> { if (ensureCameraPermission()) { launchCamera(); } });
        modeSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            if (isChecked) {
                // “Add new expiration” mode
                saveButton.setText(R.string.add_new_expiration);
            } else {
                // “Save / update existing” mode
                saveButton.setText(R.string.save_product);
            }
        });
        modeSwitch.setChecked(false);
        modeSwitch.setEnabled(false);
        clearButton.setOnClickListener(v -> clearForm());
        saveButton.setOnClickListener(v -> saveProduct());

        String[] isleLabels = getResources().getStringArray(R.array.isles);
        ArrayAdapter<String> isleAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_center_bold,
                isleLabels
        );
        isleAdapter.setDropDownViewResource(R.layout.spinner_item_center_bold);
        isle.setAdapter(isleAdapter);
        isle.setSelection(0, false);

        adapter = new ArrayAdapter<>(
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
            handleImage(imageUri);
        }

        lookupCallback = new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ProductResponse> call, @NonNull Response<ProductResponse> response) {

                if (response.isSuccessful() && response.body() != null && response.body().status == 1) {
                    ProductData data = response.body().product;
                    runOnUiThread(() -> {
                        brand.setText(data.brands);
                        name.setText(data.productName);
                        weight.setText(data.weight);
                        barcode.setText(data.barcode);
                        String categories = data.category;
                        String[] parts;

                        if (categories != null && !categories.isEmpty()) {
                            parts = categories.split("\\s*,\\s*");
                            if (parts.length > 0) {
                                // assume the *first* part is the “main” category
                                String primary = parts[0];
                                int position = adapter.getPosition(primary);
                                if (position >= 0) category.setSelection(position);
                            }
                        } else {
                            category.setSelection(0, false);
                        }

                        if (data.imageUri != null && !data.imageUri.isEmpty()) {
                            Glide.with(ProductDetails.this)
                                    .asBitmap()
                                    .load(data.imageUri)
                                    .placeholder(R.drawable.image_placeholder_border)
                                    .into(new CustomTarget<Bitmap>() {
                                        @Override
                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                            preview.setImageBitmap(resource);
                                            thumbBlob = Converters.fromBitmap(resource);
                                        }

                                        @Override
                                        public void onLoadCleared(@Nullable Drawable placeholder) {
                                            preview.setImageDrawable(placeholder);
                                            thumbBlob = null;
                                        }
                                    });
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ProductDetails.this, "Product not found.", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                Toast.makeText(ProductDetails.this, "Network error", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to fetch product", t);
            }
        };

        productViewModel = new ViewModelProvider(this,
                new SavedStateViewModelFactory(getApplication(), this))
                .get(ProductDetailsViewModel.class);

        productViewModel.getProduct().observe(this, product -> {
            if (product == null) { return; }
            currentProduct = product;
            populateForm(product);
            thumbBlob = product.getThumbnail();
        });

        bindFutureDateField(editExp, this);

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String code = result.getContents();
                barcode.setText(code);
                lookupByBarcode(code);
            }
        });

        barcodeLayout.setEndIconOnClickListener(view -> {
            if (ensureCameraPermission()) {
                ScanOptions options = new ScanOptions();
                options.setDesiredBarcodeFormats(ScanOptions.ONE_D_CODE_TYPES);
                options.setPrompt("Align the barcode inside the box");
                options.setBeepEnabled(false);
                options.setOrientationLocked(true);
                barcodeLauncher.launch(options);
            }
        });

        barcode.setOnEditorActionListener((view, id, event) -> {
            if (id == EditorInfo.IME_ACTION_GO) {
                lookupByBarcode(barcode.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void lookupByBarcode(String rawCode) {
        String code;
        try {
            code = toCanonical(rawCode);
            Log.d("BARCODE-LOOKUP", toCanonical(code));
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this,
                    "Unsupported or unreadable barcode", Toast.LENGTH_SHORT).show();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            Product local = productViewModel.getRecentExpiringProduct(code);
            runOnUiThread(() -> {
                if (local != null) {
                    currentProduct = local;
                    populateForm(local);
                } else {
                    Log.d(TAG, "→ No local hit – calling API");
                    productViewModel.fetchProduct(code, lookupCallback);
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_details, menu);
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
            if (currentProduct != null) {
                productViewModel.delete(currentProduct);
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

    private void populateForm(Product product) {
        name.setText(product.getProductName());
        brand.setText(product.getBrand());
        weight.setText(product.getWeight());
        quantity.setText(String.valueOf(product.getQuantity()));
        editExp.setText(format(product.getExpirationDate()));
        barcode.setText(BarcodeUtil.displayCode(product.getBarcode()));
        category.setSelection(product.getCategory());
        isle.setSelection(product.getIsle());
        modeSwitch.setEnabled(true);
        if (product.getThumbnail() != null) {
            Bitmap bmp = Converters.toBitmap(product.getThumbnail());
            preview.setImageBitmap(bmp);
        } else {
            preview.setBackgroundResource(R.drawable.image_placeholder_border);
            preview.setImageResource(R.drawable.ic_add_photo);
        }
    }

    private void clearForm() {
        brand.setText("");
        name.setText("");
        weight.setText("");
        quantity.setText("");
        barcode.setText("");
        category.setSelection(0);
        isle.setSelection(0);
        editExp.setText(getString(R.string.expiration_date));
        preview.setBackgroundResource(R.drawable.image_placeholder_border);
        preview.setImageResource(R.drawable.ic_add_photo);
        thumbBlob = null;
        imageUri = null;
        currentProduct = null;
        modeSwitch.setChecked(false);
        modeSwitch.setEnabled(false);
    }

    private void saveProduct() {
        if (!validForm()) { return; }
        boolean addNew = modeSwitch.isChecked() && modeSwitch.isEnabled();

        long currentUserId = Session.get().currentUserID();

        Product toSave = (addNew || currentProduct == null) ? new Product() : currentProduct;

        toSave.setProductName(name.getText().toString().trim());
        toSave.setBrand(brand.getText().toString().trim());
        toSave.setWeight(weight.getText().toString().trim());
        toSave.setQuantity(Integer.parseInt(quantity.getText().toString().trim()));
        toSave.setCategory(category.getSelectedItemPosition());
        toSave.setIsle(isle.getSelectedItemPosition());
        toSave.setExpirationDate(parseOrToday(editExp.getText().toString().trim()));
        toSave.setPurchaseDate(LocalDate.now());
        toSave.setUserID(currentUserId);

        if (thumbBlob == null && currentProduct != null) {
            thumbBlob = currentProduct.getThumbnail();
        }

        toSave.setThumbnail(thumbBlob);

        try {
            String canonical = toCanonical(barcode.getText().toString().trim());
            toSave.setBarcode(canonical);
        } catch (IllegalArgumentException ex) {
            Toast.makeText(this, "Unsupported or unreadable barcode", Toast.LENGTH_SHORT).show();
            return;
        }

        if (addNew && toSave.isExpired()) {
            Toast.makeText(this, "Cannot add a product that is already expired.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!addNew && toSave.isExpired()) {
            int oldQty   = currentProduct.getQuantity();
            int newQty   = Integer.parseInt(quantity.getText().toString().trim());
            if (newQty > oldQty) {
                Toast.makeText(this, "Product expired. Quantity can only be reduced.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        productViewModel.save(toSave);
        modeSwitch.setEnabled(true);
        modeSwitch.setChecked(true);
        Toast.makeText(this, name.getText().toString().trim() + " saved.", Toast.LENGTH_SHORT).show();
    }

    private boolean validForm() {
        String nameTxt = name.getText().toString().trim();
        if (nameTxt.isEmpty()) {
            name.requestFocus();
            Toast.makeText(this, "Please enter a product name.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String brandTxt = brand.getText().toString().trim();
        if (brandTxt.isEmpty()) {
            brand.requestFocus();
            Toast.makeText(this, "Please enter a brand.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String expTxt = editExp.getText().toString().trim();
        if (expTxt.isEmpty()) {
            editExp.requestFocus();
            Toast.makeText(this, "Please select an expiration date.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String qtyTxt = quantity.getText().toString().trim();
        if (qtyTxt.isEmpty()) {
            quantity.requestFocus();
            Toast.makeText(this, "Please enter a quantity.", Toast.LENGTH_SHORT).show();
            return false;
        }
        int qty;
        try {
            qty = Integer.parseInt(qtyTxt);
            if (qty < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            quantity.requestFocus();
            Toast.makeText(this, "Quantity must be a non-negative integer.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String weightTxt = weight.getText().toString().trim();
        if (weightTxt.isEmpty()) {
            weight.requestFocus();
            Toast.makeText(this, "Please enter a weight.", Toast.LENGTH_SHORT).show();
            return false;
        }

        String barcodeTxt = barcode.getText().toString().trim();
        if (barcodeTxt.isEmpty()) {
            barcode.requestFocus();
            Toast.makeText(this, "Please enter a barcode.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int categoryPosition = category.getSelectedItemPosition();
        if (categoryPosition == 0) {
            category.requestFocus();
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}

