package com.example.bestbymanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import com.example.bestbymanager.data.database.Repository;
import com.example.bestbymanager.data.entities.User;

public class UserDetailsViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<User> user;

    public LiveData<User> getUser() { return user; }

    public UserDetailsViewModel(@NonNull Application app, SavedStateHandle handle) {
        super(app);
        repository = new Repository(app);

        LiveData<Long> idLive = handle.getLiveData("userID", -1L);

        user = Transformations.switchMap(idLive, id -> {
            if (id == null || id <= 0) {
                return new MutableLiveData<>(null);
            } else {
                return repository.getUser(id);
            }
        });
    }
    public LiveData<User> addUser(User user, String plainPassword) { return repository.addUser(user, plainPassword); }

    public void update(User user) { repository.updateUser(user); }

    public void delete(User user) { repository.deleteUser(user); }
    public LiveData<String> resetPassword(long userID) { return repository.resetPassword(userID);}
}
