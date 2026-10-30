package com.example.hcedemo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DiagnosticLog {
    private DiagnosticLog() {}
    public static synchronized void add(Context context, String event) {
        SharedPreferences p = context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE);
        String stamp = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.ROOT).format(new Date());
        String line = stamp + "  " + event;
        String old = p.getString("history", "");
        String value = old + line + "\n";
        if (value.length() > 20000) value = value.substring(value.length() - 20000);
        p.edit().putString("history", value).commit();
        Intent i = new Intent("com.example.hcedemo.LOG");
        i.setPackage(context.getPackageName());
        i.putExtra("line", line);
        context.sendBroadcast(i);
    }
    public static String history(Context c) {
        return c.getSharedPreferences("diagnostics", Context.MODE_PRIVATE).getString("history", "");
    }
    public static void clear(Context c) {
        c.getSharedPreferences("diagnostics", Context.MODE_PRIVATE).edit().remove("history").apply();
    }
}