package com.linsmod;

import com.linsmod.common.Log;
import com.linsmod.wc3.Wc3DataHome;
import com.linsmod.webAppHost.App;
import com.linsmod.webAppHost.Context;
import com.linsmod.common.Strings;
import com.linsmod.webAppHost.mvcHost.AssetMan;
import com.linsmod.webAppHost.httpserver.MyHttpServer;

public class Main {
    private static MyHttpServer httpServer;

    public static void main(String[] args) throws InterruptedException {
        try {
            Log.d("main", "Wc3dataHost is starting...");
            App.Initialize(new Wc3DataHome());
            startHttpd(App.getContext(), false);
        } catch (Exception e) {
            Log.e("ERROR", e);
        }
    }

    public static void startHttpd(Context context, boolean daemon) {
        try {
            String wc3DATABindIp = System.getenv("WC3DATA_HOST_IP");
            if (Strings.isNullOrEmpty(wc3DATABindIp)) {
                wc3DATABindIp = "127.0.0.1";
                Log.d("[init] HOST_IP:127.0.0.1. Set env['WC3DATA_HOST_IP'] to override it ");
            } else {
                Log.d("[init] HOST_IP:" + wc3DATABindIp + " is set using env['WC3DATA_HOST_IP']");
            }

            String wc3DATABindPort = System.getenv("WC3DATA_HOST_PORT");
            if (Strings.isNullOrEmpty(wc3DATABindPort)) {
                wc3DATABindPort = "5200";
                Log.d("[init] HOST_PORT:5200. Set env['WC3DATA_HOST_PORT'] to override it ");
            } else {
                Log.d("[init] HOST_PORT:" + wc3DATABindPort + " is set using env['WC3DATA_HOST_PORT']");
            }
            MyHttpServer.HTTP_PORT = Integer.parseInt(wc3DATABindPort);
            httpServer = MyHttpServer.getInstance(wc3DATABindIp);
            httpServer.setWwwFileSystem(new AssetMan(context, "www"));
            if (!httpServer.wasStarted()) {
                httpServer.start(daemon);
                Log.d("[httpd] Http server started");
                Log.d("[httpd] Start from here: " + httpServer.getUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}