package com.bestbymanager.app.data.database;

import com.bestbymanager.app.data.entities.DiscardEvent;
import static com.bestbymanager.app.utilities.BarcodeUtil.toCanonical;
import static com.bestbymanager.app.utilities.PasswordUtil.hash;
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
import com.bestbymanager.app.UI.adapter.LoginResult;
import com.bestbymanager.app.session.Session;
import com.bestbymanager.app.data.api.OffApi;
import com.bestbymanager.app.data.api.ProductResponse;
import com.bestbymanager.app.data.dao.ProductDAO;
import com.bestbymanager.app.data.dao.UserDAO;
import com.bestbymanager.app.data.entities.Product;
import com.bestbymanager.app.data.entities.User;
import com.bestbymanager.app.data.pojo.ProductReportRow;
import com.bestbymanager.app.data.pojo.UserReportRow;
import com.bestbymanager.app.utilities.AlarmScheduler;
import com.bestbymanager.app.utilities.PasswordUtil;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
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
    private static final int EARLY_WARNING_DAYS = 7;
    @SuppressWarnings("unchecked")
    private static <T> LiveData<T> emptyLiveData() { return (LiveData<T>) EMPTY; }
    private static final int PIN_MAX_ATTEMPTS = 5;
    private static final long PIN_LOCKOUT_MS = 5L * 60L * 1000L;

    public Repository(Application application) {
        this.context = application.getApplicationContext();
        ProductDatabaseBuilder db = ProductDatabaseBuilder.getDatabase(application);
        mUserDAO=db.userDAO();
        mProductDAO=db.productDAO();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://world.openfoodfacts.org/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(OffApi.class);
    }
    public Long getFirstAdminIdBlocking() { return mUserDAO.getFirstAdminId(); }
    public int userCountBlocking() { return mUserDAO.userCount(); }
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
    public Call<ProductResponse> fetchProduct(String raw, Callback<ProductResponse> cb) {
        String code = canonicalOrNull(raw);
        if (code == null) return null;

        Call<ProductResponse> call = api.getByBarcode(code);
        call.enqueue(cb);
        return call;
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
    public void discardExpiredProduct(long productID, int quantity, @Nullable String reason, @Nullable Long userId) {
        executor.execute(() -> {
            if (quantity <= 0) {
                showToast("Discard quantity must be > 0.");
                return;
            }
            DiscardEvent event = new DiscardEvent(productID, userId, quantity, reason, LocalDate.now());
            boolean ok = mProductDAO.discardProduct(event);
            if (!ok) {
                showToast("Not enough on-hand quantity to discard.");
            } else {
                showToast("Discard recorded.");
                int q = mProductDAO.getQuantityBlocking(productID);
                if (q <= 0) {
                    mProductDAO.clearEarlyWarning(productID);
                    int cancelled = AlarmScheduler.cancelAll(context, productID);
                    if (cancelled != 0) showToast("Reminders cleared (qty is 0).");
                }
            }
        });
    }

    public long insertProductBlocking(Product product) {
        try {
            product.setBarcode(toCanonical(product.getBarcode()));
        } catch (IllegalArgumentException ex) {
            showToast("Unsupported or unreadable barcode");
            return -1L;
        }

        long id = mProductDAO.insert(product); // IGNORE -> -1 on conflict
        if (id == -1L) {
            // Conflict: product already exists for this barcode (likely unique constraint)
            Product existing = mProductDAO.getLatestByBarcodeForUser(product.getUserID(), product.getBarcode());
            if (existing != null) return existing.getProductID();
            showToast("Product already exists.");
            return -1L;
        }

        product.setProductID(id);
        AlarmScheduler.scheduleAlarm(context, product.getExpirationDate(), id,
                product.getProductName() + " expires today.");

        if (product.isEarlyWarningEnabled()) {
            LocalDate earlyDate = product.getExpirationDate().minusDays(EARLY_WARNING_DAYS);
            if (!earlyDate.isBefore(LocalDate.now())) {
                AlarmScheduler.scheduleEarlyWarning(context, earlyDate, id,
                        product.getProductName() + " expires in 7 days.");
            }
        }

        return id;
    }

    public int updateProductBlocking(Product product) {
        try {
            product.setBarcode(toCanonical(product.getBarcode()));
        } catch (IllegalArgumentException ex) {
            showToast("Unsupported or unreadable barcode");
            return 0;
        }

        boolean wasEarly = mProductDAO.getEarlyWarningEnabledBlocking(product.getProductID()) == 1;

        // If qty is 0, early warning must be forced off (persisted), otherwise UI will reload it as ON
        if (product.getQuantity() <= 0 && product.isEarlyWarningEnabled()) {
            product.setEarlyWarningEnabled(false);
        }

        int rows = mProductDAO.updateProduct(product);
        if (rows <= 0) return 0;

        int cancelled = AlarmScheduler.cancelAll(context, product.getProductID());

        if (product.getQuantity() > 0) {
            AlarmScheduler.scheduleAlarm(context, product.getExpirationDate(), product.getProductID(),
                    product.getProductName() + " expires today.");

            if (product.isEarlyWarningEnabled()) {
                LocalDate earlyDate = product.getExpirationDate().minusDays(EARLY_WARNING_DAYS);
                if (!earlyDate.isBefore(LocalDate.now())) {
                    AlarmScheduler.scheduleEarlyWarning(context, earlyDate, product.getProductID(),
                            product.getProductName() + " expires in 7 days.");
                }
            }

            // toast only when user turned early warning OFF (qty still > 0)
            if (wasEarly && !product.isEarlyWarningEnabled()) {
                showToast("Early reminder cleared.");
            }
        } else {
            if (cancelled != 0) showToast("Reminders cleared (qty is 0).");
        }
        return rows;
    }

    public void deleteProduct(Product product) { executor.execute(() -> {
            int rows = mProductDAO.deleteProduct(product);
            if (rows == 0) {
                showToast("Product not found.");
            }  else {
                AlarmScheduler.cancelAlarm(context, product.getProductID());
                AlarmScheduler.cancelEarlyWarning(context, product.getProductID());
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

    public static final class PinState {
        public final boolean hasPin;
        public final int failedAttempts;
        @Nullable public final Long lockedUntilMs;
        public final boolean locked;

        public PinState(boolean hasPin, int failedAttempts, @Nullable Long lockedUntilMs) {
            this.hasPin = hasPin;
            this.failedAttempts = failedAttempts;
            this.lockedUntilMs = lockedUntilMs;
            long now = System.currentTimeMillis();
            this.locked = lockedUntilMs != null && lockedUntilMs > now;
        }
    }

    public enum PinVerifyCode {
        OK,
        NO_PIN_SET,
        LOCKED,
        BAD_PIN
    }

    public static final class PinVerifyResult {
        public final PinVerifyCode code;
        @Nullable public final Long lockedUntilMs;
        public final int failedAttempts;

        public PinVerifyResult(PinVerifyCode code, @Nullable Long lockedUntilMs, int failedAttempts) {
            this.code = code;
            this.lockedUntilMs = lockedUntilMs;
            this.failedAttempts = failedAttempts;
        }
    }

    /** Read current PIN state for selection UI. */
    public LiveData<PinState> getEmployeePinState(long userId) {
        MutableLiveData<PinState> out = new MutableLiveData<>();
        executor.execute(() -> {
            String hash = mUserDAO.getEmployeePinHashBlocking(userId);
            int fails = mUserDAO.getEmployeePinFailedAttemptsBlocking(userId);
            Long lockedUntil = mUserDAO.getEmployeePinLockedUntilBlocking(userId);

            // auto-clear expired lockout
            if (lockedUntil != null && lockedUntil <= System.currentTimeMillis()) {
                mUserDAO.clearEmployeePinLockout(userId);
                fails = 0;
                lockedUntil = null;
            }

            boolean hasPin = hash != null && !hash.trim().isEmpty();
            out.postValue(new PinState(hasPin, fails, lockedUntil));
        });
        return out;
    }

    /** Set or replace PIN (hash + resets lockout/attempts). */
    public LiveData<Boolean> setEmployeePin(long userId, @NonNull String plainPin) {
        MutableLiveData<Boolean> out = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                String pin = plainPin.trim();
                if (!pin.matches("\\d{4,8}")) { // 4-8 digits
                    out.postValue(false);
                    return;
                }
                String hash = PasswordUtil.hash(pin);
                mUserDAO.setEmployeePinHash(userId, hash);
                out.postValue(true);
            } catch (Exception e) {
                out.postValue(false);
            }
        });
        return out;
    }

    /** Verify PIN + apply attempt/lockout policy. */
    public LiveData<PinVerifyResult> verifyEmployeePin(long userId, @NonNull String plainPin) {
        MutableLiveData<PinVerifyResult> out = new MutableLiveData<>();
        executor.execute(() -> {
            long now = System.currentTimeMillis();

            String storedHash = mUserDAO.getEmployeePinHashBlocking(userId);
            int fails = mUserDAO.getEmployeePinFailedAttemptsBlocking(userId);
            Long lockedUntil = mUserDAO.getEmployeePinLockedUntilBlocking(userId);

            // locked?
            if (lockedUntil != null && lockedUntil > now) {
                out.postValue(new PinVerifyResult(PinVerifyCode.LOCKED, lockedUntil, fails));
                return;
                // expired lockout -> clear
            } else if (lockedUntil != null && lockedUntil <= now) {
                mUserDAO.clearEmployeePinLockout(userId);
                fails = 0;
                lockedUntil = null;
            }

            boolean hasPin = storedHash != null && !storedHash.trim().isEmpty();
            if (!hasPin) {
                out.postValue(new PinVerifyResult(PinVerifyCode.NO_PIN_SET, null, fails));
                return;
            }

            boolean ok = BCrypt.checkpw(plainPin.trim(), storedHash);
            if (ok) {
                mUserDAO.clearEmployeePinLockout(userId);
                out.postValue(new PinVerifyResult(PinVerifyCode.OK, null, 0));
                return;
            }

            // bad pin -> increment + maybe lock
            mUserDAO.incrementEmployeePinFailedAttempts(userId);
            int newFails = fails + 1;

            if (newFails >= PIN_MAX_ATTEMPTS) {
                long until = now + PIN_LOCKOUT_MS;
                mUserDAO.setEmployeePinLockedUntil(userId, until);
                out.postValue(new PinVerifyResult(PinVerifyCode.LOCKED, until, newFails));
            } else {
                out.postValue(new PinVerifyResult(PinVerifyCode.BAD_PIN, null, newFails));
            }
        });
        return out;
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }
}
