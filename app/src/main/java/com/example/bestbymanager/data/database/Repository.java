package com.example.bestbymanager.data.database;

import static com.example.bestbymanager.utilities.BarcodeUtil.toCanonical;
import static com.example.bestbymanager.utilities.PasswordUtil.hash;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.bestbymanager.UI.adapter.LoginResult;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.data.api.OffApi;
import com.example.bestbymanager.data.api.ProductResponse;
import com.example.bestbymanager.data.dao.ProductDAO;
import com.example.bestbymanager.data.dao.UserDAO;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.entities.User;
import com.example.bestbymanager.data.pojo.ProductReportRow;
import com.example.bestbymanager.data.pojo.UserReportRow;
import com.example.bestbymanager.utilities.AlarmScheduler;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** @noinspection unused*/
public class Repository {
    private final UserDAO mUserDAO;
    private final ProductDAO mProductDAO;
    private final Context context;
    public static final int NUMBER_OF_THREADS = 4;
    private final Executor executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private final OffApi api;
    private static final LiveData<List<?>> EMPTY = new MutableLiveData<>(Collections.emptyList());
    @SuppressWarnings("unchecked")
    private static <T> LiveData<T> emptyLiveData() { return (LiveData<T>) EMPTY; }

    public Repository(Application application) {
        this.context = application.getApplicationContext();
        ProductDatabaseBuilder db = ProductDatabaseBuilder.getDatabase(application);
        mUserDAO=db.userDAO();
        mProductDAO=db.productDAO();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(OffApi.class);
    }

    public LiveData<List<ProductReportRow>> getAllProducts() { return mProductDAO.getAllProducts(); }
    public LiveData<List<UserReportRow>> getAllEntries(LocalDate today) { return mUserDAO.getAllEntries(today); }
    public LiveData<List<UserReportRow>> getEntriesByEmployee(long userID, LocalDate today) { return mUserDAO.getEntriesByEmployee(userID, today); }
    public LiveData<List<UserReportRow>> getEntriesByBarcodeForRange(String raw, LocalDate from, LocalDate to, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mUserDAO.getEntriesByBarcodeForRange(code, from, to, today);
    }
    public LiveData<List<UserReportRow>> getEntriesForEmployeeAndBarcode(long userID, String raw, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mUserDAO.getEntriesForEmployeeAndBarcode(userID, code, today);
    }
    public LiveData<List<UserReportRow>> getEntriesByBarcodeForEmployeeInRange(long userID, String raw, LocalDate from, LocalDate to, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mUserDAO.getEntriesByBarcodeForEmployeeInRange(userID, code, from, to, today);
    }
    public LiveData<List<UserReportRow>> getEntriesForEmployeeInRange(long userID, LocalDate from, LocalDate to, LocalDate today) { return mUserDAO.getEntriesForEmployeeInRange(userID, from, to, today); }
    public LiveData<List<UserReportRow>> getAdmins() { return mUserDAO.getAdmins(); }
    public LiveData<List<UserReportRow>> getEntriesByDateRange(LocalDate from, LocalDate to, LocalDate today) { return mUserDAO.getEntriesByDateRange(from, to, today); }
    public LiveData<List<UserReportRow>> getEntriesForBarcode(String raw, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mUserDAO.getEntriesByBarcode(code, today);
    }
    public Product getRecentExpirationByBarcode(String raw) {
        String code = canonicalOrNull(raw);
        if (code == null) { return null; }
        return mProductDAO.getRecentExpirationByBarcode(code);
    }
    public LiveData<List<Product>> getProductsByBarcode(String raw) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mProductDAO.getProductsByBarcode(code);
    }
    public void fetchProduct(String raw, Callback<ProductResponse> cb) {
        String code = canonicalOrNull(raw);
        if (code == null) { return; }
        api.getByBarcode(code).enqueue(cb);
    }
    public LiveData<List<Product>> getProducts(LocalDate today){ return mProductDAO.getProducts(today); }
    public LiveData<Product> getProduct(long productID){ return mProductDAO.getProduct(productID); }
    public LiveData<User> getUser(long userID){ return mUserDAO.getUser(userID); }
    public LiveData<List<User>> getUsers(){ return mUserDAO.getUsers(); }
    public LiveData<List<User>> loadAdmins() { return mUserDAO.loadAdmins(); }
    public LiveData<List<ProductReportRow>> getExpiring(LocalDate from, LocalDate selected) {return mProductDAO.getExpiring(from, selected); }
    public LiveData<List<Product>> getProductsByDateRange(LocalDate from, LocalDate selected) {return mProductDAO.getProductsByDateRange(from, selected); }
    public LiveData<List<ProductReportRow>> getExpired(LocalDate today) {return mProductDAO.getExpired(today); }
    public LiveData<List<ProductReportRow>> getReportRowsByBarcode(String raw) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() :  mProductDAO.getReportRowsByBarcode(code);
    }
    public LiveData<List<ProductReportRow>> getProductsByBarcodeAndDateRange(String raw, LocalDate from, LocalDate to) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mProductDAO.getProductsByBarcodeAndDateRange(code, from, to);
    }
    public void insertProduct(Product product) { executor.execute(() -> {
        try {
            product.setBarcode(toCanonical(product.getBarcode()));
        } catch (IllegalArgumentException ex) {
            showToast("Unsupported or unreadable barcode");
            return;
        }
        long id = mProductDAO.insert(product);
            if (id == -1) {
                showToast("Product already exists.");
            } else {
                product.setProductID(id);
                AlarmScheduler.scheduleAlarm(context, product.getExpirationDate(), product.getProductID(), product.getProductName() + " expires today.");
            }
        });
    }
    public void updateProduct(Product product) { executor.execute(() -> {
            try {
                product.setBarcode(toCanonical(product.getBarcode()));
            } catch (IllegalArgumentException ex) {
                showToast("Unsupported or unreadable barcode");
                return;
            }
            int rows = mProductDAO.updateProduct(product);
            if (rows == 0) {
                showToast("Error - conflict on barcode or ID.");
            }  else {
                AlarmScheduler.cancelAlarm(context, product.getProductID());
                AlarmScheduler.scheduleAlarm(context, product.getExpirationDate(), product.getProductID(), product.getProductName() + " expires today.");
            }
        });
    }
    public void deleteProduct(Product product) { executor.execute(() -> {
            int rows = mProductDAO.deleteProduct(product);
            if (rows == 0) {
                showToast("Product not found.");
            }  else {
                AlarmScheduler.cancelAlarm(context, product.getProductID());
            }
        });
    }

    public LiveData<Boolean> isProductExpired(int productID) {
        return Transformations.map(
                getProduct(productID), product -> {
                    if (product == null) return false;
                    LocalDate expired = product.getExpirationDate();
                    return expired.isBefore(LocalDate.now());
                }
        );
    }

    public LiveData<LoginResult> login(String username, String plainPassword) {
        MutableLiveData<LoginResult> pass = new MutableLiveData<>();
        executor.execute(() -> {
            User user = mUserDAO.findByUsername(username);

            boolean ok = user != null
                    && !user.getHash().isEmpty()
                    && BCrypt.checkpw(plainPassword, user.getHash());

            if (!ok) {
                pass.postValue(new LoginResult(LoginResult.Code.BAD_CREDENTIALS, null));
                return;
            }

            // Force password reset if flagged (regardless of expiry window)
            if (user.isMustChange()) {
                // Reject password resets after 24 hours
                OffsetDateTime until = user.getResetExpires();
                if (until != null && OffsetDateTime.now().isAfter(until)) {
                    pass.postValue(LoginResult.expired());
                    return;
                }
                // Temp is still valid → must go to ResetPassword (no full session yet)
                pass.postValue(LoginResult.mustReset(user));
                return;
            }

            Session.get().logIn(user, context);
            pass.postValue(new LoginResult(LoginResult.Code.OK, user));
        });
        return pass;
    }

    public LiveData<User> insertUser(String userName, String plainPassword) {
        MutableLiveData<User> registered = new MutableLiveData<>();
        executor.execute(() -> {
            boolean isFirstUser = mUserDAO.userCount() == 0;
            String hash = hash(plainPassword);

            User toInsert = new User(userName, hash);
            toInsert.setFirstName("Default");
            toInsert.setLastName("User");
            toInsert.setAdmin(isFirstUser);
            try {
                long id = mUserDAO.insert(toInsert);
                Log.d("DEBUG_INSERT", "Insert result: " + id);
                if (id > 0) {
                    toInsert.setUserID(id);
                    Session.get().logIn(toInsert, context);
                    registered.postValue(toInsert);
                } else {
                    registered.postValue(null);
                }
            } catch (Exception e) {
                Log.e("DEBUG_INSERT", "Insert crashed", e);
                registered.postValue(null);
            }
        });
        return registered;
    }

    public LiveData<User> addUser(User user, @NonNull String plainPassword) {
        MutableLiveData<User> result = new MutableLiveData<>();
        executor.execute(() -> {
            String hash = hash(plainPassword);
            user.setHash(hash);

            long id = mUserDAO.insert(user);
            if (id > 0) {
                user.setUserID(id);
                OffsetDateTime expires = OffsetDateTime.now().plusHours(24);
                mUserDAO.updatePassword(id, hash, expires, /*mustChange=*/true);
                result.postValue(user);
            } else {
                result.postValue(null);
            }
        });
        return result;
    }

    public void updateUser(User user) { executor.execute(() -> mUserDAO.update(user)); }
    public void deleteUser(User user) { executor.execute(() -> mUserDAO.delete(user)); }

    public interface TempPwdCallback { void onResult(boolean ok, String plainTemp); }

    public LiveData<String> resetPassword(long userID) {
        MutableLiveData<String> out = new MutableLiveData<>();
        issueTempPassword(userID, (ok, plainTemp) -> out.postValue(ok ? plainTemp : null));
        return out;
    }

    public void issueTempPassword(long userId, TempPwdCallback cb) {
        executor.execute(() -> {
            try {
                String tmp = mUserDAO.setTempPassword(userId);
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(true, tmp));
            } catch (Exception ex) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(false, null));
            }
        });
    }

    public LiveData<Boolean> changePassword(long userId, @NonNull String plainPwd) {
        MutableLiveData<Boolean> ok = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                mUserDAO.changePassword(userId, hash(plainPwd));
                ok.postValue(true);
            } catch (Exception ex) {
                ok.postValue(false);
            }
        });
        return ok;
    }

    private @Nullable String canonicalOrNull(String raw) {
        try {
            return toCanonical(raw);
        } catch (IllegalArgumentException ex) {
            showToast("Unsupported barcode");
            return null;
        }
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }
}
