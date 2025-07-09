package com.example.bestbymanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    public LiveData<User> addUser(User user, @Nullable String plainPassword) { return repository.addUser(user, plainPassword); }

    public void update(User user, @Nullable String plainPassword) { repository.updateUser(user, plainPassword); }

    public void delete(User user) { repository.deleteUser(user); }
    public void issueTempPassword(long userID, Repository.TempPwdCallback cb) { repository.issueTempPassword(userID, cb); }
}
