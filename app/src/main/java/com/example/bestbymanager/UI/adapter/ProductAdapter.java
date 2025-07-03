package com.example.bestbymanager.UI.adapter;

import androidx.recyclerview.widget.DiffUtil;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.entities.Product;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    public interface OnProductClick { void onProductClick(long productID); }
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM/dd/yy");
    private final List<Product> data = new ArrayList<>();
    private final OnProductClick listener;

    public ProductAdapter(OnProductClick l) { this.listener = l; }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView brand, name, date;
        ProductViewHolder(View view) {
            super(view);
            brand = view.findViewById(R.id.brand);
            name  = view.findViewById(R.id.product_name);
            date  = view.findViewById(R.id.expiration_date);
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_list_item, parent, false);
        return new ProductViewHolder(row);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder, int position) {

        Product product = data.get(position);

        holder.brand.setText(product.getBrand());
        holder.name .setText(product.getProductName());
        holder.date .setText(product.getExpirationDate().format(FMT));

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product.getProductID()));
    }

    @Override public int getItemCount() { return data.size(); }

    public void setProducts(List<Product> incoming) {

        final List<Product> newData = incoming == null ? Collections.emptyList() : incoming;

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return data.size(); }
            @Override public int getNewListSize() { return newData.size(); }

            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                return data.get(oldPos).getProductID()
                        == newData.get(newPos).getProductID();
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                Product a = data.get(oldPos);
                Product b = newData.get(newPos);
                return Objects.equals(a.getBrand(),       b.getBrand()) &&
                        Objects.equals(a.getProductName(), b.getProductName()) &&
                        a.getExpirationDate().equals(b.getExpirationDate()) &&
                        a.getQuantity() == b.getQuantity();
            }
        });

        data.clear();
        data.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }
}

