package com.example.ggwave;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ggwave.databinding.ItemAttendanceBinding;

import java.util.Objects;

public class AttendanceAdapter extends ListAdapter<Attendance, RecyclerView.ViewHolder> {
    protected AttendanceAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AttendanceViewHolder(ItemAttendanceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Attendance attendance = getItem(position);
        ((AttendanceViewHolder) holder).bind(attendance);
    }

    public static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceBinding binding;
        public AttendanceViewHolder(ItemAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        public void bind(Attendance attendance) {
            binding.tvId.setText(attendance.getId());
            binding.tvName.setText(attendance.getName());
            if(attendance.getAttended()) {
                binding.tvIsChecked.setText("Y");
            } else {
                binding.tvIsChecked.setText("N");
            }
        }
    }

    public static class DiffCallback extends DiffUtil.ItemCallback<Attendance> {

        @Override
        public boolean areItemsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
            return oldItem.equals(newItem);
        }
    }

}
