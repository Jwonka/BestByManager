package com.example.bestbymanager.UI.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.pojo.ProductReportRow;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProductReportAdapter extends RecyclerView.Adapter<ProductReportAdapter.ReportViewHolder> {

    public List<ProductReportRow> getCurrentProductList() { return data; }

    public interface OnRowClick { void onRowClick(long productID); }
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd/yy");

    private final List<ProductReportRow> data = new ArrayList<>();
    private final OnRowClick listener;

    public ProductReportAdapter(OnRowClick l){ this.listener = l; }
    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView brand, productName, enteredBy, date, quantity;
        ReportViewHolder(View view) {
            super(view);
            brand = view.findViewById(R.id.brand);
            productName = view.findViewById(R.id.product_name);
            enteredBy = view.findViewById(R.id.username);
            date = view.findViewById(R.id.expiration_date);
            quantity = view.findViewById(R.id.expired_quantity);
        }
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View row = LayoutInflater.from(p.getContext()).inflate(R.layout.product_report_row_item, p, false);
        return new ReportViewHolder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ProductReportRow results = data.get(position);

        String expDate = "Expires: " + results.expirationDate.format(FMT);
        String qty = "Quantity: " + results.quantity;
        String name = "User: " + results.enteredBy;

        holder.brand.setText(results.brand);
        holder.productName.setText(results.productName);
        holder.enteredBy.setText(name);
        holder.date.setText(expDate);
        holder.quantity.setText(qty);

        holder.itemView.setOnClickListener(vacation1 -> listener.onRowClick(results.productID));
    }
    @Override
    public int getItemCount() { return data.size(); }

    public void setReportList(List<ProductReportRow> incoming) {

        final List<ProductReportRow> newData = incoming == null ? Collections.emptyList() : incoming;

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return data.size(); }
            @Override public int getNewListSize() { return newData.size(); }
            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                return data.get(oldPos).productID == newData.get(newPos).productID;
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                ProductReportRow a = data.get(oldPos);
                ProductReportRow b = newData.get(newPos);
                return a.quantity == b.quantity &&
                        a.expirationDate.equals(b.expirationDate) &&
                        Objects.equals(a.brand,       b.brand) &&
                        Objects.equals(a.productName, b.productName) &&
                        Objects.equals(a.enteredBy,   b.enteredBy);
            }
        });

        data.clear();
        data.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }
}
