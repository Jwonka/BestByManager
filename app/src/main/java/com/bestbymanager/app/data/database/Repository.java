package com.bestbymanager.app.data.database;

import com.bestbymanager.app.data.entities.DiscardEvent;
import static com.bestbymanager.app.utilities.BarcodeUtil.toCanonical;
import static com.bestbymanager.app.utilities.PasswordUtil.hash;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.bestbymanager.app.UI.adapter.UnlockKioskResult;
import com.bestbymanager.app.data.entities.Employee;
import com.bestbymanager.app.data.pojo.EmployeeReportRow;
import com.bestbymanager.app.data.api.OffApi;
import com.bestbymanager.app.data.api.ProductResponse;
import com.bestbymanager.app.data.dao.ProductDAO;
import com.bestbymanager.app.data.dao.EmployeeDAO;
import com.bestbymanager.app.data.entities.Product;
import com.bestbymanager.app.data.pojo.ProductReportRow;
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
    private final EmployeeDAO mEmployeeDAO;
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
        BestByManagerDatabase db = BestByManagerDatabase.getDatabase(application);
        mEmployeeDAO = db.employeeDAO();
        mProductDAO = db.productDAO();

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

    public Long getFirstAdminIdBlocking() { return mEmployeeDAO.getFirstAdminId(); }
    public int employeeCountBlocking() { return mEmployeeDAO.employeeCount(); }
    public LiveData<List<ProductReportRow>> getAllProducts() { return mProductDAO.getAllProducts(); }
    public LiveData<List<EmployeeReportRow>> getAllEntries(LocalDate today) { return mEmployeeDAO.getAllEntries(today); }
    public LiveData<List<EmployeeReportRow>> getEntriesByEmployee(long employeeID, LocalDate today) { return mEmployeeDAO.getEntriesByEmployee(employeeID, today); }

    public LiveData<List<EmployeeReportRow>> getEntriesByBarcodeForRange(String raw, LocalDate from, LocalDate to, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mEmployeeDAO.getEntriesByBarcodeForRange(code, from, to, today);
    }

    public LiveData<List<EmployeeReportRow>> getEntriesForEmployeeAndBarcode(long employeeID, String raw, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mEmployeeDAO.getEntriesForEmployeeAndBarcode(employeeID, code, today);
    }

    public LiveData<List<EmployeeReportRow>> getEntriesByBarcodeForEmployeeInRange(long employeeID, String raw, LocalDate from, LocalDate to, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mEmployeeDAO.getEntriesByBarcodeForEmployeeInRange(employeeID, code, from, to, today);
    }

    public LiveData<List<EmployeeReportRow>> getEntriesForEmployeeInRange(long employeeID, LocalDate from, LocalDate to, LocalDate today) {
        return mEmployeeDAO.getEntriesForEmployeeInRange(employeeID, from, to, today);
    }

    public LiveData<List<EmployeeReportRow>> getEntriesByDateRange(LocalDate from, LocalDate to, LocalDate today) { return mEmployeeDAO.getEntriesByDateRange(from, to, today); }

    public LiveData<List<EmployeeReportRow>> getEntriesForBarcode(String raw, LocalDate today) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mEmployeeDAO.getEntriesByBarcode(code, today);
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
    public LiveData<Employee> getEmployee(long employeeID){ return mEmployeeDAO.getEmployee(employeeID); }
    public LiveData<List<Employee>> getEmployees(){ return mEmployeeDAO.getEmployees(); }
    public LiveData<List<ProductReportRow>> getExpiring(LocalDate from, LocalDate selected) { return mProductDAO.getExpiring(from, selected); }
    public LiveData<List<Product>> getProductsByDateRange(LocalDate from, LocalDate selected) { return mProductDAO.getProductsByDateRange(from, selected); }
    public LiveData<List<ProductReportRow>> getExpired(LocalDate today) { return mProductDAO.getExpired(today); }

    public LiveData<List<ProductReportRow>> getReportRowsByBarcode(String raw) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() :  mProductDAO.getReportRowsByBarcode(code);
    }

    public LiveData<List<ProductReportRow>> getProductsByBarcodeAndDateRange(String raw, LocalDate from, LocalDate to) {
        String code = canonicalOrNull(raw);
        return code == null ? emptyLiveData() : mProductDAO.getProductsByBarcodeAndDateRange(code, from, to);
    }

    public void discardExpiredProduct(long productID, int quantity, @Nullable String reason, @Nullable Long employeeId) {
        executor.execute(() -> {
            if (quantity <= 0) {
                showToast("Discard quantity must be > 0.");
                return;
            }
            DiscardEvent event = new DiscardEvent(productID, employeeId, quantity, reason, LocalDate.now());
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
            Product existing = mProductDAO.getLatestByBarcodeForEmployee(product.getEmployeeID(), product.getBarcode());
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

            if (wasEarly && !product.isEarlyWarningEnabled()) {
                showToast("Early reminder cleared.");
            }
        } else {
            if (cancelled != 0) showToast("Reminders cleared (qty is 0).");
        }
        return rows;
    }

    public void deleteProduct(Product product) {
        executor.execute(() -> {
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

    public LiveData<UnlockKioskResult> unlockKiosk(String employeeName, String plainPassword) {
        MutableLiveData<UnlockKioskResult> pass = new MutableLiveData<>();
        executor.execute(() -> {
            Employee employee = mEmployeeDAO.findByName(employeeName);

            boolean ok = employee != null
                    && employee.getHash() != null
                    && !employee.getHash().isEmpty()
                    && BCrypt.checkpw(plainPassword, employee.getHash());

            if (!ok) {
                pass.postValue(new UnlockKioskResult(UnlockKioskResult.Code.BAD_CREDENTIALS, null));
                return;
            }

            if (employee.isMustChange()) {
                OffsetDateTime until = employee.getResetExpires();
                if (until != null && OffsetDateTime.now().isAfter(until)) {
                    pass.postValue(UnlockKioskResult.expired());
                    return;
                }
                pass.postValue(UnlockKioskResult.mustReset(employee));
                return;
            }

            // IMPORTANT: no Session side-effects here anymore
            pass.postValue(new UnlockKioskResult(UnlockKioskResult.Code.OK, employee));
        });
        return pass;
    }

    public LiveData<Employee> insertEmployee(String employeeName, String plainPassword) {
        MutableLiveData<Employee> registered = new MutableLiveData<>();
        executor.execute(() -> {
            boolean isFirstEmployee = mEmployeeDAO.employeeCount() == 0;
            String hash = hash(plainPassword);

            Employee toInsert = new Employee(employeeName, hash);
            toInsert.setAdmin(isFirstEmployee);

            try {
                long id = mEmployeeDAO.insert(toInsert);
                if (id > 0) {
                    toInsert.setEmployeeID(id);

                    // IMPORTANT: no Session side-effects here anymore
                    registered.postValue(toInsert);
                } else {
                    registered.postValue(null);
                }
            } catch (Exception e) {
                registered.postValue(null);
            }
        });
        return registered;
    }

    public LiveData<Employee> addEmployee(Employee employee, @NonNull String plainPassword) {
        MutableLiveData<Employee> result = new MutableLiveData<>();
        executor.execute(() -> {
            String hash = hash(plainPassword);
            employee.setHash(hash);

            long id = mEmployeeDAO.insert(employee);
            if (id > 0) {
                employee.setEmployeeID(id);
                OffsetDateTime expires = OffsetDateTime.now().plusHours(24);
                mEmployeeDAO.updatePassword(id, hash, expires, /*mustChange=*/true);
                result.postValue(employee);
            } else {
                result.postValue(null);
            }
        });
        return result;
    }

    public void updateEmployee(Employee employee) { executor.execute(() -> mEmployeeDAO.update(employee)); }
    public void deleteEmployee(Employee employee) { executor.execute(() -> mEmployeeDAO.delete(employee)); }

    public interface TempPwdCallback { void onResult(boolean ok, String plainTemp); }

    public LiveData<String> resetPassword(long employeeID) {
        MutableLiveData<String> out = new MutableLiveData<>();
        issueTempPassword(employeeID, (ok, plainTemp) -> out.postValue(ok ? plainTemp : null));
        return out;
    }

    public void issueTempPassword(long employeeId, TempPwdCallback cb) {
        executor.execute(() -> {
            try {
                String tmp = mEmployeeDAO.setTempPassword(employeeId);
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(true, tmp));
            } catch (Exception ex) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(false, null));
            }
        });
    }

    public LiveData<Boolean> changePassword(long employeeId, @NonNull String plainPwd) {
        MutableLiveData<Boolean> ok = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                mEmployeeDAO.changePassword(employeeId, hash(plainPwd));
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
    public LiveData<PinState> getEmployeePinState(long employeeId) {
        MutableLiveData<PinState> out = new MutableLiveData<>();
        executor.execute(() -> {
            String hash = mEmployeeDAO.getEmployeePinHashBlocking(employeeId);
            int fails = mEmployeeDAO.getEmployeePinFailedAttemptsBlocking(employeeId);
            Long lockedUntil = mEmployeeDAO.getEmployeePinLockedUntilBlocking(employeeId);

            if (lockedUntil != null && lockedUntil <= System.currentTimeMillis()) {
                mEmployeeDAO.clearEmployeePinLockout(employeeId);
                fails = 0;
                lockedUntil = null;
            }

            boolean hasPin = hash != null && !hash.trim().isEmpty();
            out.postValue(new PinState(hasPin, fails, lockedUntil));
        });
        return out;
    }

    /** Set or replace PIN (hash + resets lockout/attempts). */
    public LiveData<Boolean> setEmployeePin(long employeeId, @NonNull String plainPin) {
        MutableLiveData<Boolean> out = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                String pin = plainPin.trim();
                if (!pin.matches("\\d{4,8}")) {
                    out.postValue(false);
                    return;
                }
                String hash = PasswordUtil.hash(pin);
                mEmployeeDAO.setEmployeePinHash(employeeId, hash);
                out.postValue(true);
            } catch (Exception e) {
                out.postValue(false);
            }
        });
        return out;
    }

    /** Verify PIN + apply attempt/lockout policy. */
    public LiveData<PinVerifyResult> verifyEmployeePin(long employeeId, @NonNull String plainPin) {
        MutableLiveData<PinVerifyResult> out = new MutableLiveData<>();
        executor.execute(() -> {
            long now = System.currentTimeMillis();

            String storedHash = mEmployeeDAO.getEmployeePinHashBlocking(employeeId);
            int fails = mEmployeeDAO.getEmployeePinFailedAttemptsBlocking(employeeId);
            Long lockedUntil = mEmployeeDAO.getEmployeePinLockedUntilBlocking(employeeId);

            if (lockedUntil != null) {
                if (lockedUntil > now) {
                    out.postValue(new PinVerifyResult(PinVerifyCode.LOCKED, lockedUntil, fails));
                    return;
                }
                mEmployeeDAO.clearEmployeePinLockout(employeeId);
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
                mEmployeeDAO.clearEmployeePinLockout(employeeId);
                out.postValue(new PinVerifyResult(PinVerifyCode.OK, null, 0));
                return;
            }

            mEmployeeDAO.incrementEmployeePinFailedAttempts(employeeId);
            int newFails = fails + 1;

            if (newFails >= PIN_MAX_ATTEMPTS) {
                long until = now + PIN_LOCKOUT_MS;
                mEmployeeDAO.setEmployeePinLockedUntil(employeeId, until);
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