package com.linsmod.wc3;

import com.linsmod.common.LinqList;
import com.linsmod.common.Log;
import com.linsmod.common.Strings;
import com.linsmod.webAppHost.Context;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Wc3DataHome extends Context {
    private Path wc3dataHome;

    @Override
    public Path getFilesDir() {
        return wc3dataHome;
    }

    public Wc3DataHome() {
        String wc3DATAHome = System.getenv("WC3DATA_HOME");
        if (!Strings.isNullOrEmpty(wc3DATAHome)) {
            Path p = Paths.get(wc3DATAHome);
            if (!p.toFile().exists()) {
                throw new RuntimeException("Invalid path is provided by env 'WC3DATA_HOME', please check.");
            }
            this.wc3dataHome = p;
            Log.d("init", "Data path located using env[WC3DATA_HOME]: " + this.wc3dataHome);
        }
        if (this.wc3dataHome == null) {
            String property = System.getProperty("user.home");
            Path wc3data = Paths.get(property, "wc3data");
            File file = wc3data.toFile();
            if (file.exists() && file.isDirectory()) {
                this.wc3dataHome = wc3data;
                Log.d("init", "Using data path: " + this.wc3dataHome);
            }
        }
        if (this.wc3dataHome != null) {
            File file = this.wc3dataHome.toFile();
            if (file.exists() && file.isDirectory()) {
                LinqList<String> linqList = new LinqList<>("www/index.html",
                        "files/versions.json",
                        "files/images.dat");
                if (linqList.all((String x) -> {
                    if (!this.wc3dataHome.resolve(x).toFile().exists()) {
                        Log.d("init", "Error: Missing data file: " + this.wc3dataHome.resolve(x));
                        return false;
                    }
                    return true;
                })) {
                    return;
                }
            }
        }
        Log.d("E: WC3DATA_HOME env is not configured. Please follow the instructions in project README.");
        Log.d("main", "...Hanged due to an error. Will exit in 30 seconds.");
        int seconds = 30;
        while (seconds-- > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("E: WC3DATA is not configured. Please follow the instructions in project README.");
    }
}
