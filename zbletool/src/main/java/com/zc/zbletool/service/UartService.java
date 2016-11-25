package com.zc.zbletool.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.zc.zbletool.Contants;
import com.zc.zbletool.utils.BleL;

import java.util.List;
import java.util.UUID;

public class UartService extends Service {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            String intentAction;
            BleL.i("newState = " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = Contants.BLESDK_ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                BleL.i("Attempting to start service discovery:"
                        + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = Contants.BLESDK_ACTION_GATT_DISCONNECTED;
                BleL.i("Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleL.w("mBluetoothGatt = " + mBluetoothGatt);
                broadcastUpdate(Contants.BLESDK_ACTION_GATT_SERVICES_DISCOVERED_NEW);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(Contants.BLESDK_ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(Contants.BLESDK_ACTION_DATA_CHANGE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            broadcastUpdate(Contants.BLESDK_ACTION_DATA_WRITE, characteristic);
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            broadcastUpdate(Contants.BLESDK_ACTION_READ_REMOTE_RSSI, rssi);
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            broadcastUpdate(Contants.BLESDK_ACTION_DESCRIPTOR_WRITE);
            super.onDescriptorWrite(gatt, descriptor, status);
        }


    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 BluetoothGattCharacteristic characteristic) {
        Intent intent = new Intent(action);
        intent.putExtra(Contants.BLESDK_BYTES_DATA, characteristic.getValue());
        String uuid = characteristic.getUuid().toString();
        intent.putExtra(Contants.BLESDK_CHARACTERISTICUUID, uuid);
        Integer intValue = characteristic.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        int mIntValue = intValue;
        intent.putExtra(Contants.BLESDK_INT_DATA, mIntValue);
        try {
            byte[] value = characteristic.getValue();
            String byte2HexStr = byte2HexStr(value);
            intent.putExtra(Contants.BLESDK_STRING_DATA, byte2HexStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendBroadcast(intent);
    }

    /**
     * bytes转换成十六进制字符串
     */
    private String byte2HexStr(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1)
                hs = hs + "0" + stmp;
            else
                hs = hs + stmp;
        }
        return hs.toUpperCase();
    }

    private void broadcastUpdate(String action, int rssi) {
        Intent intent = new Intent(action);
        intent.putExtra(Contants.BLESDK_INT_DATA, rssi);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public UartService getService() {
            return UartService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                BleL.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            BleL.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        BleL.i(address);
        if (mBluetoothAdapter == null || address == null) {
            BleL.w("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            BleL.d("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            BleL.w("Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        BleL.d("Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            BleL.w("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        // mBluetoothGatt.close();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        BleL.w("mBluetoothGatt closed");
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            BleL.w("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    public void readCharacteristic(UUID serviceUUID,
                                   UUID CharacteristicUUID) {
        BluetoothGattService RxService = mBluetoothGatt.getService(serviceUUID);
        showMessage("mBluetoothGatt null" + mBluetoothGatt);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService
                .getCharacteristic(CharacteristicUUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.readCharacteristic(RxChar);
    }

    public void enableTXNotification(UUID rx_service_uuid, UUID tx_char_uuid,
                                     UUID descriptorUUID) {
        BluetoothGattService RxService = mBluetoothGatt
                .getService(rx_service_uuid);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService
                .getCharacteristic(tx_char_uuid);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar, true);
        BluetoothGattDescriptor descriptor = TxChar
                .getDescriptor(descriptorUUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    public void writeRXCharacteristic(UUID serviceUUID,
                                      UUID CharacteristicUUID, byte[] value) {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            showMessage("writeRXCharacteristic InterruptedException");
            e.printStackTrace();
        }
        BluetoothGattService RxService = mBluetoothGatt.getService(serviceUUID);
        showMessage("mBluetoothGatt null" + mBluetoothGatt);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService
                .getCharacteristic(CharacteristicUUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(Contants.BLESDK_DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        RxChar.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);
        BleL.d("write TXchar - status=" + status);
    }

    private void showMessage(String msg) {
        BleL.e(msg);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public List<BluetoothGattCharacteristic> getSupportedGattCharacteristics(
            UUID serviceUUID) {
        if (mBluetoothGatt == null)
            return null;

        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        return service.getCharacteristics();
    }

    public BluetoothGattCharacteristic getCharacteristic(UUID serviceUUID,
                                                         UUID CharacteristicUUID) {
        if (mBluetoothGatt == null)
            return null;

        BluetoothGattService service = mBluetoothGatt.getService(serviceUUID);
        BluetoothGattCharacteristic c = service
                .getCharacteristic(CharacteristicUUID);
        return service.getCharacteristic(CharacteristicUUID);
    }

    public boolean readRemoteRssi() {
        if (mBluetoothGatt == null)
            return false;

        return mBluetoothGatt.readRemoteRssi();
    }
}
