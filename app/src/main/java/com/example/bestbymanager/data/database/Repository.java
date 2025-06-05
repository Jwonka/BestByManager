package com.example.bestbymanager.data.database;

import android.app.Application;
import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.bestbymanager.UI.authentication.Session;
import com.example.bestbymanager.data.dao.UserDAO;
import com.example.bestbymanager.data.entities.User;
import org.mindrot.jbcrypt.BCrypt;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Repository {
    private final UserDAO mUserDAO;

    private final Context context;
    public static final int NUMBER_OF_THREADS = 4;
    private final Executor executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public Repository(Application application) {
        this.context = application.getApplicationContext();
        DatabaseBuilder db = DatabaseBuilder.getDatabase(application);
        mUserDAO=db.userDAO();
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
}
