package com.example.hcedemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.content.ComponentName;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.provider.Settings;
import android.os.Build;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Typeface;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView log;
    private NfcAdapter adapter;
    private boolean reading = false;
    private TextView routeStatus;
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
        routeStatus = new TextView(this);
        layout.addView(routeStatus);
        Button refresh = new Button(this);
        refresh.setText("刷新 NFC / HCE 路由诊断");
        layout.addView(refresh);
        refresh.setOnClickListener(v -> refreshRoute());
        Button settings = new Button(this);
        settings.setText("打开系统 NFC 设置");
        layout.addView(settings);
        settings.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_NFC_SETTINGS)); }
            catch (Exception e) { append("系统不支持直接打开 NFC 设置"); }
        });
        Button toggle = new Button(this);
        toggle.setText("启用实体卡检测");
        layout.addView(toggle);
        toggle.setOnClickListener(v -> {
            reading = !reading;
            toggle.setText(reading ? "停止实体卡检测" : "启用实体卡检测");
            updateReader();
            append(reading ? "读卡模式开启：请贴近实体卡" : "读卡模式关闭");
            refreshRoute();
        });
        TextView hint = new TextView(this);
        hint.setText("自定义 ISO-DEP 测试响应（HEX，含状态字）。先 SELECT 测试 AID，再发送 80CA000000。Classic 模拟不可用。");
        layout.addView(hint);
        EditText responseInput = new EditText(this);
        responseInput.setSingleLine(true);
        responseInput.setText(getSharedPreferences("hce_test", MODE_PRIVATE).getString("response", "48656C6C6F9000"));
        layout.addView(responseInput);
        Button save = new Button(this);
        save.setText("保存测试响应");
        layout.addView(save);
        Button report = new Button(this);
        report.setText("复制诊断报告");
        layout.addView(report);
        report.setOnClickListener(v -> {
            String data = "HEC-test 诊断报告\n" + routeStatus.getText()
                + "\n已验证：实体卡曾报告 IsoDep + MifareClassic 接口。"
                + "\n未验证：学校终端实际使用 Classic 还是 ISO-DEP。"
                + "\n注意：无 APDU 日志不能证明终端使用 Classic。\n"
                + DiagnosticLog.history(this);
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("HCE diagnostic", data));
            append("报告已复制，可粘贴发送（可能包含卡片 UID，请注意隐私）");
        });
        Button clear = new Button(this);
        clear.setText("清空诊断日志");
        layout.addView(clear);
        clear.setOnClickListener(v -> {
            DiagnosticLog.clear(this);
            log.setText("日志已清空");
        });
        save.setOnClickListener(v -> {
            String value = responseInput.getText().toString().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            if (value.length() < 4 || value.length() % 2 != 0 || !value.matches("[0-9A-F]+") || value.length() > 512) {
                append("保存失败：输入偶数位 HEX，至少包含两字节状态字，最多 256 字节");
                return;
            }
            getSharedPreferences("hce_test", MODE_PRIVATE).edit().putString("response", value).apply();
            append("已保存自定义测试响应: " + value);
        });
        log = new TextView(this);
        log.setText("等待操作…");
        log.setTextIsSelectable(true);
        log.setMinHeight((int)(300 * getResources().getDisplayMetrics().density));
        layout.addView(log);
        ScrollView outerScroll = new ScrollView(this);
        outerScroll.addView(layout);
        setContentView(outerScroll);
        log.setText(DiagnosticLog.history(this));
        refreshRoute();
    }
    private void append(String line) {
        runOnUiThread(() -> log.append("\n" + line));
    }
    private void refreshRoute() {
        StringBuilder b = new StringBuilder("设备路由状态（仅公开 API 可见）");
        boolean enabled = adapter != null && adapter.isEnabled();
        b.append("\nNFC 开启：").append(enabled);
        b.append("\nHCE 支持：").append(getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION));
        b.append("\n读卡模式：").append(reading ? "已开启（此时不能同时作 HCE）" : "已关闭");
        if (enabled && Build.VERSION.SDK_INT >= 19) {
            try {
                CardEmulation em = CardEmulation.getInstance(adapter);
                ComponentName component = new ComponentName(this, DemoHceService.class);
                b.append("\n测试 AID 静态配置：F0010203040506（apduservice.xml）");
                b.append("\n动态注册 AID：").append(em.getAidsForService(component, CardEmulation.CATEGORY_OTHER));
                b.append("（null 可为正常：该 API 不返回静态注册项）");
                b.append("\nother 分类默认服务（false 不一定异常）：").append(em.isDefaultServiceForCategory(component, CardEmulation.CATEGORY_OTHER));
                b.append("\n测试 AID 的默认处理服务：").append(em.isDefaultServiceForAid(component, "F0010203040506"));
            } catch (Exception e) {
                b.append("\nHCE 路由查询失败：").append(e.getClass().getSimpleName());
            }
        }
        b.append("\n系统钱包私有路由及其他服务 APDU：普通应用无法查看");
        routeStatus.setText(b.toString());
        DiagnosticLog.add(this, "路由状态刷新：" + b.toString().replace("\n", " | "));
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
        DiagnosticLog.add(this, b.toString());
    }
    private static String hex(byte[] data) {
        if (data == null) return "无";
        StringBuilder b = new StringBuilder();
        for (byte v : data) b.append(String.format(Locale.ROOT,"%02X",v & 0xFF));
        return b.toString();
    }
    @Override public void onResume() { super.onResume(); if (reading) updateReader(); if (routeStatus != null) refreshRoute(); }
    @Override public void onPause() { if (reading && adapter != null) adapter.disableReaderMode(this); super.onPause(); }
    @Override public void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter("com.example.hcedemo.LOG");
        registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);
    }
    @Override public void onStop() { unregisterReceiver(receiver); super.onStop(); }
}
