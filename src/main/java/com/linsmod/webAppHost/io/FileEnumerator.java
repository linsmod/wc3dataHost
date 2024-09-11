package com.linsmod.webAppHost.io;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileEnumerator {

    /**
     * 枚举指定目录下的所有文件信息，根据参数决定是否递归枚举子目录。
     *
     * @param directoryPath 目录路径
     * @param recursive     是否递归枚举子目录
     * @return 文件信息列表
     */
    public static List<FileInfo> enumerateFiles(Path directoryPath, boolean recursive) {
        List<FileInfo> filesInfo = new ArrayList<>();
        File directory = directoryPath.toFile();
        enumerateFilesRecursively(directory, filesInfo, recursive);
        return filesInfo;
    }

    private static void enumerateFilesRecursively(File directory, List<FileInfo> filesInfo, boolean recursive) {
        File[] entries = directory.listFiles();

        if (entries != null) {
            for (File entry : entries) {
                FileInfo fileInfo = new FileInfo(
                        entry.getName(),
                        entry.getAbsolutePath(),
                        entry.isDirectory(),
                        entry.length(),
                        entry.lastModified(),
                        entry.isFile(),
                        false
                );

                filesInfo.add(fileInfo);

                if (recursive && entry.isDirectory()) {
                    enumerateFilesRecursively(entry, filesInfo, recursive);
                }
            }
        }
    }
}