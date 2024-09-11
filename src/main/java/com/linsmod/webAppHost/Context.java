package com.linsmod.webAppHost;

import java.nio.file.Path;

public abstract class Context {

    public final AssetManager getAssets() {
        return new AssetManager(this);
    }

    public abstract Path getFilesDir();
}
