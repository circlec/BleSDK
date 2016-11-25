package com.zc.zbletool.conn;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.zc.zbletool.service.UartService;
import com.zc.zbletool.utils.CommonUtils;

import java.util.List;
import java.util.UUID;


public class BleConnection {
	protected static final String TAG = BleConnection.class.getSimpleName();
	private static UartService mAprilBeaconService = null;
	private static String address;
	private static Context context;

	private static ServiceConnection mAprilBeaconServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className,
				IBinder rawBinder) {

			mAprilBeaconService = ((UartService.LocalBinder) rawBinder)
					.getService();

			Log.d(TAG, "onServiceConnected mService= " + mAprilBeaconService);
			if (!mAprilBeaconService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
			}
			mAprilBeaconService.connect(address);
		}

		public void onServiceDisconnected(ComponentName classname) {
			mAprilBeaconService = null;
		}
	};

	/**
	 * 连接设备
	 * 
	 * @param myContext
	 *            上下文
	 * @param myAddress
	 *            设备的mac地址
	 */
	public static void connDevice(Context myContext, String myAddress) {
		context = myContext;
		address = myAddress;
		Intent bindIntent = new Intent(context, UartService.class);
		context.bindService(bindIntent, mAprilBeaconServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	/**
	 * 断开连接
	 */
	public static void disConnect() {
		if (CommonUtils.isServiceRun(context,
				"com.zc.blesdk.service.UartService")) {
			context.unbindService(mAprilBeaconServiceConnection);
		}
		if (mAprilBeaconService != null) {
			mAprilBeaconService.disconnect();
			mAprilBeaconService.close();
			mAprilBeaconService.stopSelf();
			mAprilBeaconService = null;
		}
	}

	public static void writeValue(UUID serviceUUID, UUID characteristicUUID,
			byte[] value, String className) {
		if (className.equals(UartService.class.getSimpleName())) {
			if (mAprilBeaconService != null)
				mAprilBeaconService.writeRXCharacteristic(serviceUUID,
						characteristicUUID, value);
		}
	}

	/**
	 * 写入特征值 （暂时支持对单个特征值写入，如果要连续写入多个可能会出现问题） 要写入多个的话需在广播里自己定义操作
	 * 
	 * @param serviceUUID
	 *            服务UUID
	 * @param characteristicUUID
	 *            特征值UUID
	 * @param value
	 *            写入的值
	 */
	public static void writeValue(UUID serviceUUID, UUID characteristicUUID,
			byte[] value) {
		if (mAprilBeaconService != null) {
			mAprilBeaconService.writeRXCharacteristic(serviceUUID,
					characteristicUUID, value);
		}
	}

	/**
	 * 读取特征值（暂时支持读取单个特征值，如果要连续读取多个特征值可能会出现问题）
	 * 
	 * @param serviceUUID
	 *            服务UUID
	 * @param characteristicUUID
	 *            特征值UUID
	 */
	public static void readCharacteristic(UUID serviceUUID,
			UUID characteristicUUID) {
		if (mAprilBeaconService != null) {
			mAprilBeaconService.readCharacteristic(serviceUUID,
					characteristicUUID);
		}
	}

	/**
	 * 设置特征值通知开启
	 * 
	 * @param serviceUUID
	 *            服务UUID
	 * @param characteristicUUID
	 *            特征值UUID
	 * @param descriptorUUID
	 *            描述符UUID
	 */
	public static void enableTXNotification(UUID serviceUUID,
			UUID characteristicUUID, UUID descriptorUUID) {
		if (mAprilBeaconService != null) {
			mAprilBeaconService.enableTXNotification(serviceUUID,
					characteristicUUID, descriptorUUID);
		}
	}

	/**
	 * 获取设备服务列表
	 * 
	 * @return
	 */
	public static List<BluetoothGattService> getSupportedGattServices() {
		if (mAprilBeaconService != null) {
			return mAprilBeaconService.getSupportedGattServices();
		}
		return null;
	}

	/**
	 * 获取服务对应特征值列表
	 * 
	 * @param serviceUUID
	 *            服务UUID
	 * @return
	 */
	public static List<BluetoothGattCharacteristic> getSupportedGattCharacteristics(
			UUID serviceUUID) {
		if (mAprilBeaconService != null) {
			return mAprilBeaconService
					.getSupportedGattCharacteristics(serviceUUID);
		}
		return null;
	}

	/**
	 * 获取特征值
	 * 
	 * @param serviceUUID
	 *            服务UUID
	 * @param CharacteristicUUID
	 *            特征值UUID
	 * @return
	 */
	public static BluetoothGattCharacteristic getCharacteristic(
			UUID serviceUUID, UUID CharacteristicUUID) {
		if (mAprilBeaconService != null) {
			return mAprilBeaconService.getCharacteristic(serviceUUID,
					CharacteristicUUID);
		}
		return null;
	}

	public static boolean readRemoteRssi() {
		if (mAprilBeaconService != null) {
			return mAprilBeaconService.readRemoteRssi();
		}
		return false;
	}

}