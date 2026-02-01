package com.bestbymanager.app.UI.activities;

import static com.bestbymanager.app.utilities.LocalDateBinder.bindFutureDateField;
import static com.bestbymanager.app.utilities.LocalDateBinder.format;
import static com.bestbymanager.app.utilities.LocalDateBinder.parseOrToday;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.bestbymanager.app.R;
import com.bestbymanager.app.UI.authentication.Session;
import com.bestbymanager.app.data.api.ProductData;
import com.bestbymanager.app.data.api.ProductResponse;
import com.bestbymanager.app.data.database.Converters;
import com.bestbymanager.app.data.entities.Product;
import com.bestbymanager.app.databinding.ActivityProductDetailsBinding;
import com.bestbymanager.app.utilities.BarcodeUtil;
import com.bestbymanager.app.viewmodel.ProductDetailsViewModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetails extends AppCompatActivity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private ProductDetailsViewModel productViewModel;
    private static final String TAG = "ProductDetails";
    private static final int REQ_CAMERA = 10;
    private EditText name, quantity, weight, brand, editExp, barcode;
    Button saveButton;
    Button clearButton;
    SwitchMaterial modeSwitch;
    private Product currentProduct;
    private boolean pendingEarlyWarningEnabled = false;
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
            i.putExtra("deeplinkProductId", getIntent().getLongExtra("productID", -1L));
            i.putExtra("fromNotification", getIntent().getBooleanExtra("fromNotification", false));
            startActivity(i);
            finish();
            return;
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

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ActivityProductDetailsBinding binding = ActivityProductDetailsBinding.inflate(getLayoutInflater());
        super.setContentView(binding.getRoot());

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

        TextView attribution = findViewById(R.id.image_attribution);
        attribution.setLinkTextColor(ContextCompat.getColor(this, R.color.dark_green));

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
        TextInputLayout expirationLayout = binding.editExpirationLayout;
        expirationLayout.setEndIconOnClickListener(v -> showEarlyWarningDialog());
        applyEarlyBellIcon(expirationLayout);
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
                    runOnUiThread(() -> toast("Product not found."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductResponse> call, @NonNull Throwable t) {
                toast("Network error");
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

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    private void lookupByBarcode(String code) {
        io.execute(() -> {
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
        boolean hasProduct = currentProduct != null;

        MenuItem delete = menu.findItem(R.id.deleteProduct);
        if (delete != null) delete.setVisible(hasProduct);

        // show discard whenever a product exists and there is on-hand qty
        MenuItem discard = menu.findItem(R.id.discardProduct);
        if (discard != null) {
            boolean showDiscard = hasProduct && currentProduct.getQuantity() > 0;
            discard.setVisible(showDiscard);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       if (item.getItemId() == R.id.mainScreen) {
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
        } else if (item.getItemId() == R.id.discardProduct) {
            if (currentProduct == null) {
                toast("No product loaded.");
                return true;
            }
            showDiscardDialog(currentProduct);
            return true;
        } else if (item.getItemId() == R.id.deleteProduct) {
            if (currentProduct != null) {
                productViewModel.delete(currentProduct);
                clearForm();
                toast("Product deleted.");
            } else {
                toast("Error deleting product.");
            }
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDiscardDialog(@NonNull Product product) {
        View v = getLayoutInflater().inflate(R.layout.dialog_discard_expired, null);

        TextInputEditText qtyEt = v.findViewById(R.id.discard_quantity);
        TextInputEditText reasonEt = v.findViewById(R.id.discard_reason);

        qtyEt.setText(String.valueOf(product.getQuantity()));

        new AlertDialog.Builder(this)
                .setTitle("Discard item")
                .setView(v)
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.discard_confirm, (d, which) -> {
                    String qtyTxt = qtyEt.getText() == null ? "" : qtyEt.getText().toString().trim();
                    int qty;
                    try {
                        qty = Integer.parseInt(qtyTxt);
                    } catch (NumberFormatException ex) {
                        toast("Invalid quantity.");
                        return;
                    }
                    if (qty <= 0) { toast("Discard quantity must be > 0."); return; }
                    if (qty > product.getQuantity()) { toast("Cannot discard more than on-hand."); return; }

                    String reason = reasonEt.getText() == null ? null : reasonEt.getText().toString().trim();
                    if (reason != null && reason.isEmpty()) reason = null;

                    long userId = Session.get().currentUserID();
                    Log.d("DISCARD", "ProductDetails discard uid=" + userId);
                    productViewModel.discardExpiredProduct(product.getProductID(), qty, reason, userId);

                    // optimistic UI: reduce displayed qty so it feels immediate
                    int newQty = product.getQuantity() - qty;
                    currentProduct = product;
                    quantity.setText(String.valueOf(newQty));
                    invalidateOptionsMenu();
                })
                .show();
    }

    private void showEarlyWarningDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_early_warning, null);
        MaterialSwitch toggle = v.findViewById(R.id.early_warning_switch);

        toggle.setChecked(pendingEarlyWarningEnabled);

        new AlertDialog.Builder(this)
                .setTitle("Early warning")
                .setView(v)
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton("OK", (d, which) -> {
                    pendingEarlyWarningEnabled = toggle.isChecked();
                    TextInputLayout exp = findViewById(R.id.edit_expiration_layout);
                    applyEarlyBellIcon(exp);
                })
                .show();
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

    private static int clampSelection(int idx, Spinner spinner) {
        if (spinner == null || spinner.getAdapter() == null) return 0;
        int count = spinner.getAdapter().getCount();
        if (count <= 0) return 0;
        if (idx < 0) return 0;
        if (idx >= count) return count - 1;
        return idx;
    }

    private void populateForm(Product product) {
        currentProduct = product;
        invalidateOptionsMenu();

        name.setText(product.getProductName());
        brand.setText(product.getBrand());
        weight.setText(product.getWeight());
        quantity.setText(String.valueOf(product.getQuantity()));
        editExp.setText(format(product.getExpirationDate()));
        pendingEarlyWarningEnabled = product.isEarlyWarningEnabled();
        applyEarlyBellIcon((TextInputLayout) findViewById(R.id.edit_expiration_layout));
        // nullable barcode
        String bc = product.getBarcode();
        barcode.setText(bc == null ? "" : BarcodeUtil.displayCode(bc));

        // clamp indices to adapter bounds
        category.setSelection(clampSelection(product.getCategory(), category), false);
        isle.setSelection(clampSelection(product.getIsle(), isle), false);

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
        invalidateOptionsMenu();
        pendingEarlyWarningEnabled = false;
        modeSwitch.setChecked(false);
        modeSwitch.setEnabled(false);
    }

    private boolean isBarcodePlausible(String code) {
        if (code == null) return false;
        String c = code.trim();
        if (!c.matches("\\d+")) return false;
        // Accept common EAN/UPC lengths; tighten as you need:
        return c.length() == 8 || c.length() == 12 || c.length() == 13 || c.length() == 14;
    }

    private void saveProduct() {
        long currentUserId = Session.get().currentUserID();
        if (currentUserId <= 0) {
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra("deepLink", getIntent());
            startActivity(i);
            finish();
            return;
        }

        final boolean isCreate = (currentProduct == null);
        final boolean addNewExpiration = (!isCreate) && modeSwitch.isEnabled() && modeSwitch.isChecked();

        if (!validForm(isCreate)) return;

        String barcodeTxt = barcode.getText().toString().trim();
        if (!isBarcodePlausible(barcodeTxt)) {
            toast("Unsupported or unreadable barcode.");
            return;
        }

        int qtyParsed = parseQtyInt(quantity.getText().toString().trim());

        Product toSave = (isCreate || addNewExpiration) ? new Product() : currentProduct;

        toSave.setProductName(name.getText().toString().trim());
        toSave.setBrand(brand.getText().toString().trim());
        toSave.setWeight(weight.getText().toString().trim());
        toSave.setQuantity(qtyParsed);
        toSave.setCategory(category.getSelectedItemPosition());
        toSave.setIsle(isle.getSelectedItemPosition());
        toSave.setExpirationDate(parseOrToday(editExp.getText().toString().trim()));
        toSave.setPurchaseDate(LocalDate.now());
        toSave.setUserID(currentUserId);
        toSave.setBarcode(barcodeTxt);

        if (thumbBlob == null && currentProduct != null) {
            thumbBlob = currentProduct.getThumbnail();
        }

        toSave.setThumbnail(thumbBlob);

        if (isCreate && toSave.isExpired()) {
            toast("Cannot add a product that is already expired.");
            return;
        }
        // only applies when editing an existing product (not create, not add-new-expiration)
        if (!isCreate && !addNewExpiration && toSave.isExpired()) {
            int oldQty = currentProduct.getQuantity();
            if (qtyParsed > oldQty) {
                toast("Product expired. Quantity can only be reduced.");
                return;
            }
        }

        toSave.setEarlyWarningEnabled(pendingEarlyWarningEnabled);

        productViewModel.save(toSave, id -> runOnUiThread(() -> {
            if (id <= 0) { toast("Save failed."); return; }

            if (toSave.getProductID() == id) {
                currentProduct = toSave;
                populateForm(toSave);
            } else {
                toSave.setProductID(id);
                lookupByBarcode(barcodeTxt);
            }

            modeSwitch.setEnabled(true);
            modeSwitch.setChecked(false);
            toast(name.getText().toString().trim() + " saved.");
        }));
    }

    private static int parseQtyInt(String qtyTxt) {
        // validForm already enforced 0..999_999_999
        return (int) Long.parseLong(qtyTxt);
    }

    private boolean validForm(boolean isCreate) {
        String nameTxt = name.getText().toString().trim();
        if (nameTxt.isEmpty()) { name.requestFocus(); toast("Please enter a product name."); return false; }

        String brandTxt = brand.getText().toString().trim();
        if (brandTxt.isEmpty()) { brand.requestFocus(); toast("Please enter a brand."); return false;}

        String expTxt = editExp.getText().toString().trim();
        if (expTxt.isEmpty()) { editExp.requestFocus(); toast("Please select an expiration date."); return false; }

        String qtyTxt = quantity.getText().toString().trim();
        if (qtyTxt.isEmpty()) { quantity.requestFocus(); toast("Please enter a quantity."); return false; }

        long qtyLong;
        try {
            qtyLong = Long.parseLong(qtyTxt);
        } catch (NumberFormatException e) {
            quantity.requestFocus();
            toast("Quantity must be a non-negative integer.");
            return false;
        }

        if (qtyLong < 0) { quantity.requestFocus(); toast("Quantity must be 0 or more."); return false; }

        if (qtyLong > 999_999_999L) { quantity.requestFocus(); toast("Quantity must be 999999999 or less."); return false; }

        if (isCreate && qtyLong == 0) { quantity.requestFocus(); toast("Quantity must be greater than 0 when creating a new product."); return false; }

        String weightTxt = weight.getText().toString().trim();
        if (weightTxt.isEmpty()) { weight.requestFocus(); toast("Please enter a weight."); return false; }

        String barcodeTxt = barcode.getText().toString().trim();
        if (barcodeTxt.isEmpty()) { barcode.requestFocus(); toast("Please enter a barcode."); return false; }

        int categoryPosition = category.getSelectedItemPosition();
        if (categoryPosition == 0) { category.requestFocus(); toast("Please select a category."); return false; }

        return true;
    }

    private void applyEarlyBellIcon(@NonNull TextInputLayout expirationLayout) {
        int icon = pendingEarlyWarningEnabled ? R.drawable.ic_bell_filled : R.drawable.ic_bell;
        expirationLayout.setEndIconDrawable(icon);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
    }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}

