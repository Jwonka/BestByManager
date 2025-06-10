package com.example.bestbymanager.data.database;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.data.dao.ProductDAO;
import com.example.bestbymanager.data.dao.UserDAO;
import com.example.bestbymanager.data.entities.Product;
import com.example.bestbymanager.data.entities.User;
import com.example.bestbymanager.data.pojo.ExpiredProductReportRow;
import org.mindrot.jbcrypt.BCrypt;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Repository {
    private final UserDAO mUserDAO;
    private final ProductDAO mProductDAO;
    private final Context context;
    public static final int NUMBER_OF_THREADS = 4;
    private final Executor executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public Repository(Application application) {
        this.context = application.getApplicationContext();
        ProductDatabaseBuilder db = ProductDatabaseBuilder.getDatabase(application);
        mUserDAO=db.userDAO();
        mProductDAO=db.productDAO();
    }

    public LiveData<List<ExpiredProductReportRow>> searchReport(LocalDate date) { return mProductDAO.reportRows(date); }
    public LiveData<List<Product>> getProducts(){ return mProductDAO.getProducts(); }
    public LiveData<List<Product>> getExpiringSoon(LocalDate today, LocalDate selected) {return mProductDAO.getExpiringSoon(today, selected); }
    public LiveData<Product> getProduct(int productID){ return mProductDAO.getProduct(productID); }
    public void insertProduct(Product product) { executor.execute(() -> {
            long id = mProductDAO.insert(product);
            if (id == -1) {
                showToast("Product already exists.");
            }
        });
    }
    public void updateProduct(Product product) { executor.execute(() -> {
            int rows = mProductDAO.updateProduct(product);
            if (rows == 0) {
                showToast("Error - conflict on barcode or ID.");
            }
        });
    }
    public void deleteProduct(Product product) { executor.execute(() -> {
        int rows = mProductDAO.deleteProduct(product);
        if (rows == 0) {
            showToast("Product not found.");
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
                Session.get().logIn(user);
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
            toInsert.isAdmin = isFirstUser;

            long id = mUserDAO.insert(toInsert);
            if (id > 0) {
                User user = new User((int) id, userName, hash);
                user.isAdmin = isFirstUser;
                Session.get().logIn(user);
                registered.postValue(user);
            } else {
                registered.postValue(null);
            }

        });
        return registered;
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
    }
}
