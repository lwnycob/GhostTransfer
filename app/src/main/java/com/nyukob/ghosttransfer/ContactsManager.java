package com.nyukob.ghosttransfer;
import android.content.SharedPreferences;
import java.util.*;

public class ContactsManager {
    private static final String KEY = "device_contacts";

    public static class Contact {
        public final String name, ip;
        public Contact(String name, String ip) {
            this.name = name != null ? name.trim() : "";
            this.ip = ip != null ? ip.trim() : "";
        }
    }

    public static List<Contact> load(SharedPreferences prefs) {
        List<Contact> r = new ArrayList<>();
        String raw = prefs.getString(KEY, "");
        if (raw.isEmpty()) return r;
        for (String line : raw.split("\n")) {
            String[] p = line.split("\t", 2);
            if (p.length == 2 && !p[0].isEmpty() && !p[1].isEmpty()) r.add(new Contact(p[0], p[1]));
        }
        return r;
    }

    public static void save(SharedPreferences prefs, List<Contact> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(list.get(i).name).append("\t").append(list.get(i).ip);
        }
        prefs.edit().putString(KEY, sb.toString()).apply();
    }

    public static void add(SharedPreferences p, String name, String ip) {
        List<Contact> l = load(p); l.add(new Contact(name, ip)); save(p, l);
    }

    public static void update(SharedPreferences p, int idx, String name, String ip) {
        List<Contact> l = load(p);
        if (idx >= 0 && idx < l.size()) { l.set(idx, new Contact(name, ip)); save(p, l); }
    }

    public static void delete(SharedPreferences p, int idx) {
        List<Contact> l = load(p);
        if (idx >= 0 && idx < l.size()) { l.remove(idx); save(p, l); }
    }
}
