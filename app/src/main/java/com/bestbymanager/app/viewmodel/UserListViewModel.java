package com.bestbymanager.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.User;
import java.util.List;

public class UserListViewModel  extends AndroidViewModel {
    private final LiveData<List<User>> users;
    private final LiveData<List<User>> admins;
    public UserListViewModel(@NonNull Application app) {
        super(app);
        Repository repository = new Repository(app);
        this.users = repository.getUsers();
        this.admins = repository.loadAdmins();
    }
    public LiveData<List<User>> getUsers() { return users; }
    public LiveData<List<User>> loadAdmins() { return admins; }
}