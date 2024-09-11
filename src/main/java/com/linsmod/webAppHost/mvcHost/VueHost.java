package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.Log;
import com.linsmod.common.Strings;
import com.linsmod.wc3.HashLookup;
import com.linsmod.wc3.wc3data;
import com.linsmod.webAppHost.App;
import com.linsmod.webAppHost.ann.ServiceMethod;
import com.linsmod.webAppHost.httpserver.CacheControl;
import com.linsmod.webAppHost.httpserver.CachedFile;
import com.linsmod.webAppHost.httpserver.Payload;
import com.linsmod.webAppHost.io.FileInfo;
import com.linsmod.webAppHost.io.FileMan;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VueHost extends ProtoHost {
    static Map<Long, String> idmap = new HashMap<>();
    private final AssetMan files;
    private final String indexHtml = "index.html";
    private final FileMan fileMan;
    private final boolean isApiHost;
    private HashMap<String, String> mime = new HashMap<>();

    public VueHost(AssetMan files, Route route, CacheControl cc, boolean isApiHost) {
        super(String.class, route, cc);
        this.files = files;
        this.isApiHost = isApiHost;
        mime.put(".txt", "text/plain");
        mime.put(".js", "text/javascript");
        mime.put(".html", "text/html");
        mime.put(".css", "text/css");
        mime.put(".ico", "image/x-icon");
        mime.put(".map", "application/json");
        mime.put(".dat", "application/octet-stream");
        mime.put(".json", "application/json");
        mime.put(".gz", "application/x-gzip");
        mime.put(".gzx", "application/octet-stream");
        mime.put(".png", "image/png");
        mime.put(".woff", "application/x-font-woff");
        mime.put(".woff2", "application/x-font-woff");
        mime.put(".ttf", "application/octet-stream");
        mime.put(".wasm", "application/wasm");
        this.fileMan = App.mediaFiles;
    }

    public Result process(Payload[] payloads) {
        try {

            Route route = getRoute();
            String path = route.getRewrittenPath();
            if (path.equals("/")) {
                return createAssetStreamResponse(this.indexHtml, "text/html");
            }
            if (isApiHost && !route.hasExt()) {
                return processMvc(payloads);
            }
            if (route.hasExt()) {
                for (String ext :
                        mime.keySet()) {
                    if (path.endsWith(ext)) {
                        Log.d("VueHost", "try file: " + path);
                        return createAssetStreamResponse(path, mime.get(ext));
                    }
                }
                throw new FileNotFoundException(path);
            } else {
                return createAssetStreamResponse(this.indexHtml, "text/html");
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return error(throwable);
        }
    }

    @ServiceMethod(argNames = "src,tileset", authRequired = false)
    public Result files(String src, String tileset) throws IOException {
        return images(src, tileset);
    }

    @ServiceMethod(argNames = "src,tileset", authRequired = false)
    public Result images(String src, String tileset) throws IOException {
        AssetMan wc3dataFiles = this.files.create("files");
        String path = this.getRoute().getPath();
        // 使用正则表达式匹配路径
        Pattern pattern = Pattern.compile("^/api/(images|files)/([a-fA-F0-9]{16})$");
        Matcher matcher = pattern.matcher(path);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid request.");
        }

        // 提取匹配到的信息
        String type = matcher.group(1);
        String idHex = matcher.group(2);
        int tilesetID = tileset == null ? 0 : (tileset.charAt(0) - 64);
//        http://192.168.1.106:5200/api/files/3fb63483212948f8?src=TerrainArt\Terrain.slk
        //idHi = 1068905603
        //idLo = 556353784

        // query by idhex
        wc3data.ArchiveEntry entry = wc3data.loadResource(wc3dataFiles, type, idHex, tilesetID);
        if (entry == null && !Strings.isNullOrEmpty(src)) {

            // fallback to using src param if passed.
            src = fixupDotaSrc(src);
            src = fixupSpellError(src);
            String s = wc3data.rmExt(src);
            HashLookup hashLookup = new HashLookup(s);
            entry = wc3data.loadResource(wc3dataFiles, type, hashLookup.hash_str, tilesetID);
        }
        if (entry == null) {
            return fileNotFound();
        }
        cacheControl.setLastModified(App.firstInstallTime, entry.createETag());
        byte[] bytes = entry.bytes();
        return new Result.ByteArrayResult(bytes, type.equals("images") ? "image/png" : "application/octet-stream");
    }

    private String fixupSpellError(String src) {
        String lower = src.toLowerCase();
        switch (lower) {
            case "objects\\spawnmodels\\orc\\orcblood\\orcbloodriderlesswyvernrider.mdx ":
                return "objects\\spawnmodels\\orc\\orcblood\\ordbloodriderlesswyvernrider.mdx ";
            default:
                return src;
        }
    }

    private String fixupDotaSrc(String src) {
        String lower = src.toLowerCase();
        switch (lower) {
            case "buildings\\other\\bookofsummoning\\bookofsummoning2.mdx":
                return "Buildings\\Other\\BookOfSummoning\\BookOfSummoning.mdx";
            case "abilities\\spells\\nightelf\\faeriefire\\faeriefiretarget0.mdx":
                return "abilities\\spells\\nightelf\\faeriefire\\faeriefiretarget.mdx";
            case "abilities\\spells\\nightelf\\faeriefire\\faeriefiretarget1.mdx":
                return "abilities\\spells\\nightelf\\faeriefire\\faeriefiretarget.mdx";
            case "abilities\\spells\\nightelf\\faeriefire\\faeriefiretarget2.mdx":
                return "abilities\\spells\\nightelf\\faeriefire\\faeriefiretarget.mdx";
            default:
                return src;
        }
    }

    private Result fileNotFound() {
        return new Result.FileResult(null, "");
    }

    @ServiceMethod(argNames = "type")
    public List<FileInfo> listFiles(String type) {
        List<FileInfo> files = new ArrayList<>();
        switch (type) {
            case "all":
                files.addAll(fileMan.listFiles("public", "公共题库"));
                files.addAll(fileMan.listFiles("mytest", "答题记录"));
                files.addAll(fileMan.listFiles("mytest.fails", "错题记录"));
                break;
            case "public":
                files.addAll(fileMan.listFiles("public", "公共题库"));
                break;
            case "mytest":
                files.addAll(fileMan.listFiles("mytest", "答题记录"));
                break;
            case "mytest.fails":
                files.addAll(fileMan.listFiles("mytest.fails", "错题记录"));
                break;
        }
//        for (FileInfo f :
//                files) {
//            f.id = f.lastMod;
//            idmap.put(f.lastMod, f.path);
//        }
        return files;
    }

    /**
     * 无需确认直接删除
     *
     * @param path
     */
    @ServiceMethod(argNames = "path")
    public void deleteFile(String path) {
        fileMan.deleteFiles(Arrays.asList(path));
    }

    @ServiceMethod(argNames = "files")
    public Result deleteFiles(List<String> files) {
        return createOpDialog("确认删除？", "正在删除" + files.size() + "个文件", new PendingAction() {
            @Override
            public Result perform(int code) throws Exception {
                if (code == BUTTON_OK) {
                    fileMan.deleteFiles(files);
                    return Result.toast("文件删除成功");
                } else {
                    return Result.toast("操作已取消");
                }
            }
        });
    }

    @ServiceMethod(argNames = "id,accepted")
    public Result performOp(String id, boolean accepted) throws Exception {
        return performOp(id, accepted ? BUTTON_OK : BUTTON_CANCEL);
    }

    @ServiceMethod(argNames = "path,name")
    public Result renameFile(String path, String name) {
        return super.data(fileMan.renameFile(path, name));
    }

    @ServiceMethod(argNames = "clientId", authRequired = false)
    public Result authClient(String clientId) {
        return super.authClient(clientId);
    }

    @ServiceMethod(argNames = "path", authRequired = false)
    public Result.FileResult image(String path) {
        File file = new File(path);
        if (file.exists()) {
            return new Result.FileResult(file, "image/png");
        }
        return null;
    }

    @ServiceMethod(argNames = "file")
    public String upload(CachedFile file) throws IOException {
        Path p = fileMan.resolvePath("upload/" + new Date().getTime() + file.getExt());
        p.toFile().getParentFile().mkdirs();
        try (
                BufferedInputStream inputStream = new BufferedInputStream(file.open());
                FileOutputStream outputStream = new FileOutputStream(p.toFile());
        ) {
            byte[] buffer = new byte[8192]; // 增大缓冲区大小
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            file.getFile().delete();
            return p.toString();
        } catch (IOException e) {
            throw new IOException("File upload failed: " + e.getMessage(), e);
        }
    }

    private Result createAssetStreamResponse(String path, String mime) throws IOException {

        // only none html is using cache
        if (!mime.contains("html"))
            cacheControl.setLastModified(App.firstInstallTime);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
//        long length = files.getLength(path);
        //java.io.FileNotFoundException: This file can not be opened as a file descriptor; it is probably compressed
        return new Result.StreamResult(files.open(path), -1, mime);
    }

    @Override
    protected Result processMvc(Payload[] payloads) throws Throwable {
        return super.processMvc(payloads);
    }
}
