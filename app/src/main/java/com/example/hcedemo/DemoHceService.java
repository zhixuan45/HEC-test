package com.example.hcedemo;

import android.nfc.cardemulation.HostApduService;
import android.content.Intent;
import android.os.Bundle;
import java.util.Arrays;

public class DemoHceService extends HostApduService {
    private static final byte[] AID = hex("F0010203040506");
    private static final byte[] SUCCESS = hex("9000");
    private static final byte[] NOT_SUPPORTED = hex("6D00");
    private static final byte[] TEST_COMMAND = hex("80CA000000");

    @Override public byte[] processCommandApdu(byte[] command, Bundle extras) {
        publish("收到 APDU: " + toHex(command));
        if (command != null && command.length >= 5
                && (command[0] & 0xFF) == 0x00
                && (command[1] & 0xFF) == 0xA4
                && (command[2] & 0xFF) == 0x04
                && (command[3] & 0xFF) == 0x00
                && (command[4] & 0xFF) == AID.length
                && command.length >= 5 + AID.length
                && Arrays.equals(Arrays.copyOfRange(command, 5, 5 + AID.length), AID)) {
            publish("测试 AID 匹配，返回 9000");
            return SUCCESS;
        }
        if (Arrays.equals(command, TEST_COMMAND)) {
            String response = getSharedPreferences("hce_test", MODE_PRIVATE).getString("response", "48656C6C6F9000");
            publish("测试命令匹配，返回: " + response);
            return hex(response);
        }
        publish("非测试指令，返回 6D00");
        return NOT_SUPPORTED;
    }

    @Override public void onDeactivated(int reason) {
        publish("连接结束，原因代码: " + reason);
    }

    private void publish(String line) {
        Intent i = new Intent("com.example.hcedemo.LOG");
        i.setPackage(getPackageName());
        i.putExtra("line", line);
        sendBroadcast(i);
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        return out;
    }
    private static String toHex(byte[] b) {
        if (b == null) return "null";
        StringBuilder s = new StringBuilder();
        for (byte v : b) s.append(String.format(java.util.Locale.ROOT, "%02X", v & 0xFF));
        return s.toString();
    }
}
