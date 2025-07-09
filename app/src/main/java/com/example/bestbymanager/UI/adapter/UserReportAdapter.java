package com.example.bestbymanager.UI.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.pojo.UserReportRow;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserReportAdapter extends RecyclerView.Adapter<UserReportAdapter.ReportViewHolder> {

    public List<? extends UserReportRow> getCurrentUserList() { return data; }
    public interface OnRowClick { void onRowClick(long userID);}
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd/yy");

    private final List<UserReportRow> data = new ArrayList<>();
    private final UserReportAdapter.OnRowClick listener;

    public UserReportAdapter(UserReportAdapter.OnRowClick l) {
        this.listener = l;
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userID, count;

        ReportViewHolder(View view) {
            super(view);
            userID = view.findViewById(R.id.full_name);
            userName = view.findViewById(R.id.username);
            count = view.findViewById(R.id.count);
        }
    }

    @NonNull
    @Override
    public UserReportAdapter.ReportViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View row = LayoutInflater.from(p.getContext()).inflate(R.layout.product_report_row_item, p, false);
        return new UserReportAdapter.ReportViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull UserReportAdapter.ReportViewHolder holder, int position) {
        UserReportRow results = data.get(position);


        String count = "Count: " + results.count;
        String name = "User: " + results.userName;
        String id = "UserID: " + results.userID;

        holder.userID.setText(id);
        holder.userName.setText(name);
        holder.count.setText(count);

        holder.itemView.setOnClickListener(vacation1 -> listener.onRowClick(results.userID));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void setUserList(List<UserReportRow> incoming) {

        final List<UserReportRow> newData = incoming == null ? Collections.emptyList() : incoming;

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return data.size();
            }

            @Override
            public int getNewListSize() {
                return newData.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return data.get(oldPos).userID == newData.get(newPos).userID;
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                UserReportRow a = data.get(oldPos);
                UserReportRow b = newData.get(newPos);
                return Objects.equals(a.userID, b.userID) &&
                        Objects.equals(a.userName, b.userName);
            }
        });

        data.clear();
        data.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }
}