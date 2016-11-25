package com.zc.zbletool.utils;

import java.util.UUID;
/** {@hide} */
public class UUID2bytesUtils {
	
	private final static char[] HEX = "0123456789abcdef".toCharArray();
	
	 public static void main(String[] args) {
	        String uuid = "2c4dbc35-7985-42b1-a391-019e3acfea05";
	        byte[] bys = uuid2Bytes(uuid);
	        // 验证
	        System.out.println(hex(bys).equals(uuid.replace("-", "")));
	    }
	
	public static byte[] uuid2Bytes(String uuid) {
        if(uuid == null) {
            throw new NullPointerException("uuid is null");
        }
        UUID uid = UUID.fromString(uuid);
        long lsb = uid.getLeastSignificantBits();
        long msg = uid.getMostSignificantBits();
        byte[] bys = new byte[16];
        for(int i = 0, j = 8; i < 8; i++, j++) {
            bys[i] = (byte)((msg >>> ((7 - i) << 3)) & 0xff);
            bys[j] = (byte)((lsb >>> ((7 - i) << 3)) & 0xff);
        }
        return bys;
    }
 
    public static String hex(byte[] bytes) {
        if(bytes == null) {
            return null;
        }
        if(bytes.length == 0) {
            return "";
        }
        char[] chs = new char[bytes.length << 1];
        for(int i = 0, k = 0; i < bytes.length; i++) {
            chs[k++] = HEX[(bytes[i] & 0xf0) >> 4];
            chs[k++] = HEX[(bytes[i] & 0xf)];
        }
        return new String(chs);
    }
}
