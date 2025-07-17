package com.example.bestbymanager.UI.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bestbymanager.R;
import com.example.bestbymanager.data.pojo.UserReportRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<? extends UserReportRow> getCurrentUserList() { return data; }
    public interface OnRowClick { void onRowClick(long userID);}
    private final List<UserReportRow> data = new ArrayList<>();
    private final UserReportAdapter.OnRowClick listener;
    public UserReportAdapter(UserReportAdapter.OnRowClick l) {
        this.listener = l;
    }
    static final int TYPE_HEADER = 0;
    static final int TYPE_ROW    = 1;
    static final int TYPE_FOOTER = 2;

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView userName, fullName;
        public HeaderViewHolder(@NonNull View view) {
            super(view);
            userName = view.findViewById(R.id.header_user_name);
            fullName = view.findViewById(R.id.header_full_name);
        }
    }

    public static class RowViewHolder extends RecyclerView.ViewHolder {
        TextView brand, product, good, expired, total;
        public RowViewHolder(@NonNull View view) {
            super(view);
            brand = view.findViewById(R.id.row_brand);
            product = view.findViewById(R.id.row_product_name);
            good = view.findViewById(R.id.row_good_count);
            expired = view.findViewById(R.id.row_expired_count);
            total = view.findViewById(R.id.row_total_count);
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        TextView title, good, expired, total;
        public FooterViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.footer_title);
            good = view.findViewById(R.id.footer_good_total);
            expired = view.findViewById(R.id.footer_expired_total);
            total = view.findViewById(R.id.footer_total);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= data.size()) { return TYPE_ROW; }
        UserReportRow row = data.get(position);
        if (row.isFooter) return TYPE_FOOTER;
        if (row.isHeader) return TYPE_HEADER;
        return TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.user_report_header, parent, false);
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_FOOTER) {
            View v = inflater.inflate(R.layout.user_report_footer, parent, false);
            return new FooterViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.user_report_row_item, parent, false);
            return new RowViewHolder(v);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (data.isEmpty() || position >= data.size()) return;
        UserReportRow item = data.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).fullName.setText("Employee: " + item.firstName + " " + item.lastName);
            ((HeaderViewHolder) holder).userName.setText("Username: " + item.userName);

        } else if (holder instanceof RowViewHolder) {
            RowViewHolder rowHolder = (RowViewHolder) holder;
            rowHolder.brand.setText(item.brand);
            rowHolder.product.setText(item.productName);
            rowHolder.good.setText("Good: " + item.goodCount);
            rowHolder.expired.setText("Expired: " + item.expiredCount);
            rowHolder.total.setText("Total: " + item.totalCount);
            rowHolder.itemView.setOnClickListener(v -> listener.onRowClick(item.userID));

        } else if (holder instanceof FooterViewHolder) {
            FooterViewHolder footHolder = (FooterViewHolder) holder;
            footHolder.title.setText("Summary for " + item.userName);
            footHolder.good.setText("Good: " + item.goodCount);
            footHolder.expired.setText("Expired: " + item.expiredCount);
            footHolder.total.setText("Total: " + item.totalCount);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    public void setUserList(List<UserReportRow> incoming) {
        if (incoming == null || incoming.isEmpty()) { return; }

        final List<UserReportRow> newData = new ArrayList<>();

        Map<Long, List<UserReportRow>> grouped = incoming.stream().collect(Collectors.groupingBy(r -> r.userID));

        for (Map.Entry<Long, List<UserReportRow>> entry : grouped.entrySet()) {
            List<UserReportRow> groupRows = entry.getValue();
            if (groupRows == null || groupRows.isEmpty()) { continue; }
            UserReportRow sample = groupRows.get(0);
            UserReportRow header = new UserReportRow();
            header.isHeader = true;
            header.userID = sample.userID;
            header.userName = sample.userName;
            header.firstName = sample.firstName;
            header.lastName = sample.lastName;
            newData.add(header);

            newData.addAll(groupRows);

            UserReportRow footer = new UserReportRow();
            footer.isFooter = true;
            footer.userID = sample.userID;
            footer.userName = sample.userName;
            footer.goodCount = groupRows.stream().mapToInt(r -> r.goodCount != null ? r.goodCount : 0).sum();
            footer.expiredCount = groupRows.stream().mapToInt(r -> r.expiredCount != null ? r.expiredCount : 0).sum();
            footer.totalCount = groupRows.stream().mapToInt(r -> r.totalCount != null ? r.totalCount : 0).sum();
            newData.add(footer);
        }


        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return data.size(); }
            @Override public int getNewListSize() { return newData.size(); }
            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                UserReportRow a = data.get(oldPos);
                UserReportRow b = newData.get(newPos);
                return a.userID == b.userID &&
                        Objects.equals(a.brand, b.brand) &&
                        Objects.equals(a.productName, b.productName);
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                return Objects.equals(data.get(oldPos), newData.get(newPos));
            }
        });

        data.clear();
        data.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }
}