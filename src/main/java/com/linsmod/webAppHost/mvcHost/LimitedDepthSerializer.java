package com.linsmod.webAppHost.mvcHost;

import com.google.gson.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;

public class LimitedDepthSerializer implements JsonSerializer<Object> {
    public Stack<Object> paths = new Stack<>();
    Gson defaultGson = new Gson();
    private int maxDepth;

    public LimitedDepthSerializer(int i) {
        this.maxDepth = i;
    }

    public static boolean stringType(Class<?> type) {
        if (type == null) {
            return false;
        }
        return type.isPrimitive() ||
                (type.equals(Integer.class) ||
                        type.equals(Long.class) ||
                        type.equals(Double.class) ||
                        type.equals(Float.class) ||
                        type.equals(Boolean.class) ||
                        type.equals(Character.class) ||
                        type.equals(Byte.class) ||
                        type.equals(String.class) ||
                        type.equals(Date.class) ||
                        type.equals(Enum.class) ||
                        type.equals(URI.class) ||
                        type.equals(Void.class));
    }

    private int depth() {
        return paths.size();
    }

    private boolean overLimit() {
        return depth() > maxDepth;
    }

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null || overLimit()) {
            return JsonNull.INSTANCE;
        }
        Object parent = paths.size() > 0 ? paths.peek() : null;
        paths.push(src);
        try {
            JsonElement d = defaultGson.toJsonTree(src, typeOfSrc);
            appendTypeName(src, d);
            return d;
        } finally {
            paths.pop();
        }
    }

    private JsonElement serializePath(Object parent, Object src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement element = null;
        if (src.getClass().isArray()) {
            JsonArray array = new JsonArray();
            if (!overLimit()) {
                int length = Array.getLength(src);
                for (int i = 0; i < length; i++) {
                    Object o = Array.get(src, i);
                    array.add(context.serialize(o, typeOfSrc));
                }
            }
            element = array;
        } else if (src instanceof Iterable) {
            JsonArray array = new JsonArray();
            if (!overLimit()) {
                Iterator iterator = ((Iterable) src).iterator();
                while (iterator.hasNext()) {
                    Object o = iterator.next();
                    array.add(context.serialize(o, typeOfSrc));
                }
            }
            element = array;
        } else if (stringType(src.getClass())) {
            try {
                element = new JsonPrimitive(defaultGson.toJson(src).toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            String debug = src.toString();
            String clazz = src.getClass().getName();
            JsonObject jsonObject = new JsonObject();
            if (overLimit()) {
                jsonObject.add("__warn__", new JsonPrimitive("max_depth_exceeded"));
            } else {
                Field[] fields = src.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    try {
                        jsonObject.add(field.getName(), context.serialize(field.get(src)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
//            if (depth() == 1) {
//                jsonObject.add("__depth__", new JsonPrimitive(maxDepth + " in maximum"));
//                jsonObject.add("__debug__", new JsonPrimitive(debug));
//                String[] split = clazz.split("\\$");
//                jsonObject.add("__clazz__", new JsonPrimitive(split.length == 2 ? split[1] : split[0]));
//            }
            element = jsonObject;
        }
        return element;
    }

    public void appendTypeName(Object src, JsonElement element) {
        if (depth() == 1 && element.isJsonObject() && src != null) {
            String debug = src.toString();
            if (debug.length() > 30) {
                debug = debug.substring(0, 30) + "...";
            }
            String clazz = src.getClass().getName();
            JsonObject jsonObject = (JsonObject) element;
            jsonObject.add("__depth__", new JsonPrimitive(maxDepth + " in maximum"));
            jsonObject.add("__vmobj__", new JsonPrimitive(debug));
            String[] split = clazz.split("\\$");
//            if (!((JsonObject) element).keySet().contains("__clazz__"))
//                jsonObject.add("__clazz__", new JsonPrimitive(split.length == 2 ? split[1] : split[0]));
        }
    }
}