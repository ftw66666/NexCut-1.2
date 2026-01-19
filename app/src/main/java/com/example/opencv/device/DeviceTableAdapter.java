package com.example.opencv.device;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opencv.R;

import java.util.List;
import java.util.Set;


public class DeviceTableAdapter extends RecyclerView.Adapter<DeviceTableAdapter.ViewHolder> {

    private final List<Device> deviceList;
    private final Set<Device> deviceSet;
    private final OnDeviceClickListener clickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public DeviceTableAdapter(List<Device> deviceList, Set<Device> deviceset, OnDeviceClickListener clickListener) {
        this.deviceList = deviceList;
        this.clickListener = clickListener;
        this.deviceSet = deviceset;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.device_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.ipTextView.setText(device.getIp());
        holder.modelTextView.setText(device.getModel());
        holder.deviceIdTextView.setText(device.getDeviceId());

        // 绑定点击事件
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ipTextView, portTextView, modelTextView, deviceIdTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ipTextView = itemView.findViewById(R.id.ipTextView);
            modelTextView = itemView.findViewById(R.id.modelTextView);
            deviceIdTextView = itemView.findViewById(R.id.deviceIdTextView);
        }
    }

    public void addData(Device device) {
        deviceList.add(device);
        deviceSet.add(device);
        this.notifyItemInserted(deviceList.size());
    }

    public void removeDevice(Device device) {
        int position = deviceList.indexOf(device);
        if (position != -1) {
            deviceList.remove(position);
            deviceSet.remove(device);
            this.notifyItemRemoved(position);
        }
    }


}