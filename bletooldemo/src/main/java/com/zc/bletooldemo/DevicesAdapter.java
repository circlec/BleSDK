package com.zc.bletooldemo;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.zc.zbletool.ScanResult;

import java.util.ArrayList;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

    private ArrayList<ScanResult> results;
    private ItemClickListener mItemClickListener;

    public DevicesAdapter() {
        results = new ArrayList<>();
    }

    public void refreshData(ArrayList<ScanResult> newResults) {
        results.clear();
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(ItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view, mItemClickListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BluetoothDevice device = results.get(position).getDevice();
        int rssi = results.get(position).getRssi();
        if (device != null) {
            holder.tvName.setText(TextUtils.isEmpty(device.getName()) ? "unKnown" : device.getName());
            holder.tvMac.setText(TextUtils.isEmpty(device.getAddress()) ? "unKnown" : device.getAddress());
        } else {
            holder.tvName.setText("unKnown");
            holder.tvMac.setText("unKnown");
        }
        holder.tvRssi.setText(String.format("RSSI：%d", rssi));
        if (rssi >= -40) {
            holder.ratingBar.setRating(5);
        } else if (rssi < -40 && rssi >= -55) {
            holder.ratingBar.setRating(4);
        } else if (rssi < -55 && rssi >= -70) {
            holder.ratingBar.setRating(3);
        } else if (rssi < -70 && rssi >= -85) {
            holder.ratingBar.setRating(2);
        } else {
            holder.ratingBar.setRating(1);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView tvName;
        public TextView tvMac;
        public TextView tvRssi;
        public RatingBar ratingBar;
        private ItemClickListener mItemClickListener;

        public ViewHolder(View itemView, ItemClickListener mItemClickListener) {
            super(itemView);
            this.mItemClickListener = mItemClickListener;
            getItem(itemView);
            itemView.setOnClickListener(this);
        }

        private void getItem(View itemView) {
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
            tvMac = (TextView) itemView.findViewById(R.id.tv_mac);
            tvRssi = (TextView) itemView.findViewById(R.id.tv_rssi);
            ratingBar = (RatingBar) itemView.findViewById(R.id.rb_item);
        }

        @Override
        public void onClick(View v) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClickListener(v, getAdapterPosition());
            }
        }
    }
}
