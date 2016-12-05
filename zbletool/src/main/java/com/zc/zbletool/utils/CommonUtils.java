package com.zc.zbletool.utils;

import java.util.List;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class CommonUtils {
	/**
	 * 判断服务是否后台运行
	 * 
	 * @param context
	 *            Context
	 * @param className
	 *            判断的服务名字
	 * @return true 在运行 false 不在运行
	 */
	public static boolean isServiceRun(Context mContext, String className) {
		boolean isRun = false;
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager
				.getRunningServices(200);
		int size = serviceList.size();
		for (int i = 0; i < size; i++) {
			BleL.d(serviceList.get(i).service.getClassName());
			if (serviceList.get(i).service.getClassName().equals(className) == true) {
				isRun = true;
				break;
			}
		}
		return isRun;
	}
}
