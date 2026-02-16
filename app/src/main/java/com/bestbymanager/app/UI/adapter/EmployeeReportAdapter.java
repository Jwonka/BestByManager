package com.bestbymanager.app.UI.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bestbymanager.app.R;
import com.bestbymanager.app.data.pojo.EmployeeReportRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EmployeeReportAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<? extends EmployeeReportRow> getCurrentEmployeeList() { return data; }
    public interface OnRowClick { void onRowClick(long employeeID); }

    private final List<EmployeeReportRow> data = new ArrayList<>();
    private final OnRowClick listener;

    public EmployeeReportAdapter(OnRowClick l) { this.listener = l; }

    static final int TYPE_HEADER = 0;
    static final int TYPE_ROW    = 1;
    static final int TYPE_FOOTER = 2;

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView employeeName, fullName;
        public HeaderViewHolder(@NonNull View view) {
            super(view);
            employeeName = view.findViewById(R.id.header_employee_name);
        }
    }

    public static class RowViewHolder extends RecyclerView.ViewHolder {
        TextView brand, product, good, expired, discarded, total;
        public RowViewHolder(@NonNull View view) {
            super(view);
            brand = view.findViewById(R.id.row_brand);
            product = view.findViewById(R.id.row_product_name);
            good = view.findViewById(R.id.row_good_count);
            expired = view.findViewById(R.id.row_expired_count);
            discarded = view.findViewById(R.id.row_discarded_count);
            total = view.findViewById(R.id.row_total_count);
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        TextView title, good, expired, discarded, total;
        public FooterViewHolder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.footer_title);
            good = view.findViewById(R.id.footer_good_total);
            expired = view.findViewById(R.id.footer_expired_total);
            discarded = view.findViewById(R.id.footer_discarded_total);
            total = view.findViewById(R.id.footer_total);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= data.size()) return TYPE_ROW;
        EmployeeReportRow row = data.get(position);
        if (row.isFooter) return TYPE_FOOTER;
        if (row.isHeader) return TYPE_HEADER;
        return TYPE_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.employee_report_header, parent, false);
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_FOOTER) {
            View v = inflater.inflate(R.layout.employee_report_footer, parent, false);
            return new FooterViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.employee_report_row_item, parent, false);
            return new RowViewHolder(v);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (data.isEmpty() || position >= data.size()) return;
        EmployeeReportRow item = data.get(position);

        int discarded = item.discardedCount == null ? 0 : item.discardedCount;
        int lots = item.lotCount == null ? 0 : item.lotCount;

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).employeeName.setText("Employee Name: " + item.employeeName);

        } else if (holder instanceof RowViewHolder) {
            RowViewHolder rowHolder = (RowViewHolder) holder;
            rowHolder.brand.setText(item.brand);
            rowHolder.product.setText(item.productName);
            rowHolder.good.setText("Good: " + item.goodCount);
            rowHolder.expired.setText("Expired: " + item.expiredCount);
            rowHolder.discarded.setText("Discarded: " + discarded);
            rowHolder.total.setText("Total Units: " + item.totalCount + " • Lots: " + lots);
            rowHolder.itemView.setOnClickListener(v -> listener.onRowClick(item.employeeID));

        } else if (holder instanceof FooterViewHolder) {
            FooterViewHolder footHolder = (FooterViewHolder) holder;
            footHolder.title.setText("Summary for " + item.employeeName);
            footHolder.good.setText("Good: " + item.goodCount);
            footHolder.expired.setText("Expired: " + item.expiredCount);
            footHolder.discarded.setText("Discarded: " + discarded);
            footHolder.total.setText("Total Units: " + item.totalCount + " • Lots: " + lots);
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    public void setEmployeeList(List<EmployeeReportRow> incoming) {
        if (incoming == null || incoming.isEmpty()) return;

        final List<EmployeeReportRow> newData = new ArrayList<>();

        Map<Long, List<EmployeeReportRow>> grouped = incoming.stream()
                .collect(Collectors.groupingBy(r -> r.employeeID, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Long, List<EmployeeReportRow>> entry : grouped.entrySet()) {
            List<EmployeeReportRow> groupRows = entry.getValue();
            if (groupRows == null || groupRows.isEmpty()) continue;

            EmployeeReportRow sample = groupRows.get(0);

            EmployeeReportRow header = new EmployeeReportRow();
            header.isHeader = true;
            header.employeeID = sample.employeeID;
            header.employeeName = sample.employeeName;
            newData.add(header);

            newData.addAll(groupRows);

            EmployeeReportRow footer = new EmployeeReportRow();
            footer.isFooter = true;
            footer.employeeID = sample.employeeID;
            footer.employeeName = sample.employeeName;
            footer.goodCount = groupRows.stream().mapToInt(r -> r.goodCount != null ? r.goodCount : 0).sum();
            footer.expiredCount = groupRows.stream().mapToInt(r -> r.expiredCount != null ? r.expiredCount : 0).sum();
            footer.discardedCount = groupRows.stream().mapToInt(r -> r.discardedCount != null ? r.discardedCount : 0).sum();
            footer.totalCount = groupRows.stream().mapToInt(r -> r.totalCount != null ? r.totalCount : 0).sum();
            footer.lotCount = groupRows.stream().mapToInt(r -> r.lotCount != null ? r.lotCount : 0).sum();
            newData.add(footer);
        }

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return data.size(); }
            @Override public int getNewListSize() { return newData.size(); }
            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                EmployeeReportRow a = data.get(oldPos);
                EmployeeReportRow b = newData.get(newPos);
                return a.employeeID == b.employeeID &&
                        Objects.equals(a.brand, b.brand) &&
                        Objects.equals(a.productName, b.productName) &&
                        a.isHeader == b.isHeader &&
                        a.isFooter == b.isFooter;
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                EmployeeReportRow a = data.get(oldPos);
                EmployeeReportRow b = newData.get(newPos);
                return Objects.equals(a.employeeName, b.employeeName)
                        && Objects.equals(a.brand, b.brand)
                        && Objects.equals(a.productName, b.productName)
                        && Objects.equals(a.goodCount, b.goodCount)
                        && Objects.equals(a.expiredCount, b.expiredCount)
                        && Objects.equals(a.discardedCount, b.discardedCount)
                        && Objects.equals(a.totalCount, b.totalCount)
                        && Objects.equals(a.lotCount, b.lotCount)
                        && a.isHeader == b.isHeader
                        && a.isFooter == b.isFooter;
            }
        });

        data.clear();
        data.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }
}