package com.bestbymanager.app.UI.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bestbymanager.app.R;
import com.bestbymanager.app.data.entities.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    public interface OnUserClick { void onUserClick(long userID); }
    private final List<User> data = new ArrayList<>();
    private final UserAdapter.OnUserClick listener;

    public UserAdapter(UserAdapter.OnUserClick l) { this.listener = l; }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView username, fullname;
        UserViewHolder(View view) {
            super(view);
            username = view.findViewById(R.id.entered_by);
            fullname = view.findViewById(R.id.full_name);
        }
    }

    @NonNull
    @Override
    public UserAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_list_item, parent, false);
        return new UserAdapter.UserViewHolder(row);
    }

    @Override
    public void onBindViewHolder(
            @NonNull UserAdapter.UserViewHolder holder, int position) {

        User user = data.get(position);
        String name = user.getFirstName() + " " + user.getLastName();

        holder.username.setText(user.getUserName());
        holder.fullname.setText(name);
        holder.itemView.setOnClickListener(v -> listener.onUserClick(user.getUserID()));
    }

    @Override public int getItemCount() { return data.size(); }

    public void setUsers(List<User> incoming) {

        final List<User> newData = incoming == null ? Collections.emptyList() : incoming;

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return data.size(); }
            @Override public int getNewListSize() { return newData.size(); }

            @Override public boolean areItemsTheSame(int oldPos, int newPos) {
                return data.get(oldPos).getUserID() == newData.get(newPos).getUserID();
            }
            @Override public boolean areContentsTheSame(int oldPos, int newPos) {
                User a = data.get(oldPos);
                User b = newData.get(newPos);
                return Objects.equals(a.getUserName(), b.getUserName()) &&
                        Objects.equals(a.getFirstName(), b.getFirstName()) &&
                        Objects.equals(a.getLastName(),  b.getLastName());
            }
        });

        data.clear();
        data.addAll(newData);
        diff.dispatchUpdatesTo(this);
    }
}
