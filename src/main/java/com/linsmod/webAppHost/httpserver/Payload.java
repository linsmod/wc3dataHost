package com.linsmod.webAppHost.httpserver;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.linsmod.common.ArgumentError;
import com.linsmod.common.BaseUtility;
import com.linsmod.common.LinqList;
import com.linsmod.common.NotImplemented;
import com.linsmod.webAppHost.App;
import com.linsmod.webAppHost.mvcHost.ProtoArgument;
import com.linsmod.webAppHost.ann.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;

public abstract class Payload {
    private final Scope scope;

    protected Payload(Scope scope) {
        this.scope = scope;
    }

    /**
     * flag a payload comes from where, eg httpRequest
     *
     * @return
     */
    public Scope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "Payload{" +
                "scope=" + scope.namespace() +
                '}';
    }

    /**
     * 作为整体转换
     *
     * @param targetType
     * @return
     */

    public abstract Object parse(Class<?> targetType);


    /**
     * 提取单个参数
     *
     * @param targetType
     * @param name
     * @return
     */
    public abstract Object parse(Class<?> targetType, String name);

    /**
     * 作为参数列表转换
     *
     * @param parameters
     * @return
     */
    public Object[] parse(List<ProtoArgument> parameters) {
        return new LinqList<>(parameters).select(x -> parse(x.type, x.name)).toArray();
    }

    public static class JsonPayload extends Payload {
        private final String jsonText;

        public JsonPayload(String jsonText, Scope scope) {
            super(scope);
            this.jsonText = jsonText;
        }

        public String getRaw() {
            return this.jsonText;
        }

        @Override
        public Object parse(Class<?> targetType) {
            boolean fieldTypeSupported = BaseUtility.isFieldTypeSupported(targetType.getName());
            if (fieldTypeSupported && jsonText != null && jsonText.trim().startsWith("{")) {
                //map json object filed to primitive like type
                LinkedTreeMap o = (LinkedTreeMap) App.gson.fromJson(jsonText, Object.class);
                for (Object k : o.keySet()) {
                    Object v = o.get(k);
                    if (v != null) {
                        // ignores unused fields.
                        if (v.toString().equals("_") || v.toString().equals("uiEvent")) {
                            continue;
                        }
                        if (!v.getClass().equals(String.class)) {
                            if (targetType.equals(Long.class) || targetType.equals(long.class)) {
                                return Long.parseLong(v.toString());
                            }
                            if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
                                return Long.parseLong(v.toString());
                            }
                            if (targetType.equals(Double.class) || targetType.equals(double.class)) {
                                return Double.parseDouble(v.toString());
                            }
                            return v.toString();
                        } else {
                            return v;
                        }
                    }
                    return targetType.equals(String.class) ? "" : null;
                }
                throw new NotImplemented("ERR_DSFDFJO");
            }
            return App.gson.fromJson(jsonText, targetType);
        }

        @Override
        public Object parse(Class<?> targetType, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] parse(List<ProtoArgument> parameters) {
            return parseByName(parameters);
//            return parseByOrder(parameters);
        }

        public Object[] parseByName(List<ProtoArgument> parameters) {
            Object[] parsed = new Object[parameters.size()];

            try (StringReader sr = new StringReader(this.jsonText);
                 JsonReader jsonReader = App.gson.newJsonReader(sr)) {

                jsonReader.beginObject();

                while (jsonReader.hasNext()) {
                    String paramName = jsonReader.nextName();
                    int index = findParameterIndex(paramName, parameters);

                    if (index != -1) {
                        TypeAdapter<?> adapter = App.gson.getAdapter(parameters.get(index).getType());

                        try {
                            Object value = adapter.read(jsonReader);
                            parsed[index] = value;
                        } catch (JsonIOException | JsonSyntaxException e) {
                            throw new RuntimeException("Error parsing value for parameter: " + paramName, e);
                        }
                    } else {
                        // 如果参数列表中没有该字段，则跳过该值
                        jsonReader.skipValue();
                    }
                }

                jsonReader.endObject();
            } catch (IOException e) {
                throw new RuntimeException("IO Exception during JSON parsing", e);
            }

            return parsed;
        }

        private int findParameterIndex(String paramName, List<ProtoArgument> parameters) {
            for (int i = 0; i < parameters.size(); i++) {
                if (Objects.equals(paramName, parameters.get(i).getName())) {
                    return i;
                }
            }
            return -1; // 如果没找到匹配的参数名
        }

        public Object[] parseByOrder(Parameter[] parameters) {
            Object[] parsed = new Object[parameters.length];
            StringReader sr = new StringReader(this.jsonText);
            JsonReader jsonReader = App.gson.newJsonReader(sr);
            try {
                jsonReader.beginObject();

                // 修正：直接按顺序读取JSON键值对，与参数顺序匹配
                for (int i = 0; i < parameters.length && jsonReader.hasNext(); i++) {
                    jsonReader.nextName(); // 移动到下一个键名，但这里实际上忽略了键名，因为我们按顺序匹配
                    TypeAdapter<?> adapter = App.gson.getAdapter(parameters[i].getType());
                    Object o = adapter.read(jsonReader);
                    if (o == null && parameters[i].isAnnotationPresent(NotNull.class)) {
                        throw new RuntimeException("Parameter at position " + i + " requires [NotNull]");
                    }
                    parsed[i] = o;
                }

                // 确保所有参数都被处理了
                if (jsonReader.hasNext()) {
                    throw new RuntimeException("More JSON keys than expected parameters");
                }

                jsonReader.endObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return parsed;
        }

    }

    public static class UnspecifiedPayload extends Payload {
        private final String rawText;

        public UnspecifiedPayload(String rawText, Scope scope) {
            super(scope);
            this.rawText = rawText;
        }

        public String getRaw() {
            return this.rawText;
        }

        @Override
        public Object parse(Class<?> targetType) {
            throw new RuntimeException("can not parse from UnspecifiedPayload");
        }

        @Override
        public Object parse(Class<?> targetType, String name) {
            throw new RuntimeException("can not parse from UnspecifiedPayload");
        }

        @Override
        public Object[] parse(List<ProtoArgument> parameters) {
            throw new RuntimeException("can not parse from UnspecifiedPayload");
        }
    }

    public static class ListMapPayload extends Payload {
        private static final int TO_MAP_TAKE_SINGLE = 38;
        private static final int TO_MAP_TAKE_FIRST = 367;
        private final Map<String, List<String>> map;

        public ListMapPayload(Map<String, List<String>> d, Scope scope) {
            super(scope);
            this.map = d;
        }

        @Override
        public Object parse(Class<?> targetType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object parse(Class<?> targetType, String name) {
            if (name == null)
                throw new UnsupportedOperationException();
            if (map.containsKey(name) && targetType.equals(String.class))
                return map.get(name).get(0);
            return null;
        }

        /**
         * @param conversion TO_MAP_TAKE_SINGLE or TO_MAP_TAKE_FIRST
         * @return
         */
        public MapPayload asMapPayload(int conversion) {
            if (conversion == TO_MAP_TAKE_SINGLE) {
                return asMapPayload(values -> {
                    if (values.size() > 1)
                        throw new RuntimeException("multi-values not allowed in TO_MAP_TAKE_SINGLE conversion.");
                    return values.size() == 1 ? values.get(0) : null;
                });
            } else {
                return asMapPayload(values -> values.size() > 0 ? values.get(0) : null);
            }
        }

        public MapPayload asMapPayload(Function<List<String>, String> conversion) {
            Map<String, String> target = new HashMap<>();
            for (String s : map.keySet()) {
                target.put(s, conversion.apply(map.get(s)));
            }
            return new MapPayload(target, this.getScope());
        }
    }


    public static class MapPayload extends Payload {
        private final Map<String, String> map = new HashMap<>();

        public MapPayload(Map<String, String> d, Scope scope) {
            super(scope);
            for (String s : d.keySet()) {
                this.map.put(s, d.get(s));
            }
        }

        @Override
        public Object parse(Class<?> targetType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object parse(Class<?> targetType, String name) {
            if (targetType == File.class) {
                String path = map.get(name);
                if (path == null && name.startsWith("arg")) {
                    //编译器优化掉了名字, 变成了arg0,arg1... 导致无法匹配
                    //file -> /data/user/0/com.linsmod.lgd/cache/NanoHTTPD-3945189546185337367
                    path = map.get("file");
                    if (path == null && map.size() == 1) {
                        for (String s : map.keySet()) {
                            path = map.get(s);
                        }
                    }
                }
                if (path != null)
                    return new File(path);
                else {
                    throw new RuntimeException("File payload parse failed.");
                }
            }
            return getIgnoreCase(name);
        }

        private Object getIgnoreCase(String name) {
            for (String s : map.keySet()) {
                if (s.equalsIgnoreCase(name)) {
                    return map.get(s);
                }
            }
            return null;
        }

        @Override
        public Object[] parse(List<ProtoArgument> parameters) {
            throw new RuntimeException();
        }
    }

    public static class FilesPayload extends Payload {
        List<CachedFile> files;

        protected FilesPayload(List<CachedFile> files, Scope scope) {
            super(scope);
            this.files = files != null ? files : new ArrayList<>();
        }

        @Override
        public Object parse(Class<?> targetType) {
            if (this.files.size() == 1) {
                return files.get(0);
            } else if (this.files.size() == 0) {
                return null;
            }
            throw new ArgumentError("No uploaded files found");
        }

        CachedFile byName(String name) {
            for (CachedFile file : this.files) {
                String name1 = file.getName();
                if (name1 != null && name1.equalsIgnoreCase(name))
                    return file;
            }
            return null;
        }

        CachedFile single() {
            if (this.files.size() == 1)
                return this.files.get(0);
            throw new RuntimeException("multi files not allowed in single()");
        }

        @Override
        public Object parse(Class<?> targetType, String name) {
            CachedFile f = null;
            f = byName(name);
            if (f == null && name.startsWith("arg")) {
                //编译器优化掉了名字, 变成了arg0,arg1... 导致无法匹配
                //file -> /data/user/0/com.linsmod.lgd/cache/NanoHTTPD-3945189546185337367
                f = single();
            }
            if (f != null) {
                if (targetType == File.class) {
                    return f.getFile();
                } else if (targetType == CachedFile.class) {
                    return f;
                } else {
                    // should not be here.
                }
            }
            return null;
        }
    }

    public static class Scope {
        public static final int NS_HTTP = 1;
        public static final int NS_HTTP_HEADER = 100;
        public static final int NS_HTTP_BODY = 101;
        public static final int NS_HTTP_COOKIE = 102;
        public static final int NS_HTTP_PARAMS = 103;
        public static final Scope HTTP_BODY = new Scope(NS_HTTP_BODY);
        private final int ns;
        private final Scope parent;
        Map<String, Object> map = new HashMap<>();

        public Scope(int ns) {
            this.ns = ns;
            this.parent = null;
        }

        private Scope(int ns, Scope parent) {
            this.ns = ns;
            this.parent = parent;
        }

        public JsonPayload createJsonPayload(String d) {
            return new JsonPayload(d, this);
        }

        public UnspecifiedPayload createUnspecified(String d) {
            return new UnspecifiedPayload(d, this);
        }

        public FilesPayload createFilesPayload(List<CachedFile> files) {
            return new FilesPayload(files, this);
        }

        public MapPayload createMapPayload(Map<String, String> d) {
            return new MapPayload(d, this);
        }

        public ListMapPayload createListMapPayload(Map<String, List<String>> d) {
            return new ListMapPayload(d, this);
        }

        public Scope sub(int ns) {
            return new Scope(ns, this);
        }

        public void put(String key, Object value) {
            this.map.put(key, value);
        }

        public int ns() {
            return ns;
        }

        public String namespace() {
            switch (this.ns) {
                case NS_HTTP:
                    return "HTTP";
                case NS_HTTP_HEADER:
                    return "HTTP_HEADER";
                case NS_HTTP_BODY:
                    return "HTTP_BODY";
                case NS_HTTP_COOKIE:
                    return "NS_HTTP_COOKIE";
                case NS_HTTP_PARAMS:
                    return "NS_HTTP_PARAMS";
                default:
                    throw new IllegalStateException("Unexpected value: " + this.ns);
            }
        }
    }
}
