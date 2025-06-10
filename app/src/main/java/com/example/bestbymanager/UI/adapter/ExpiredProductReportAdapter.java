package com.example.bestbymanager.UI.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.pojo.ExpiredProductReportRow;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ExpiredProductReportAdapter  extends RecyclerView.Adapter<ExpiredProductReportAdapter.ReportViewHolder> {
    public interface OnRowClick { void onRowClick(int productID); }
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd/yy");

    private List<ExpiredProductReportRow> data = new ArrayList<>();
    private final OnRowClick listener;

    public ExpiredProductReportAdapter(OnRowClick listener){ this.listener = listener; }
    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView name, date, count;
        ReportViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.name);
            date = view.findViewById(R.id.expiration_date);
            count = view.findViewById(R.id.expired_count);
        }
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View row = LayoutInflater.from(p.getContext()).inflate(R.layout.report_row_item, p, false);
        return new ReportViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ExpiredProductReportRow results = data.get(position);

        holder.name.setText(results.productName);
        holder.date.setText(results.expirationDate.format(FMT));
        holder.count.setText(String.valueOf(results.expiredCount));

        holder.itemView.setOnClickListener(vacation1 -> listener.onRowClick(results.productID));
    }

    @Override
    public int getItemCount() { return data.size(); }

    public void setReportList(List<ExpiredProductReportRow> list) {
        data = list == null ? new ArrayList<>() : list;
        notifyDataSetChanged();
    }
}

