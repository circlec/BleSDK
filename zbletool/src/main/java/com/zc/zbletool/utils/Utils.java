package com.zc.zbletool.utils;


import android.bluetooth.BluetoothDevice;

import com.zc.zbletool.Beacon;


public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    final private static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    @SuppressWarnings("unused")
    private static final int MANUFACTURER_SPECIFIC_DATA = 255;

    public static Beacon beaconFromLeScan(BluetoothDevice device, int rssi,
                                          byte[] scanRecord) {
        return fromScanData(device, rssi, scanRecord);
    }

    private static Beacon fromScanData(BluetoothDevice device, int rssi,
                                       byte[] scanData) {

        if (((int) scanData[5] & 0xff) == 0x4c
                && ((int) scanData[6] & 0xff) == 0x00
                && ((int) scanData[7] & 0xff) == 0x02
                && ((int) scanData[8] & 0xff) == 0x15) {
            // yes! This is an iBeacon
        } else {
            // This is not an iBeacon
            // Log.d(TAG,
            // "This is not an iBeacon advertisment.  The bytes I see are: "
            // + bytesToHex(scanData));
            return null;
        }

        int major = (scanData[25] & 0xff) * 0x100 + (scanData[26] & 0xff);
        int minor = (scanData[27] & 0xff) * 0x100 + (scanData[28] & 0xff);
        int measuredPower = (int) scanData[29]; // this one is signed
        int power = (int) scanData[31];

        byte[] proximityUuidBytes = new byte[16];
        System.arraycopy(scanData, 9, proximityUuidBytes, 0, 16);
        String hexString = bytesToHex(proximityUuidBytes);
        StringBuilder sb = new StringBuilder();
        sb.append(hexString.substring(0, 8));
        sb.append("-");
        sb.append(hexString.substring(8, 12));
        sb.append("-");
        sb.append(hexString.substring(12, 16));
        sb.append("-");
        sb.append(hexString.substring(16, 20));
        sb.append("-");
        sb.append(hexString.substring(20, 32));
        String proximityUuid = sb.toString();

        return new Beacon(proximityUuid, device.getName(), device.getAddress(),
                major, minor, measuredPower, rssi, power);

    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static double computeAccuracy(Beacon beacon) {
        if (beacon.getRssi() == 0) {
            return -1.0D;
        }
        if (beacon.getMeasuredPower() == 0) {
            return -1.0D;
        }
        double ratio = beacon.getRssi() / beacon.getMeasuredPower();
        double rssiCorrection = 0.96D + Math.pow(Math.abs(beacon.getRssi()),
                3.0D) % 10.0D / 150.0D;

        if (ratio <= 1.0D) {
            return Math.pow(ratio, 9.98D) * rssiCorrection;
        }
        return (0.103D + 0.89978D * Math.pow(ratio, 7.71D)) * rssiCorrection;
    }

    public static enum Proximity {
        UNKNOWN,

        IMMEDIATE,

        NEAR,

        FAR;

        private Proximity() {
        }
    }

    private static Proximity proximityFromAccuracy(double accuracy) {
        if (accuracy < 0.0D) {
            return Proximity.UNKNOWN;
        }
        if (accuracy < 0.5D) {
            return Proximity.IMMEDIATE;
        }
        if (accuracy <= 3.0D) {
            return Proximity.NEAR;
        }
        return Proximity.FAR;
    }

    @SuppressWarnings("unused")
    private static Proximity computeProximity(Beacon beacon) {
        return proximityFromAccuracy(computeAccuracy(beacon));
    }

    public static int parseInt(String numberAsString) {
        try {
            return Integer.parseInt(numberAsString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static byte[] password2byte(String password) {
        byte[] bytes = password.getBytes();
        byte[] a = new byte[password.length() + 2];
        a[0] = 7 & 0xff;
        a[1] = (byte) (password.length() & 0xff);
        for (int i = 0; i < bytes.length; i++) {
            a[i + 2] = bytes[i];
        }
        return a;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}
