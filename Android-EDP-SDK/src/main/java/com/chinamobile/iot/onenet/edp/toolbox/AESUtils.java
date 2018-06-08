/*
 * Copyright (c) 2016-2018. China Mobile Communications Corporation. All rights reserved.
 */

package com.chinamobile.iot.onenet.edp.toolbox;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES加密工具类
 * <p>
 *
 * Created by chenglei on 2015/12/31.
 */
public class AESUtils {

    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2),
                    16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    public static byte[] encrypt(byte[] plainText, byte[] keyBytes) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(padding(plainText));
    }
    public static byte[] decrypt(byte[] cipherText, byte[] keyBytes) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return removePadding(cipher.doFinal(cipherText));
    }

    private static byte[] padding(byte[] text) {
        if (text != null) {
            int size = text.length;
            int mod = size % 16;
            int paddingSize = 16 - mod;
            ByteBuffer buffer = ByteBuffer.allocate(size + paddingSize);
            buffer.put(text);
            for (int i = 0; i < paddingSize; i++) {
                buffer.put((byte) ('0' + paddingSize));
            }
            text = new byte[buffer.position()];
            buffer.flip();
            buffer.get(text);
        }
        return text;
    }

    private static byte[] removePadding(byte[] text) {
        if (text != null) {
            int size = text.length;
            int paddingSize = text[size - 1] - '0';
            byte[] buffer = new byte[size - paddingSize];
            System.arraycopy(text, 0, buffer, 0, buffer.length);
            text = buffer;
        }
        return text;
    }

}
