package com.linsmod.webAppHost.io;


import java.io.IOException;

public class FileInfo {
    public final String path;
    public final boolean dir;
    public final long size;
    public final long lastMod;
    private final String name;
    private final boolean isVirt;
    private final boolean isFile;
    public String loc;
    public String locName;

    public FileInfo(String name, String path, boolean dir, long size, long lastMod, boolean file, boolean virtual) {
        this.name = name;
        this.path = path;
        this.dir = dir;
        this.size = size;
        this.lastMod = lastMod;
        this.isFile = file;
        this.isVirt = virtual;
    }

    public Iterable<String> read() throws IOException {
        return new TextFile(path);
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "path='" + path + '\'' +
                ", isDirectory=" + dir +
                ", size=" + size +
                ", lastModified=" + lastMod +
                '}';
    }

    public String getName() {
        return this.name;
    }
}