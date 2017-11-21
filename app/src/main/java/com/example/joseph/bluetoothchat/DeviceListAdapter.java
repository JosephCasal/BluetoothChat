package com.example.joseph.bluetoothchat;

import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 11/20/17.
 */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder>{

    List<BluetoothDevice> deviceList = new ArrayList<>();
    Context context;
    Activity activity;

    public DeviceListAdapter(Activity activity){
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        BluetoothDevice device = deviceList.get(position);
        holder.device = device;
        if(device.getName() == null){
            holder.tvName.setText(device.getAddress());
        }else{
            holder.tvName.setText(device.getName());
        }

    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void addDevice(BluetoothDevice device){
        deviceList.add(device);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        BluetoothDevice device;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    Intent intent = new Intent(context, ChatActivity.class);
//                    Bundle bundle = new Bundle();
//                    bundle.putParcelable("device", device);
//                    intent.putExtras(bundle);
//                    context.startActivity(intent);
                    ((MainActivity)activity).connectDevice(device);
                }
            });

        }
    }
}
