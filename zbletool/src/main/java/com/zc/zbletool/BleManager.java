package com.zc.zbletool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.zc.zbletool.conn.BleConnection;
import com.zc.zbletool.service.UartService;
import com.zc.zbletool.utils.BleL;
import com.zc.zbletool.utils.UUID2bytesUtils;
import com.zc.zbletool.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;

public class BleManager {

    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private MyScanCallBack scanCallBack;
    private ArrayList<ScanResult> results;
    private ArrayList<Beacon> beacons;
    private long scanPeriod = 1000;
    private long beforeTime;
    private boolean isStartScan;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            ScanResult result = new ScanResult();
            Beacon beacon = Utils.beaconFromLeScan(device, rssi, scanRecord);
            result.setDevice(device);
            result.setRssi(rssi);
            result.setScanRecord(scanRecord);
            result.setHexScanRecord(UUID2bytesUtils.hex(scanRecord));
            long nowTime = System.currentTimeMillis();
            if (nowTime - beforeTime > scanPeriod) {
                beforeTime = nowTime;
                ArrayList<ScanResult> myResults = new ArrayList<>();
                myResults.addAll(results);
                scanCallBack.onScanCallBack(myResults);
                results.clear();
                ArrayList<Beacon> myBeacons = new ArrayList<>();
                myBeacons.addAll(beacons);
                scanCallBack.onScanBeaconsCallBack(myBeacons);
                beacons.clear();

            } else {
                if (!results.contains(result)) {
                    results.add(result);
                } else {
                    results.remove(result);
                    results.add(result);
                }
                if (beacon != null && !beacons.contains(beacon)) {
                    beacons.add(beacon);
                } else if (beacon != null) {
                    beacons.remove(beacon);
                    beacons.add(beacon);
                }
            }
            scanCallBack.onScanCallBack(result);
            scanCallBack.onScanCallBack(device, rssi, scanRecord);
            if (beacon != null) {
                scanCallBack.onScanBeaconCallBack(beacon);
            }
        }
    };

    public BleManager(Context context) {
        this.context = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) context
                .getApplicationContext().getSystemService(
                        Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    /**
     * 检查权限及服务是否配置进清单文件中
     *
     * @return true为已经配置
     */
    public boolean checkPermissionsAndService() {
        PackageManager pm = this.context.getPackageManager();
        int bluetoothPermission = pm.checkPermission(
                "android.permission.BLUETOOTH", this.context.getPackageName());
        int bluetoothAdminPermission = pm.checkPermission(
                "android.permission.BLUETOOTH_ADMIN",
                this.context.getPackageName());

        Intent intent = new Intent(this.context, UartService.class);
        List<?> resolveInfo = pm.queryIntentServices(intent, MATCH_DEFAULT_ONLY);

        return (bluetoothPermission == 0) && (bluetoothAdminPermission == 0)
                && (resolveInfo.size() > 0);
    }

    /**
     * 检测是否支持蓝牙4.0BLE
     *
     * @return true为设备支持BLE
     */
    public boolean hasBluetooth() {
        return this.context.getPackageManager().hasSystemFeature(
                "android.hardware.bluetooth_le");
    }

    /**
     * 开启蓝牙Ble扫描
     *
     * @return true为成功开启扫描
     */
    public boolean startBleScan(MyScanCallBack scanCallBack) {
        results = new ArrayList<>();
        beacons = new ArrayList<>();
        if (mBluetoothAdapter == null) {
            BleL.e("mBluetoothAdapter == null");
            return false;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            BleL.e("mBluetoothAdapter is not enable");
            return false;
        }
        isStartScan = true;
        this.scanCallBack = scanCallBack;
        return mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    /**
     * 停止蓝牙Ble扫描
     */
    public void stopBleScan() {
        isStartScan = false;
        if (results != null) {
            results.clear();
        }
        if (mBluetoothAdapter == null) {
            BleL.e("mBluetoothAdapter == null");
            return;
        }
        if (mLeScanCallback == null) {
            BleL.e("mLeScanCallback == null");
            return;
        }
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    /**
     * 设置扫描间隔时间 返回扫描结果list的结果
     *
     * @param scanPeriod 扫描间隔 单位ms
     */
    public void setScanPeriod(long scanPeriod) {
        this.scanPeriod = scanPeriod;
    }

    /**
     * 扫描开启的回调
     *
     * @author think_admin
     */
    public static abstract interface ScanCallBack {
        /**
         * 扫描到数据时的回调
         *
         * @param scanRecord 扫描到的设备的广播数据
         * @param rssi       扫描到的设备的rssi
         * @param device     扫描到的设备
         */
        public abstract void onScanCallBack(BluetoothDevice device, int rssi,
                                            byte[] scanRecord);

        /**
         * 扫描到数据时的回调
         *
         * @param result 扫描数据结果
         */
        public abstract void onScanCallBack(ScanResult result);

        /**
         * scanPeriod内扫描到数据时的回调
         *
         * @param results scanPeriod内扫描到的设备的集合
         */
        public abstract void onScanCallBack(ArrayList<ScanResult> results);

        /**
         * 扫描到Beacon的回调
         *
         * @param beacon 返回扫描到的beacon
         */
        public abstract void onScanBeaconCallBack(Beacon beacon);

        /**
         * scanPeriod内扫描到Beacon的回调
         *
         * @param beacons 返回scanPeriod内扫描到beacon的集合
         */
        public abstract void onScanBeaconsCallBack(ArrayList<Beacon> beacons);

    }

    public static class MyScanCallBack implements ScanCallBack {

        public void onScanCallBack(BluetoothDevice device, int rssi,
                                   byte[] scanRecord) {

        }

        ;

        public void onScanCallBack(ArrayList<ScanResult> results) {

        }

        public void onScanCallBack(ScanResult result) {

        }

        public void onScanBeaconCallBack(Beacon beacon) {

        }

        public void onScanBeaconsCallBack(ArrayList<Beacon> beacons) {

        }
    }

    ;

    /**
     * 连接设备
     *
     * @param address 设备的mac地址
     */
    public void connectDevice(String address) {
        BleConnection.connDevice(context, address);
    }

    /**
     * 断开与设备的连接
     */
    public void disconnect() {
        BleConnection.disConnect();
    }

    /**
     * 写入特征值 （暂时支持对单个特征值写入，如果要连续写入多个可能会出现问题）
     *
     * @param serviceUUID        服务UUID
     * @param characteristicUUID 特征值UUID
     * @param value              写入的值
     */
    public void wirteValue(UUID serviceUUID, UUID characteristicUUID,
                           byte[] value) {
        BleConnection.writeValue(serviceUUID, characteristicUUID, value);
    }

    /**
     * 读取特征值（暂时支持读取单个特征值，如果要连续读取多个特征值可能会出现问题）
     *
     * @param serviceUUID        服务UUID
     * @param characteristicUUID 特征值UUID
     */
    public void readCharacteristic(UUID serviceUUID, UUID characteristicUUID) {
        BleConnection.readCharacteristic(serviceUUID, characteristicUUID);
    }

    public void enableTXNotification(UUID serviceUUID, UUID characteristicUUID,
                                     UUID descriptorUUID) {
        BleConnection.enableTXNotification(serviceUUID, characteristicUUID,
                descriptorUUID);
    }

    /**
     * 读取设备Rssi
     *
     * @return
     */
    public boolean readRemoteRssi() {
        return BleConnection.readRemoteRssi();
    }

    /**
     * 获取设备服务列表
     *
     * @return
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        return BleConnection.getSupportedGattServices();
    }

    /**
     * 获取服务特征值列表
     *
     * @param serviceUUID
     * @return
     */
    public List<BluetoothGattCharacteristic> getSupportedGattCharacteristics(
            UUID serviceUUID) {
        return BleConnection.getSupportedGattCharacteristics(serviceUUID);
    }

    /**
     * 根据服务UUID及特征值UUID获取特征值
     *
     * @param serviceUUID
     * @param CharacteristicUUID
     * @return
     */
    public BluetoothGattCharacteristic getCharacteristic(UUID serviceUUID,
                                                         UUID CharacteristicUUID) {
        return BleConnection.getCharacteristic(serviceUUID, CharacteristicUUID);
    }

    public boolean isStartScan() {
        return isStartScan;
    }
}
