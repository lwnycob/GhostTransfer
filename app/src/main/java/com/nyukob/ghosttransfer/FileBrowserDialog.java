package com.nyukob.ghosttransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Built-in file browser dialog.
 * Works without SAF, compatible with touch and D-pad remote.
 * Requires MANAGE_EXTERNAL_STORAGE for full access on Android 11+.
 */
public class FileBrowserDialog {

    public interface FilePickCallback {
        void onFileSelected(File file);
        void onCancelled();
    }

    private final Activity activity;
    private final FilePickCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private AlertDialog dialog;
    private File currentDir;
    private TextView tvPath;
    private ListView listView;
    private FileListAdapter adapter;
    private final List<File> quickRoots = new ArrayList<>();

    public FileBrowserDialog(Activity activity, FilePickCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void show() {
        buildQuickRoots();
        File start = Environment.getExternalStorageDirectory();
        if (start == null || !start.exists()) start = activity.getExternalFilesDir(null);
        if (start == null) start = activity.getFilesDir();
        buildDialog(start);
    }

    private void buildQuickRoots() {
        quickRoots.clear();
        File internal = Environment.getExternalStorageDirectory();
        if (internal != null && internal.exists()) quickRoots.add(internal);
        File[] vols = new File("/storage").listFiles();
        if (vols != null) for (File v : vols) {
            String n = v.getName();
            if (!n.equals("emulated") && !n.equals("self") && v.isDirectory() && v.canRead())
                quickRoots.add(v);
        }
        File root = new File("/");
        if (root.listFiles() != null) quickRoots.add(root);
    }

    private void buildDialog(File startDir) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);

        // Quick navigation bar
        LinearLayout qBar = new LinearLayout(activity);
        qBar.setOrientation(LinearLayout.HORIZONTAL);
        qBar.setPadding(dp(8), dp(4), dp(8), dp(4));
        qBar.setBackgroundColor(0xFFF5F5F5);
        String[] labels = {"📱 Memory", "💾 SD/USB", "/ Root"};
        for (int i = 0; i < quickRoots.size(); i++) {
            final File qDir = quickRoots.get(i);
            TextView tv = new TextView(activity);
            tv.setText(i < labels.length ? labels[i] : qDir.getName());
            tv.setTextSize(11); tv.setTextColor(0xFFFFFFFF);
            tv.setBackgroundColor(0xFF6750A4); tv.setPadding(dp(8), dp(4), dp(8), dp(4));
            tv.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(4), 0); tv.setLayoutParams(lp);
            tv.setOnClickListener(v -> navigateTo(qDir));
            qBar.addView(tv);
        }
        root.addView(qBar);

        // Path display
        tvPath = new TextView(activity);
        tvPath.setTextSize(11); tvPath.setTextColor(0xFF424242);
        tvPath.setTypeface(Typeface.MONOSPACE);
        tvPath.setPadding(dp(10), dp(6), dp(10), dp(6));
        tvPath.setBackgroundColor(0xFFEEEEEE);
        root.addView(tvPath);

        // File list
        listView = new ListView(activity);
        listView.setFocusable(true);
        adapter = new FileListAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, pos, id) -> {
            Object item = adapter.getItem(pos);
            if (item instanceof String) {
                File par = currentDir.getParentFile();
                if (par != null) navigateTo(par);
            } else {
                File f = (File) item;
                if (f.isDirectory()) navigateTo(f); else confirmSelection(f);
            }
        });
        int screenH = activity.getResources().getDisplayMetrics().heightPixels;
        listView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int)(screenH * 0.60)));
        root.addView(listView);

        // Hint
        TextView hint = new TextView(activity);
        hint.setText("Tap file to select, tap folder to enter");
        hint.setTextSize(10); hint.setTextColor(0xFF9E9E9E);
        hint.setPadding(dp(10), dp(4), dp(10), dp(4));
        root.addView(hint);

        dialog = new AlertDialog.Builder(activity)
            .setTitle("📂 Select File")
            .setView(root)
            .setNegativeButton("Cancel", (d, w) -> callback.onCancelled())
            .create();
        dialog.show();
        navigateTo(startDir);
    }

    private void navigateTo(File dir) {
        currentDir = dir;
        tvPath.setText(dir.getAbsolutePath());
        adapter.setLoading(true);
        new Thread(() -> {
            List<Object> items = new ArrayList<>();
            if (dir.getParentFile() != null) items.add("⬆ ..");
            File[] files = dir.listFiles();
            if (files != null) {
                List<File> dirs = new ArrayList<>(), fl = new ArrayList<>();
                for (File f : files) {
                    if (f.getName().startsWith(".")) continue;
                    if (f.isDirectory()) dirs.add(f); else fl.add(f);
                }
                Comparator<File> cmp = (a, b) -> a.getName().compareToIgnoreCase(b.getName());
                Collections.sort(dirs, cmp); Collections.sort(fl, cmp);
                items.addAll(dirs); items.addAll(fl);
            }
            handler.post(() -> { adapter.setItems(items); if (listView.getCount() > 0) listView.setSelection(0); });
        }).start();
    }

    private void confirmSelection(File file) {
        new AlertDialog.Builder(activity)
            .setTitle("Select this file?")
            .setMessage(file.getName() + "\n" + fmt(file.length()))
            .setPositiveButton("✓ Select", (d, w) -> { if (dialog != null) dialog.dismiss(); callback.onFileSelected(file); })
            .setNegativeButton("Back", null).show();
    }

    private static String fmt(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024*1024) return new DecimalFormat("0.0").format(b/1024.0) + " KB";
        if (b < 1024L*1024*1024) return new DecimalFormat("0.0").format(b/(1024.0*1024)) + " MB";
        return new DecimalFormat("0.0").format(b/(1024.0*1024*1024)) + " GB";
    }

    private int dp(int v) { return Math.round(v * activity.getResources().getDisplayMetrics().density); }

    private class FileListAdapter extends BaseAdapter {
        private List<Object> items = new ArrayList<>();
        private boolean loading;

        void setItems(List<Object> i) { items = i; loading = false; notifyDataSetChanged(); }
        void setLoading(boolean v) { loading = v; if (v) { items.clear(); notifyDataSetChanged(); } }

        public int getCount() { return loading ? 1 : items.size(); }
        public Object getItem(int p) { return loading ? "..." : items.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View cv, ViewGroup parent) {
            LinearLayout row;
            if (cv instanceof LinearLayout) { row = (LinearLayout) cv; row.removeAllViews(); }
            else { row = new LinearLayout(activity); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(dp(10), dp(8), dp(10), dp(8)); row.setFocusable(true); }

            if (loading) { TextView tv = new TextView(activity); tv.setText("Loading..."); tv.setTextColor(0xFF9E9E9E); tv.setTextSize(13); row.addView(tv); return row; }

            Object item = items.get(pos);
            if (item instanceof String) { TextView tv = new TextView(activity); tv.setText((String)item); tv.setTextSize(14); tv.setTextColor(0xFF6750A4); tv.setTypeface(Typeface.DEFAULT_BOLD); row.addView(tv); return row; }

            File f = (File) item; boolean isDir = f.isDirectory();
            TextView icon = new TextView(activity); icon.setText(isDir ? "📁" : "📄"); icon.setTextSize(16);
            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(30), LinearLayout.LayoutParams.WRAP_CONTENT));
            row.addView(icon);

            LinearLayout col = new LinearLayout(activity); col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            TextView name = new TextView(activity); name.setText(f.getName() + (isDir ? "/" : "")); name.setTextSize(13);
            name.setTextColor(isDir ? 0xFF212121 : 0xFF424242); if (isDir) name.setTypeface(Typeface.DEFAULT_BOLD); name.setSingleLine(true);
            col.addView(name);
            if (!isDir) { TextView sz = new TextView(activity); sz.setText(fmt(f.length())); sz.setTextSize(10); sz.setTextColor(0xFF9E9E9E); col.addView(sz); }
            row.addView(col);
            return row;
        }
    }
}
