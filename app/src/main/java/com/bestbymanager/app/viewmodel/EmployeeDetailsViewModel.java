package com.bestbymanager.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.Transformations;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Employee;

public class EmployeeDetailsViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<Employee> employee;

    public LiveData<Employee> getEmployee() { return employee; }

    public EmployeeDetailsViewModel(@NonNull Application app, SavedStateHandle handle) {
        super(app);
        repository = new Repository(app);

        LiveData<Long> idLive = handle.getLiveData("employeeID", -1L);

        employee = Transformations.switchMap(idLive, id -> {
            if (id == null || id <= 0) {
                return new MutableLiveData<>(null);
            } else {
                return repository.getEmployee(id);
            }
        });
    }
    public LiveData<Employee> addEmployee(Employee employee, String plainPassword) { return repository.addEmployee(employee, plainPassword); }
    public LiveData<Boolean> updateGuarded(Employee employee, long ownerId) { return repository.updateEmployeeGuarded(employee, ownerId); }
    public LiveData<Boolean> deleteGuarded(long targetEmployeeId, long ownerId) { return repository.deleteEmployeeGuarded(targetEmployeeId, ownerId); }
    public LiveData<String> resetPassword(long employeeID) { return repository.resetPassword(employeeID);}
    public LiveData<Boolean> clearEmployeePin(long employeeId) { return repository.clearEmployeePin(employeeId); }
    public LiveData<Employee> findByName(String name) {
        MutableLiveData<Employee> out = new MutableLiveData<>();
        new Thread(() -> out.postValue(repository.findByNameBlocking(name))).start();
        return out;
    }
}
