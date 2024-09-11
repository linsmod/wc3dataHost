package com.linsmod.webAppHost;

import com.google.gson.*;
import com.linsmod.webAppHost.io.FileMan;
import com.linsmod.webAppHost.mvcHost.AssetMan;
import com.linsmod.webAppHost.mvcHost.DefaultHostFactory;
import com.linsmod.webAppHost.mvcHost.HostFactory;
import com.linsmod.webAppHost.mvcHost.LimitedDepthSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;

public class App {
    public static HostFactory hostFactory;
    public static AssetMan WwwFS;
    public static AssetMan DataFs;
    public static FileMan appFiles;
    public static FileMan mediaFiles;
    public static long firstInstallTime;
    public static long lastUpdateTime;
    //    public static Gson gson = new Gson();
    public static Gson gson = new GsonBuilder()
            .registerTypeAdapter(URI.class, new UriSerializer())
            .registerTypeAdapter(URI.class, new UriDeserializer())
            .registerTypeAdapter(Date.class, new DateSerializer())
            .registerTypeAdapter(Date.class, new DateDeserializer())
            .registerTypeHierarchyAdapter(Object.class, new LimitedDepthSerializer(10))
            .serializeNulls()
            .create();
    private static Context context;

    public static void Initialize(Context context) {
        App.context = context;
        ///data/user/0/com.linsmod.lgd/files/import/
        appFiles = new FileMan(context.getFilesDir());
        WwwFS = new AssetMan(context, "www");
        DataFs = new AssetMan(context,"");
        hostFactory = new DefaultHostFactory();
        getInstallTime();
    }

    static void getInstallTime() {
        //应用装时间
        if (!appFiles.resolvePath("install.log").toFile().exists()) {
            try {
                try (FileWriter fileWriter = new FileWriter(appFiles.resolveFile("install.log"))) {
                    fileWriter.write(String.valueOf(new Date().getTime()));
                    fileWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        firstInstallTime = appFiles.resolveFile("install.log").lastModified();
        lastUpdateTime = appFiles.resolveFile("install.log").lastModified();
    }

    public static Context getContext() {
        return context;
    }

    public static class UriSerializer implements JsonSerializer<URI> {
        @Override
        public JsonElement serialize(URI uri, Type type, JsonSerializationContext context) {
            if (uri == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(uri.toString());
        }
    }

    public static class DateSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date src, Type type, JsonSerializationContext context) {
            if (src == null) {
                return JsonNull.INSTANCE;
            }
            return new JsonPrimitive(src.getTime());
        }
    }

    public static class UriDeserializer implements JsonDeserializer<URI> {
        @Override
        public URI deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) {
                return null;
            }
            return URI.create(json.getAsString());
        }
    }

    public static class DateDeserializer implements JsonDeserializer<Date> {
        @Override
        public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) {
                return null;
            }
            return new Date(json.getAsLong());
        }
    }
}
