package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.LinqList;
import com.linsmod.webAppHost.AssetManager;
import com.linsmod.webAppHost.Context;
import com.linsmod.webAppHost.io.DetectorStream;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AssetMan {
    private final String fsPath;
    private final Context context;
    private final AssetManager assets;

    public AssetMan(Context context, String pathRelativeToAssetBasePath) {
        this.fsPath = pathRelativeToAssetBasePath;
        this.context = context;
        this.assets = context.getAssets();
    }

    public AssetMan create(String path) {
        return new AssetMan(context, file(path).toString());
    }

    public LinqList<String> list(boolean recursive) {
        return makeRelativePaths(this.fsPath, list(this.fsPath, recursive));
    }

    private List<String> list(String fullDir, boolean recursive) {
        try {
            List<String> fullPaths = makeFullPaths(fullDir, Arrays.asList(assets.list(fullDir)));
            if (!recursive)
                return fullPaths;
            List<String> recursiveSubFiles = new ArrayList<>();
            for (String fullPath : fullPaths) {
                int iLastSlash = fullPath.lastIndexOf("/");
                if (iLastSlash != -1) {
                    int iDot = fullDir.lastIndexOf(".", iLastSlash);
                    if (iDot == -1) { // no dot found , maybe a folder
                        recursiveSubFiles.addAll(list(fullPath, true));
                    }
                } else if (!fullPath.contains(".")) { // maybe a folder
                    recursiveSubFiles.addAll(list(fullPath, true));
                }
            }
            recursiveSubFiles.addAll(fullPaths);
            return recursiveSubFiles;
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }

    }

    private LinqList<String> makeRelativePaths(String parent, List<String> items) {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, items.get(i).substring(parent.length()));
        }
        return new LinqList<String>(items);
    }

    private List<String> makeFullPaths(String parent, List<String> items) {
//        String p = parent.substring(fsPath.length());
//        if (p.startsWith("/"))
//            p = p.substring(1);
        for (int i = 0; i < items.size(); i++) {
            items.set(i, Paths.get(parent, items.get(i)).toString());
        }
        return items;
    }

    public InputStream open(String fileName) throws IOException {
        Path path = Paths.get(fsPath, fileName);
        String string = path.toString();
        InputStream open = assets.open(string);
        return open;
    }

    /**
     * copy to disk then open it. the copied will not deleted until user uninstall app.
     * disk
     *
     * @param assetName
     * @return
     * @throws IOException
     */
    public File extract(String assetName) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        String assetFullPath = file(assetName).toString();
        File outFile = new File(context.getFilesDir().toFile(), assetFullPath);
        try {
            in = open(assetName);
            outFile.getParentFile().mkdirs();
            if (!outFile.exists()) {
                out = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
        return outFile;
    }

    public RandomAccessFile openExtractedRaf(String assetName) throws IOException {
        return new RandomAccessFile(extract(assetName), "r");
    }

    public FileOutputStream openExtracted(String assetName) throws IOException {
        return new FileOutputStream(extract(assetName));
    }

    public File file(String fileName) {
        return Paths.get(fsPath, fileName).toFile();
    }

    public InputStream open(String fileName, Map<String, String> replacements) throws IOException {
        Objects.requireNonNull(replacements);
        return new DetectorStream(open(fileName), replacements);
    }

    public String readString(String fileName) throws IOException {
        StringBuilder fileContent = new StringBuilder();
        try (InputStream inputStream = open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append('\n');
            }
        }
        return fileContent.toString().trim();
    }

    public String readString(String fileName, Map<String, String> strReplacements) throws IOException {
        StringBuilder fileContent = new StringBuilder();
        try (InputStream inputStream = open(fileName, strReplacements);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append('\n');
            }
        }
        return fileContent.toString().trim(); // trim()用于去除最后可能多余的换行符
    }

    public String[] readLines(String fileName) throws IOException {
        List<String> fileContent = new ArrayList<>();
        try (InputStream inputStream = open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.add(line);
            }
        }
        return fileContent.toArray(new String[0]);
    }

    public String[] readLines(String fileName, Map<String, String> strReplacements) throws IOException {
        List<String> fileContent = new ArrayList<>();
        try (InputStream inputStream = open(fileName, strReplacements);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (String s : strReplacements.keySet()) {
                    line = line.replaceAll(s, strReplacements.get(s));
                }
                fileContent.add(line);
            }
        }
        return fileContent.toArray(new String[0]);
    }
}
