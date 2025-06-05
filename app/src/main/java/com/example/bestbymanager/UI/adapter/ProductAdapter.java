package com.example.bestbymanager.UI.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.entities.Product;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    public interface OnProductClick { void onProductClick(int productId); }
    private List<Product> mProducts = new ArrayList<>();
    private final OnProductClick listener;
    public ProductAdapter(OnProductClick listener){ this.listener = listener; }
    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final TextView productItemView;
        public ProductViewHolder(View itemView) {
            super(itemView);
            productItemView = itemView.findViewById(R.id.product_item);
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.product_list_item,parent,false);
        return new ProductViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product current = mProducts.get(position);
        holder.productItemView.setText(current.getProductName());
        holder.itemView.setOnClickListener(view -> listener.onProductClick(current.getProductID()));
    }

    public void setVacations(List<Product> products){
        mProducts = products;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return mProducts.size(); }
}

