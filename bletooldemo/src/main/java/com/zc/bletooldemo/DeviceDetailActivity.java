package com.zc.bletooldemo;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.zc.zbletool.BleManager;
import com.zc.zbletool.Contants;
import com.zc.zbletool.ScanResult;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.zc.bletooldemo.R.id.tv_conn;
import static com.zc.bletooldemo.R.id.tv_device_address;
import static com.zc.bletooldemo.R.id.tv_device_connect;
import static com.zc.bletooldemo.R.id.tv_device_name;
import static com.zc.bletooldemo.R.id.tv_device_rssi;
import static com.zc.bletooldemo.R.id.tv_scan_record;
import static com.zc.bletooldemo.R.id.tv_show;

public class DeviceDetailActivity extends AppCompatActivity implements View.OnClickListener {
    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(tv_conn)
    TextView tvConn;
    @Bind(tv_device_name)
    TextView tvDeviceName;
    @Bind(tv_device_address)
    TextView tvDeviceAddress;
    @Bind(tv_device_connect)
    TextView tvDeviceConnect;
    @Bind(tv_device_rssi)
    TextView tvDeviceRssi;
    @Bind(tv_show)
    TextView tvShow;
    @Bind(tv_scan_record)
    TextView tvScanRecord;
    @Bind(R.id.expandableListView)
    ExpandableListView expandableListView;
    private ScanResult scanResult;

    private BleManager manager;
    private MyRevicer aprilBeaconReceiver;

    private int lastRssi;

    private Handler handler;

    ExpandableListAdapter mAdapter;
    private List<BluetoothGattService> supportedGattServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        ButterKnife.bind(this);
        init();
        getIntentData();
        initView();
        connectDevice();
        regiestReciver();
    }

    private void getIntentData() {
        scanResult = getIntent().getParcelableExtra("scanResult");
    }

    private void init() {
        manager = new BleManager(this);
        aprilBeaconReceiver = new MyRevicer();
        handler = new Handler();
        supportedGattServices = new ArrayList<>();
    }

    private void regiestReciver() {
        registerReceiver(aprilBeaconReceiver, makeGattUpdateIntentFilter());
    }

    private void connectDevice() {
        manager.connectDevice(scanResult.getDevice().getAddress());
    }

    private void initView() {
        tvDeviceName.setText(scanResult.getDevice().getName());
        tvDeviceAddress.setText(scanResult.getDevice().getAddress());
        tvScanRecord.setText(scanResult.getHexScanRecord());
        tvShow.setOnClickListener(this);
        tvConn.setOnClickListener(this);

    }

    public class MyRevicer extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // *********************//
            if (action.equals(Contants.BLESDK_ACTION_GATT_CONNECTED)) {// 设备连接时接收的广播
                tvDeviceConnect.setText("Connected");
                tvConn.setText("disConnect");
                manager.readRemoteRssi();
            }

            // *********************//
            if (action.equals(Contants.BLESDK_ACTION_GATT_DISCONNECTED)) {// 设备断开连接时接收的广播
                tvDeviceConnect.setText("disConnected");
                tvConn.setText("Connect");
            }

            if (action.equals(Contants.BLESDK_ACTION_DATA_RESET)) {// 重启成功--修改成功广播
                Log.i("Test","BLESDK_ACTION_DATA_RESET");
            }

            // *********************//
            if (action
                    .equals(Contants.BLESDK_ACTION_GATT_SERVICES_DISCOVERED_NEW)) {// 连接成功发现服务后接收的广播
                supportedGattServices = manager.getSupportedGattServices();
                mAdapter = new MyExpandableListAdapter();
                expandableListView.setAdapter(mAdapter);
                expandableListView.setAddStatesFromChildren(false);
                expandableListView.setGroupIndicator(null);
            }
            // *********************//
            if (action.equals(Contants.BLESDK_ACTION_DATA_AVAILABLE)) {// 接收值读取时的广播
                Log.i("Test","BLESDK_ACTION_DATA_AVAILABLE");

            }
            if (action.equals(Contants.BLESDK_ACTION_DATA_CHANGE)) {// 接收值改变时的广播
                Log.i("Test","BLESDK_ACTION_DATA_CHANGE");

            }
            if (action.equals(Contants.BLESDK_ACTION_DATA_WRITE)) {// 接收值写入时的广播
                Log.i("Test","BLESDK_ACTION_DATA_WRITE");
            }

            // *********************//
            if (action.equals(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART)) {// 设备不支持UART时的广播
                Log.i("Test","BLESDK_DEVICE_DOES_NOT_SUPPORT_UART");

            }
            if (action.equals(Contants.BLESDK_ACTION_READ_REMOTE_RSSI)) {// 读取RSSI
                int rssi = intent.getIntExtra(Contants.BLESDK_INT_DATA, -1);
                if (lastRssi != rssi) {
                    lastRssi = rssi;
                    tvDeviceRssi.setText(rssi + " db");
                }
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        manager.readRemoteRssi();
                    }
                }, 1000);
            }
        }
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Contants.BLESDK_ACTION_GATT_CONNECTED);
        intentFilter.addAction(Contants.BLESDK_ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(Contants.BLESDK_ACTION_GATT_SERVICES_DISCOVERED_NEW);
        intentFilter.addAction(Contants.BLESDK_ACTION_DATA_AVAILABLE);
        intentFilter.addAction(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(Contants.BLESDK_ACTION_DATA_CHANGE);
        intentFilter.addAction(Contants.BLESDK_ACTION_DATA_WRITE);
        intentFilter.addAction(Contants.BLESDK_ACTION_DATA_RESET);
        intentFilter.addAction(Contants.BLESDK_ACTION_DATA_DESCRIPTORWRITE);
        intentFilter.addAction(Contants.BLESDK_ACTION_DATA_WRITE_ERROR);
        intentFilter
                .addAction(Contants.BLESDK_ACTION_GATT_SERVICES_DISCOVERED_ERROR);
        intentFilter
                .addAction(Contants.BLESDK_ACTION_DATA_WRITE_PASSWORD_ERROR);
        intentFilter.addAction(Contants.BLESDK_ACTION_READ_REMOTE_RSSI);
        return intentFilter;
    }

    public class MyExpandableListAdapter extends BaseExpandableListAdapter {

        public Object getChild(int groupPosition, int childPosition) {
            return supportedGattServices.get(groupPosition)
                    .getCharacteristics().get(childPosition);
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        public int getChildrenCount(int groupPosition) {
            int i = 0;
            try {
                i = supportedGattServices.get(groupPosition)
                        .getCharacteristics().size();

            } catch (Exception e) {
            }

            return i;
        }

        public View getChildView(final int groupPosition,
                                 final int childPosition, boolean isLastChild, View convertView,
                                 ViewGroup parent) {
            View view = LayoutInflater.from(DeviceDetailActivity.this).inflate(
                    R.layout.item_expend_list_child, null);
            TextView tv_expend_char_name = (TextView) view
                    .findViewById(R.id.tv_expend_char_name);
            TextView tv_expend_char_uuid = (TextView) view
                    .findViewById(R.id.tv_expend_char_uuid);
            TextView tv_expend_char_properties = (TextView) view
                    .findViewById(R.id.tv_expend_char_properties);
            BluetoothGattCharacteristic bluetoothGattCharacteristic = supportedGattServices
                    .get(groupPosition).getCharacteristics().get(childPosition);
            final String uuid = bluetoothGattCharacteristic.getUuid()
                    .toString().toUpperCase();
            if (!TextUtils.isEmpty(uuid))
                tv_expend_char_uuid.setText(uuid);
            String name_c = BleNamesResolver.resolveCharacteristicName(uuid.toUpperCase());
            if (TextUtils.isEmpty(name_c)) {
                tv_expend_char_name.setText("Unknown Characteristic");
            } else {
                tv_expend_char_name.setText(name_c);
            }
            final int properties = bluetoothGattCharacteristic.getProperties();
            String propertiesString = "";
            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                propertiesString = propertiesString + "read ";
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                propertiesString = propertiesString + "write ";
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                propertiesString = propertiesString + "notify ";
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                propertiesString = propertiesString + "indicate ";
            }
            if ((properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0) {
                propertiesString = propertiesString + "write_no_response ";
            }
            tv_expend_char_properties.setText(propertiesString);
            view.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    BluetoothGattService bluetoothGattService = supportedGattServices
                            .get(groupPosition);
                    String serviceUUID = bluetoothGattService.getUuid()
                            .toString();
                    BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService
                            .getCharacteristics().get(childPosition);
                    String characteristicUUID = bluetoothGattCharacteristic
                            .getUuid().toString();
                    Intent intent = new Intent(DeviceDetailActivity.this,
                            OperateActivity.class);
                    intent.putExtra("serviceUUID", serviceUUID);
                    intent.putExtra("characteristicUUID", characteristicUUID);
                    intent.putExtra("properties", properties);
                    startActivity(intent);
                }
            });
            return view;
        }

        public Object getGroup(int groupPosition) {
            return supportedGattServices.get(groupPosition);
        }

        public int getGroupCount() {
            return supportedGattServices.size();
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            expandableListView.expandGroup(groupPosition);
            View view = LayoutInflater.from(DeviceDetailActivity.this).inflate(
                    R.layout.item_expend_list_group, null);
            TextView tv_name = (TextView) view
                    .findViewById(R.id.tv_item_expend_service_name);
            TextView tv_uuid = (TextView) view
                    .findViewById(R.id.tv_item_expend_service_uuid);
            BluetoothGattService bluetoothGattService = supportedGattServices
                    .get(groupPosition);
            tv_uuid.setText(bluetoothGattService.getUuid().toString()
                    .toUpperCase());
            String name = BleNamesResolver
                    .resolveServiceName(bluetoothGattService.getUuid()
                            .toString().toUpperCase());
            if (TextUtils.isEmpty(name)) {
                tv_name.setText("Unknown Service");
            } else {
                tv_name.setText(name);
            }
            return view;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case tv_show:
                if (tvShow.getText().toString().equals("show")) {
                    tvShow.setText("hide");
                    tvScanRecord.setVisibility(View.VISIBLE);
                } else {
                    tvShow.setText("show");
                    tvScanRecord.setVisibility(View.GONE);
                }
                break;
            case tv_conn:
                if (tvConn.getText().toString().equals("disConnect")
                        && tvDeviceConnect.getText().toString()
                        .equals("Connected")) {
                    tvDeviceConnect.setText("disConnecting");
                    manager.disconnect();
                } else if (tvConn.getText().toString().equals("Connect")
                        && tvDeviceConnect.getText().toString()
                        .equals("disConnected")) {
                    tvDeviceConnect.setText("Connecting");
                    manager.connectDevice(scanResult.getDevice().getAddress());
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (tvDeviceConnect.getText().toString().equals("Connected"))
            manager.disconnect();
        unregisterReceiver(aprilBeaconReceiver);
        super.onDestroy();
    }
}
