package com.example.hcedemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Typeface;

public class MainActivity extends Activity {
    private TextView log;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String line = intent.getStringExtra("line");
            if (line != null) log.append("\n" + line);
        }
    };
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this);
        title.setText("ISO-DEP / HCE 协议测试");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        layout.addView(title);
        TextView status = new TextView(this);
        boolean hce = getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION);
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        status.setText("HCE 硬件支持: " + (hce ? "是" : "否") + "\nNFC 状态: " + (adapter == null ? "无 NFC" : adapter.isEnabled() ? "已开启" : "未开启")
            + "\n测试 AID: F0010203040506\n\n本应用只响应独立测试 AID，不模拟校园卡、银行卡或真实支付凭据。\n\n需要另一台支持 ISO-DEP 的 NFC 读卡设备发送 SELECT AID，才能观察下方日志。仅安装 App 不会自动产生通信。\n");
        status.setTextSize(16);
        layout.addView(status);
        ScrollView scroll = new ScrollView(this);
        log = new TextView(this);
        log.setText("等待测试读卡器发送 APDU…");
        log.setTextIsSelectable(true);
        scroll.addView(log);
        layout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(layout);
    }
    @Override public void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter("com.example.hcedemo.LOG");
        registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
    }
    @Override public void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
    }
}
