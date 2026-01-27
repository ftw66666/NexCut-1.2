package com.example.opencv.device;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.opencv.R;

import java.util.List;

public class device_InfoItemAdapter extends RecyclerView.Adapter<device_InfoItemAdapter.ViewHolder> {
    private final List<DeviceDataItem> dataList;

    public device_InfoItemAdapter(List<DeviceDataItem> dataList) {
        this.dataList = dataList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView valueTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            valueTextView = itemView.findViewById(R.id.valueTextView);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_infoitem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DeviceDataItem item = dataList.get(position);
        holder.nameTextView.setText(item.getName());
        holder.valueTextView.setText(item.getValue());
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }


}
