package com.bestbymanager.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.bestbymanager.app.data.database.Repository;
import com.bestbymanager.app.data.entities.Employee;
import java.util.List;

public class EmployeeListViewModel extends AndroidViewModel {
    private final LiveData<List<Employee>> employees;
    public EmployeeListViewModel(@NonNull Application app) {
        super(app);
        Repository repository = new Repository(app);
        this.employees = repository.getEmployees();
    }
    public LiveData<List<Employee>> getEmployees() { return employees; }
}