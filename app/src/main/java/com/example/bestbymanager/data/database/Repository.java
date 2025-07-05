package com.example.bestbymanager.data.database;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.data.api.OffApi;
import com.example.bestbymanager.data.api.ProductResponse;
import com.example.bestbymanager.data.dao.ProductDAO;
import com.example.bestbymanager.data.dao.UserDAO;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.entities.User;
import com.example.bestbymanager.data.pojo.ProductReportRow;
import com.example.bestbymanager.utilities.AlarmScheduler;
import com.example.bestbymanager.utilities.BarcodeUtil;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Repository {
    private final UserDAO mUserDAO;
    private final ProductDAO mProductDAO;
    private final Context context;
    public static final int NUMBER_OF_THREADS = 4;
    private final Executor executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private final OffApi api;

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

    public Product getRecentExpirationByBarcode(String code) { return mProductDAO.getRecentExpirationByBarcode(code); }
    public LiveData<List<Product>> getProductsByBarcode(String code) { return mProductDAO.getProductsByBarcode(code); }
    public void fetchProduct(String barcode, Callback<ProductResponse> cb) { api.getByBarcode(barcode).enqueue(cb); }
    public LiveData<List<Product>> getProducts(){ return mProductDAO.getProducts(); }
    public LiveData<Product> getProduct(long productID){ return mProductDAO.getProduct(productID); }
    public LiveData<User> getUser(long userID){ return mUserDAO.getUser(userID); }
    public LiveData<List<User>> getUsers(){ return mUserDAO.getUsers(); }
    public LiveData<List<ProductReportRow>> getExpiring(LocalDate from, LocalDate selected) {return mProductDAO.getExpiring(from, selected); }
    public LiveData<List<Product>> getProductsByDateRange(LocalDate from, LocalDate selected) {return mProductDAO.getProductsByDateRange(from, selected); }
    public LiveData<List<ProductReportRow>> getExpired(LocalDate today) {return mProductDAO.getExpired(today); }
    public LiveData<List<ProductReportRow>> getReportRowsByBarcode(String barcode) { return mProductDAO.getReportRowsByBarcode(barcode); }
    public void insertProduct(Product product) { executor.execute(() -> {
        try {
            product.setBarcode(BarcodeUtil.toCanonical(product.getBarcode()));
            Log.d("BARCODE-SAVE", BarcodeUtil.toCanonical(product.getBarcode()));
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
                product.setBarcode(BarcodeUtil.toCanonical(product.getBarcode()));
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

    public LiveData<User> login(String username, String plainPassword) {
        MutableLiveData<User> pass = new MutableLiveData<>();
        executor.execute(() -> {
            User user = mUserDAO.findByUsername(username);
            if(user != null && BCrypt.checkpw(plainPassword, user.getHash())){
                Session.get().logIn(user, context);
                pass.postValue(user);
            } else {
                pass.postValue(null);
            }
        });
        return pass;
    }

    public LiveData<User> insertUser(String userName, String plainPassword) {
        MutableLiveData<User> registered = new MutableLiveData<>();
        executor.execute(() -> {
            boolean isFirstUser = mUserDAO.userCount() == 0;
            String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

            User toInsert = new User(userName, hash);
            toInsert.setAdmin(isFirstUser);

            long id = mUserDAO.insert(toInsert);
            if (id > 0) {
                User user = new User(userName, hash);
                user.isAdmin = isFirstUser;
                Session.get().logIn(user, context);
                registered.postValue(user);
            } else {
                registered.postValue(null);
            }

        });
        return registered;
    }

    public void addUser(User user, String plainPassword) {
        executor.execute(() -> {
            user.setHash(BCrypt.hashpw(plainPassword, BCrypt.gensalt()));

            long id = mUserDAO.insert(user);
            if (id > 0) {
                showToast("User added.");
            } else {
                showToast("Username already taken.");
            }
        });
    }

    public void updateUser(User user, @Nullable String newPlainPassword) {
        executor.execute(() -> {
            if (newPlainPassword != null && !newPlainPassword.isEmpty()) {
                user.setHash(BCrypt.hashpw(newPlainPassword, BCrypt.gensalt()));
            }
            mUserDAO.update(user);
        });
    }
    public void deleteUser(User user) { executor.execute(() -> mUserDAO.delete(user)); }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }
}
