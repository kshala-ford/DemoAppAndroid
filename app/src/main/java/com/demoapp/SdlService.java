package com.demoapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.SdlManagerListener;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.rpc.enums.AppHMIType;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.TCPTransportConfig;
import com.smartdevicelink.util.DebugTool;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class SdlService extends Service {
    private static final String APP_ID = "12345";
    private static final String APP_NAME = "Demo App";
    private static final int FOREGROUND_ID = 111;

    private SdlManager sdlManager = null;

    // The manager listener helps you know when certain events that pertain to the SDL Manager happen
    // Here we will listen for ON_HMI_STATUS and ON_COMMAND notifications
    SdlManagerListener sdlManagerListener = new SdlManagerListener() {
        @Override
        public void onStart() {
            sdlManager.getScreenManager().setTextField1("Hello World!");

            // HMI Status Listener
            sdlManager.addOnRPCNotificationListener(FunctionID.ON_HMI_STATUS, new OnRPCNotificationListener() {
                @Override
                public void onNotified(RPCNotification notification) {
                    //OnHMIStatus onHMIStatus = (OnHMIStatus) notification;
                    //if (onHMIStatus.getHmiLevel() == HMILevel.HMI_FULL && onHMIStatus.getFirstRun()) {}
                }
            });
        }

        @Override
        public void onDestroy() {
            SdlService.this.stopSelf();
        }

        @Override
        public void onError(String info, Exception e) {
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(APP_ID, "SdlService", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager == null) return;

            notificationManager.createNotificationChannel(channel);
            Notification serviceNotification = new Notification.Builder(this, channel.getId())
                    .setContentTitle("Connected through SDL")
                    .setSmallIcon(R.drawable.ic_sdl)
                    .build();
            startForeground(FOREGROUND_ID, serviceNotification);
        }
    }

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }

        if (sdlManager != null) {
            sdlManager.dispose();
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startSdlManager();
        return START_STICKY;
    }

    void startSdlManager() {
        if (sdlManager == null) {
            DebugTool.enableDebugTool();

            BaseTransportConfig transport;

            // If you use an actual machine or test bench use Bluetooth
            //transport = new MultiplexTransportConfig(this, APP_ID, MultiplexTransportConfig.FLAG_MULTI_SECURITY_OFF);

            // For testing with an Emulator use TCP
            transport = new TCPTransportConfig(12345, "192.168.1.5", true);

            // The app type to be used
            List<AppHMIType> appType = Collections.singletonList(AppHMIType.MEDIA);

            // Create App Icon, this is set in the SdlManager builder
            SdlArtwork appIcon = new SdlArtwork("app_icon.png", FileType.GRAPHIC_PNG, R.drawable.ic_launcher_round, true);

            // The manager builder sets options for your session
            SdlManager.Builder builder = new SdlManager.Builder(this, APP_ID, APP_NAME, sdlManagerListener);
            builder.setAppTypes(new Vector<>(appType));
            builder.setTransportType(transport);
            builder.setAppIcon(appIcon);

            sdlManager = builder.build();
            sdlManager.start();
        }
    }
}
