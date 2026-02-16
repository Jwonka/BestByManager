package com.bestbymanager.app.UI.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bestbymanager.app.R;
import com.bestbymanager.app.data.entities.Employee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {
    public interface OnEmployeeClick { void onEmployeeClick(long employeeID); }
    private final List<Employee> data = new ArrayList<>();
    private final OnEmployeeClick listener;

    public EmployeeAdapter(OnEmployeeClick l) { this.listener = l; }

    public static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView employeeName, fullname;
        EmployeeViewHolder(View view) {
            super(view);
            employeeName = view.findViewById(R.id.entered_by);
            fullname = view.findViewById(R.id.full_name);
        }
    }

    @NonNull
    @Override
    public EmployeeAdapter.EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.employee_list_item, parent, false);
        return new EmployeeAdapter.EmployeeViewHolder(row);
    }

    @Override
    public void onBindViewHolder(
            @NonNull EmployeeAdapter.EmployeeViewHolder holder, int position) {

        Employee employee = data.get(position);
        holder.employeeName.setText(employee.getEmployeeName());
        holder.itemView.setOnClickListener(v -> listener.onEmployeeClick(employee.getEmployeeID()));
    }

    @Override public int getItemCount() { return data.size(); }

    public void setEmployees(List<Employee> incoming) {

        final List<Employee> newData = incoming == null ? Collections.emptyList() : incoming;

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return data.size(); }
            @Override public int getNewListSize() { return newData.size(); }

            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                return data.get(oldPos).getEmployeeID() == newData.get(newPos).getEmployeeID();
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                Employee a = data.get(oldPos);
                Employee b = newData.get(newPos);
                return Objects.equals(a.getEmployeeName(), b.getEmployeeName());
            }
        });

        data.clear();
        data.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }
}
