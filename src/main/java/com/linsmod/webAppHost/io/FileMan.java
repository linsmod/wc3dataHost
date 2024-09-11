package com.linsmod.webAppHost.io;

import com.linsmod.common.ArgumentError;
import com.linsmod.common.Log;
import com.linsmod.common.NextFile;
import com.linsmod.common.Strings;
import com.linsmod.webAppHost.App;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FileMan {
    private final Path path;


    public FileMan(File path) {
        this.path = path.toPath();
    }

    public FileMan(Path path) {
        this.path = path;
    }

    public String writeJson(String fileName, Object d) throws IOException {
        Path writePath = path.resolve(fileName);
        ensurePath(writePath);
        FileWriter fileWriter = new FileWriter(writePath.toFile());
        App.gson.toJson(d, fileWriter);
        fileWriter.flush();
        Log.d("FileMan", "file written:" + writePath);
        return writePath.toString();
    }

    public Object readJson(String fileName, Class<?> type) throws IOException {
        Objects.requireNonNull(fileName, "FileMan.readJson:无效参数");
        Path writePath = path.resolve(fileName);
        ensurePath(writePath);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(writePath.toFile()));
        return App.gson.fromJson(reader, type);
    }

    public Object openFile(String folder, String fileName, Class<?> type) throws IOException {
        Objects.requireNonNull(fileName, "FileMan.readJson:无效参数");
        Path writePath = path.resolve(folder).resolve(fileName);
        ensurePath(writePath);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(writePath.toFile()));
        return App.gson.fromJson(reader, type);
    }

    public String readAsText(String folder, String fileName) throws IOException {
        Objects.requireNonNull(fileName, "FileMan.readJson:无效参数");
        Path writePath = path.resolve(folder).resolve(fileName);
        ensurePath(writePath);

        InputStreamReader reader = new InputStreamReader(new FileInputStream(writePath.toFile()));
        BufferedReader reader2 = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader2.readLine()) != null) {
            sb.append(line + "\n");
        }
        return sb.toString();
    }

    public List<FileInfo> listFiles() {
        return FileEnumerator.enumerateFiles(this.path, true);
    }

    public List<FileInfo> listFiles(String sub, String displayName) {
        return listFiles(sub, displayName, false);
    }

    public List<FileInfo> listFiles(String sub, String displayName, boolean recursive) {
        if (sub.startsWith(".") || sub.startsWith("/") || sub.startsWith("\\")) {
            throw new RuntimeException("invalid sub path!");
        }
        List<FileInfo> fileInfos = FileEnumerator.enumerateFiles(this.path.resolve(sub), recursive);
        fileInfos.forEach(x -> {
            x.loc = sub;
            x.locName = displayName;
        });
        return fileInfos;
    }
    public String resolve(String fileName) {
        return path.resolve(fileName).toString();
    }
    public Path resolvePath(String fileName) {
        return path.resolve(fileName);
    }

    public File resolveFile(String fileName) {
        return path.resolve(fileName).toFile();
    }

    public boolean exists(String fileName) {
        Path writePath = path.resolve(fileName);
        ;
        return writePath.toFile().exists();
    }

    public void ensurePath(Path path) throws IOException {
        // 确保目标文件所在的目录存在，如果不存在则创建
        File parentDir = path.getParent().toFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory " + parentDir);
        }
    }

    public void deleteFiles(List<String> files) {
        for (String file : files) {
            Objects.requireNonNull(file, "无效参数");
            Path resolve = path.resolve(file);
            resolve.toFile().delete();
        }
    }

    public List<Object> openJsonFiles(String[] files, Class<?> type) throws IOException {
        List<Object> openlist = new ArrayList<>();
        for (String file : files) {
            if (file == null) {
                throw new NullPointerException("FileMan:未指定文件路径");
            }
            Path resolve = path.resolve(file);
            Object o = readJson(file, type);
            if (o == null) {
                throw new NullPointerException("FileMan:文件内容已损坏:" + resolve.getFileName());
            }
            openlist.add(o);
        }
        return openlist;
    }

    public String renameFile(String file, String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new ArgumentError("必须提供文件名");
        }
        if (name.contains("/")) {
            throw new ArgumentError("文件名无效，不要包含斜杠");
        }
        if (name.contains("\n")) {
            throw new ArgumentError("文件名无效，不要包含换行");
        }
        Path resolve = path.resolve(file);
        Path target = resolve.resolveSibling(name);
        if (target.toFile().isDirectory()) {
            throw new ArgumentError("同名文件已存在，换一个名字吧");
        }
        if (resolve.toFile().exists()) {
            if (target.toFile().exists()) {
                throw new ArgumentError("同名文件已存在，换一个名字吧");
            }
            if (resolve.toFile().renameTo(target.toFile())) {
                return target.toString();
            } else {
                throw new ArgumentError("操作失败，请确认目标文件名有效");
            }
        } else {
            throw new ArgumentError("不存在的文件请确认");
        }
    }

    /**
     * Generate trackable Object with a 'next sequenced name in today.'
     *
     * @param namePart
     * @return
     * @throws IOException
     */
    public NextFile todayNextIdName(String namePart) throws IOException {
        NextFile obj = new NextFile();
        long id = new Date().getTime();
        String date = Strings.dateNow("yyyyMMdd");
        Path dir = path.resolve(".todayFiles")
                .resolve(date)
                .resolve(namePart);
        dir.toFile().mkdirs();
        Path resolve = dir.resolve(String.valueOf(id));
        resolve.toFile().createNewFile();
        String[] list = dir.toFile().list();
        String name = namePart + "_" + date + "_" + list.length;
        obj.id = id;
        obj.name = name;
        obj.seqNum = list.length;
        obj.selfPath = resolve.toString();
        obj.namePart = namePart;
        writeJson(obj.selfPath + ".json", obj);
        return obj;
    }

    public Path basePath() {
        return this.path;
    }
}
