package com.linsmod.webAppHost;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class AssetManager {
    private final Context context;

    public AssetManager(Context context) {
        this.context = context;
    }

    public InputStream open(String string) throws FileNotFoundException {
        return new FileInputStream(context.getFilesDir().resolve(string).toFile());
    }

    public String list(String fullDir) throws IOException {
        return null;
    }
}
