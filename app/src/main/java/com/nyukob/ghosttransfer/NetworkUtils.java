package com.nyukob.ghosttransfer;
import java.net.Inet4Address; import java.net.HttpURLConnection;
import java.net.NetworkInterface; import java.net.URL;
import java.util.*; import java.util.concurrent.*;

public class NetworkUtils {
    public interface ScanCallback { void onResult(List<String> found); }
    public interface ProgressCallback { void onProgress(int done, int total, int found); }

    public static List<String> getLocalIPv4Addresses() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) continue;
                Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (ip != null && !ip.startsWith("169.254")) result.add(ip);
                    }
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static void scanSubnetAsync(final List<String> baseIps, final int port,
                                       final ScanCallback callback, final ProgressCallback progress) {
        new Thread(() -> {
            final int BATCH = 20, DELAY = 50, MS = 600;
            List<String> allIps = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (String ip : baseIps) {
                int d = ip.lastIndexOf('.');
                if (d <= 0) continue;
                String pfx = ip.substring(0, d + 1);
                if (seen.add(pfx)) for (int i = 1; i <= 254; i++) allIps.add(pfx + i);
            }
            List<String> found = new ArrayList<>();
            int total = (allIps.size() + BATCH - 1) / BATCH;
            for (int b = 0; b < total; b++) {
                int start = b * BATCH, end = Math.min(start + BATCH, allIps.size());
                ExecutorService pool = Executors.newFixedThreadPool(BATCH);
                List<Future<String>> futures = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    final String ip = allIps.get(i); final int p = port; final int ms = MS;
                    futures.add(pool.submit(() -> {
                        HttpURLConnection conn = null;
                        try {
                            URL url = new URL("http://" + ip + ":" + p + "/ping");
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(ms); conn.setReadTimeout(ms);
                            conn.setRequestMethod("GET"); conn.setInstanceFollowRedirects(false);
                            conn.setRequestProperty("Connection", "close");
                            return conn.getResponseCode() == 200 ? ip : null;
                        } catch (Exception e) { return null; }
                        finally { if (conn != null) conn.disconnect(); }
                    }));
                }
                for (Future<String> f : futures) {
                    try { String ip = f.get(MS + 200, TimeUnit.MILLISECONDS); if (ip != null) found.add(ip); }
                    catch (Exception ignored) {}
                }
                pool.shutdown();
                if (progress != null) progress.onProgress(b + 1, total, found.size());
                if (b < total - 1) try { Thread.sleep(DELAY); } catch (Exception ignored) {}
            }
            callback.onResult(found);
        }).start();
    }
}
