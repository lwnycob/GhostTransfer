package com.nyukob.ghosttransfer;
import android.app.*; import android.content.Intent; import android.os.*; 
public class TransferService extends Service {
    public static final String CHANNEL_ID="ghost_transfer", ACTION_STOP="STOP_SERVER";
    public static final int NOTIF_ID=1;
    private static final int API_O=26;
    private GhostHttpServer server;
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){stopServer();stopSelf();return START_NOT_STICKY;}
        createChannel();startForeground(NOTIF_ID,buildNotif());
        try{if(server!=null)server.stop();server=new GhostHttpServer();server.start();ShareState.serverRunning=true;ShareState.addLog("Server started on port "+GhostHttpServer.PORT);}
        catch(Exception e){ShareState.addLog("Error: "+e.getMessage());ShareState.serverRunning=false;}
        return START_STICKY;
    }
    @Override public void onDestroy(){stopServer();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
    private void stopServer(){if(server!=null){try{server.stop();}catch(Exception ignored){}server=null;}ShareState.serverRunning=false;ShareState.addLog("Server stopped");}
    @SuppressWarnings("NewApi") private void createChannel(){
        if(Build.VERSION.SDK_INT>=API_O)try{
            Class<?> c=Class.forName("android.app.NotificationChannel");Object ch=c.getConstructor(String.class,CharSequence.class,int.class).newInstance(CHANNEL_ID,"Ghost Transfer",3);
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.getClass().getMethod("createNotificationChannel",c).invoke(nm,ch);
        }catch(Exception ignored){}
    }
    @SuppressWarnings("deprecation") private Notification buildNotif(){
        Notification.Builder b=new Notification.Builder(this).setContentTitle("Ghost Transfer").setContentText("Active - port "+GhostHttpServer.PORT).setSmallIcon(android.R.drawable.stat_sys_upload).setOngoing(true);
        if(Build.VERSION.SDK_INT>=API_O)try{b.getClass().getMethod("setChannelId",String.class).invoke(b,CHANNEL_ID);}catch(Exception ignored){}
        return b.build();
    }
}
