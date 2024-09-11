package com.linsmod.webAppHost.httpserver;

import com.linsmod.common.Strings;
import com.linsmod.webAppHost.io.TextFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class CachedFile {
    String name;
    String path;

    public CachedFile(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public InputStream open() throws FileNotFoundException {
        return new FileInputStream(path);
    }

    public TextFile openText() {
        return new TextFile(this.path);
    }

    public String getPath() {
        return this.path;
    }

    public String getName() {
        return this.name;
    }

    public String getExt() {
        if (Strings.isNullOrEmpty(name)) { // 假设Strings.isNullOrEmpty是一个检查null或空字符串的工具方法
            return ""; // 如果name是null或空字符串，返回空字符串
        }
        int i = name.indexOf('.');
        if (i == -1 || name.substring(i).contains("/")) {
            return "";
        }
        return name.substring(i);
    }

    public String getNameNoExt() {
        if (Strings.isNullOrEmpty(name)) { // 假设Strings.isNullOrEmpty是一个检查null或空字符串的工具方法
            return ""; // 如果name是null或空字符串，返回空字符串
        }
        // 使用正则表达式来分割文件名和扩展名
        // 注意：这里假设文件名中只有一个"."作为扩展名的分隔符
        String[] split = name.split("\\.", 2); // 使用limit 2来避免过多的分割
        // 如果文件名没有"."，split将只返回一个元素（即整个文件名）
        // 如果有"."，split将返回两个元素，第一个元素是文件名（不含扩展名），第二个元素是扩展名
        if (split.length > 0) {
            // 返回第一个元素，即文件名（不含扩展名）
            return split[0];
        }

        // 理论上，如果split.length <= 0，这不应该发生，因为我们已经检查了name不是null或空字符串
        // 但为了完整性，我们可以返回一个默认值或抛出异常
        return ""; // 或者你可以选择抛出一个异常
    }


    public File getFile() {
        return new File(path);
    }
}
