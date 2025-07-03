package com.example.bestbymanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.data.entities.User;
import java.util.List;

public class UserListViewModel  extends AndroidViewModel {
    private final LiveData<List<User>> users;
    public UserListViewModel(@NonNull Application app) {
        super(app);
        Repository repository = new Repository(app);
        this.users = repository.getUsers();
    }
    public LiveData<List<User>> getUsers() { return users; }
}