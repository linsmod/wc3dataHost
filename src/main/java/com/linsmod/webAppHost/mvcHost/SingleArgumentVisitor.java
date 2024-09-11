package com.linsmod.webAppHost.mvcHost;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.linsmod.common.BaseUtility;
import com.linsmod.webAppHost.httpserver.Payload;
import com.linsmod.webAppHost.httpserver.CachedFile;

import java.io.File;
import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SingleArgumentVisitor {
    public final static String ErrorMissingRequiredArgument = "MissingRequiredArgument";
    private static final List<DateTimeFormatter> COMMON_DATE_FORMATS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME
    );
    private final ProtoArgument argument;
    Gson gson = new Gson();
    private Object value;
    private boolean endVisit;

    public SingleArgumentVisitor(ProtoArgument argument) {
        this.argument = argument;
    }

    private static LocalDateTime parseDateTimeWithFormats(String input, List<DateTimeFormatter> formats) {
        for (DateTimeFormatter format : formats) {
            try {
                return LocalDateTime.parse(input, format);
            } catch (DateTimeException ignored) {
                // Try the next format
            }
        }
        throw new DateTimeException("No valid date format found for input: " + input);
    }

    public String getName() {
        return argument.getName();
    }

    public boolean typeIsArray() {
        return argument.getType().isArray();
    }

    public boolean typeIsCollection() {
        return Collection.class.isAssignableFrom(argument.getType());
    }

    public boolean typeIsSimple() {
        Class<?> type = argument.getType();
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
                        type.equals(URI.class));
    }

    public Object convert(String d) {
        if (d == null) {
            return null;
        }
        if (typeIsArray()) {
            try {
                return gson.fromJson(d, argument.getType());
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("解析错误：参数类型是Array，应该传入JSON数组但并非如此", e);
            }
        } else if (typeIsCollection()) {
            try {
                return gson.fromJson(d, argument.getType());
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("解析错误：参数类型是Collection，应该传入JSON数组但并非如此", e);
            }
        } else {
            // 基本类型及枚举、日期、字符串转换
            Class<?> paramClass = argument.getType();
            if (paramClass == int.class || paramClass == Integer.class) {
                return Integer.parseInt(d);
            } else if (paramClass == long.class || paramClass == Long.class) {
                return Long.parseLong(d);
            } else if (paramClass == float.class || paramClass == Float.class) {
                return Float.parseFloat(d);
            } else if (paramClass == double.class || paramClass == Double.class) {
                return Double.parseDouble(d);
            } else if (paramClass == boolean.class || paramClass == Boolean.class) {
                return Boolean.parseBoolean(d);
            } else if (paramClass == char.class || paramClass == Character.class) {
                if (d.length() == 1) {
                    return d.charAt(0);
                } else {
                    throw new IllegalArgumentException("Invalid string length for char conversion");
                }
            } else if (paramClass == byte.class || paramClass == Byte.class) {
                return Byte.parseByte(d);
            } else if (paramClass.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) paramClass, d);
            } else if (paramClass == Date.class) {
                try {
                    LocalDateTime dateTime = parseDateTimeWithFormats(d, COMMON_DATE_FORMATS);
                    return Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
                } catch (DateTimeException e) {
                    throw new IllegalArgumentException("Invalid date format: " + d, e);
                }
            } else if (paramClass == String.class) {
                return d;
            } else {
                throw new IllegalArgumentException("Unsupported type conversion: " + paramClass.getName());
            }
        }
    }

    public Payload typedFirst(Payload[] payloads, Class<? extends Payload> type) {
        for (Payload payload : payloads) {
            if (payload.getClass() == type)
                return payload;
        }
        return null;
    }

    public void visit(Payload[] payloads) {
        if (this.argumentType() == CachedFile.class
                || this.argumentType() == File.class) {
            Payload payload = typedFirst(payloads, Payload.FilesPayload.class);
            if (payload != null) {
                this.visitFiles((Payload.FilesPayload) payload);
            }
        } else {
            for (Payload payload : payloads) {
                if (payload instanceof Payload.JsonPayload) {
                    this.visitJson(((Payload.JsonPayload) payload));
                } else if (payload instanceof Payload.ListMapPayload) {
                    this.visitListMap((Payload.ListMapPayload) payload);
                } else if (payload instanceof Payload.MapPayload) {
                    this.visitMap((Payload.MapPayload) payload);
                } else if (payload instanceof Payload.UnspecifiedPayload) {
                    this.visitUnspecified((Payload.MapPayload) payload);
                } else {
                    throw new RuntimeException("VisitLost:" + payload);
                }
                if (endVisit) {
                    break;
                }
            }
        }
    }

    public void setValue(Object value, boolean endVisit) {
        if (value != null && !argumentType().isAssignableFrom(value.getClass())) {
            this.value = convert(value.toString());
        } else {
            this.value = value;
        }
        this.endVisit = endVisit;
    }

    public Object getValue() {
        return this.value;
    }

    private void visitFiles(Payload.FilesPayload d) {
        Object result = d.parse(this.argumentType());
        setValue(result, result != null);
    }

    private void visitUnspecified(Payload.MapPayload d) {
        Object result = d.parse(this.argumentType());
        setValue(result, result != null);
    }

    private void visitMap(Payload.MapPayload d) {
        Object result = d.parse(this.argumentType(), this.getName());
        setValue(result, result != null);
    }

    private void visitListMap(Payload.ListMapPayload d) {
        Object result = d.parse(this.argumentType(), this.getName());
        setValue(result, result != null);
    }

    private void visitJson(Payload.JsonPayload d) {
        if (this.argument.hasName()
                && BaseUtility.isFieldTypeSupported(this.argumentType().getName())) {
            List<ProtoArgument> arguments = new ArrayList<>();
            arguments.add(argument);
            Object[] results = d.parseByName(arguments);
            setValue(results[0], true);
        } else {
            Object result = d.parse(this.argumentType());
            setValue(result, result != null);
        }
    }

    private Class<?> argumentType() {
        return this.argument.getType();
    }
}