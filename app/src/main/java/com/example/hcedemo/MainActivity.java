package com.example.hcedemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Typeface;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView log;
    private NfcAdapter adapter;
    private boolean reading = false;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String line = intent.getStringExtra("line");
            if (line != null) append(line);
        }
    };
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = NfcAdapter.getDefaultAdapter(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad,pad,pad,pad);
        TextView title = new TextView(this);
        title.setText("NFC 协议诊断 / HCE");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        layout.addView(title);
        boolean hce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        TextView status = new TextView(this);
        status.setText("HCE: " + hce + " | NFC: " + (adapter != null && adapter.isEnabled())
          + "\n测试 AID: F0010203040506\n读卡模式仅检测技术参数，不读取余额或认证密钥。");
        layout.addView(status);
        Button toggle = new Button(this);
        toggle.setText("启用实体卡检测");
        layout.addView(toggle);
        toggle.setOnClickListener(v -> {
            reading = !reading;
            toggle.setText(reading ? "停止实体卡检测" : "启用实体卡检测");
            updateReader();
            append(reading ? "读卡模式开启：请贴近实体卡" : "读卡模式关闭");
        });
        ScrollView scroll = new ScrollView(this);
        log = new TextView(this);
        log.setText("等待操作…");
        log.setTextIsSelectable(true);
        scroll.addView(log);
        layout.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        setContentView(layout);
    }
    private void append(String line) {
        runOnUiThread(() -> log.append("\n" + line));
    }
    private void updateReader() {
        if (adapter == null) { append("未检测到 NFC 适配器"); return; }
        if (reading) {
            adapter.enableReaderMode(this, this::onTag, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
        } else {
            adapter.disableReaderMode(this);
        }
    }
    private void onTag(Tag tag) {
        StringBuilder b = new StringBuilder("检测到实体卡\nUID: " + hex(tag.getId()));
        for (String tech : tag.getTechList()) b.append("\n技术: ").append(tech);
        NfcA a = NfcA.get(tag);
        if (a != null) {
            b.append("\nATQA: ").append(hex(a.getAtqa()));
            b.append("\nSAK: ").append(String.format(Locale.ROOT,"0x%02X",a.getSak()));
        }
        IsoDep iso = IsoDep.get(tag);
        if (iso != null) {
            b.append("\nISO-DEP 可用: 是");
            try {
                iso.connect();
                byte[] historical = iso.getHistoricalBytes();
                byte[] hi = iso.getHiLayerResponse();
                if (historical != null) b.append("\nISO-DEP 历史字节: ").append(hex(historical));
                if (hi != null) b.append("\nISO-DEP 高层响应: ").append(hex(hi));
            } catch (Exception e) { b.append("\nISO-DEP 信息获取失败: ").append(e.getClass().getSimpleName()); }
            finally { try { iso.close(); } catch (Exception ignored) {} }
        }
        MifareClassic mc = MifareClassic.get(tag);
        b.append("\nMIFARE Classic 接口: ").append(mc != null ? "可用" : "不可用");
        if (mc != null) b.append("\n扇区数: ").append(mc.getSectorCount()).append(" / 容量: ").append(mc.getSize());
        Ndef ndef = Ndef.get(tag);
        b.append("\nNDEF: ").append(ndef != null ? "支持" : "未发现");
        b.append("\n说明: 以上为本机报告的协议接口，不代表支持卡片模拟。");
        append(b.toString());
    }
    private static String hex(byte[] data) {
        if (data == null) return "无";
        StringBuilder b = new StringBuilder();
        for (byte v : data) b.append(String.format(Locale.ROOT,"%02X",v & 0xFF));
        return b.toString();
    }
    @Override public void onResume() { super.onResume(); if (reading) updateReader(); }
    @Override public void onPause() { if (reading && adapter != null) adapter.disableReaderMode(this); super.onPause(); }
    @Override public void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter("com.example.hcedemo.LOG");
        registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);
    }
    @Override public void onStop() { unregisterReceiver(receiver); super.onStop(); }
}
