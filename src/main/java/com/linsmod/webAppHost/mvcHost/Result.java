package com.linsmod.webAppHost.mvcHost;

import com.google.gson.Gson;
import com.linsmod.common.BaseUtility;
import com.linsmod.webAppHost.App;
import com.linsmod.common.Strings;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class Result extends TypedData {
    public static final int ERR_AUTH_REQUIRED = 401;
    public static VoidResult Void = new VoidResult();
    public static NullResult Null = new NullResult();
    public static JsonResult EmptyList = new JsonResult(new Object[0]);
    public static JsonResult EmptyObject = new JsonResult(new Object());
    public String err;
    public List<String> errCause;
    public List<String> errStack;

    private Result() {

    }

    public static ErrorResult error(String err) {
        return new ErrorResult(err);
    }

    public static ErrorResult error(String err, int code) {
        return new ErrorResult(err, code);
    }

    public static ErrorResult error(Throwable err) {
        return new ErrorResult(err);
    }

    public static ErrorResult internalServerError(String err) {
        return new ErrorResult(err, 500);
    }

    public static ErrorResult internalServerError(Exception err) {
        return new ErrorResult(err, 500);
    }

    public static JsonResult json(Object data) {
        return new JsonResult(data);
    }

    public static AlertData alert(String title, String message) {
        return new AlertData(title, message, null, false);
    }

    public static AlertData alert(String title, String message, Object id) {
        return new AlertData(title, message, id, false);
    }

    public static AlertData toast(String message) {
        return new AlertData(null, message, null, true);
    }

    public boolean hasError() {
        return err != null;
    }

    public String toJson() {
        return App.gson.toJson(this);
    }

    public static class JsonResult extends Result {
        static Gson gson = new Gson();
        private final Object value;

        public JsonResult(Object data) {
            this.value = data;
        }

        @Override
        public String toJson() {
            if (value == null) {
                return "{}";
            }
            // 简单品种需要包裹起来，比如String，Number
            // 客户端使用 data.value来访问
            boolean fieldTypeSupported = BaseUtility.isFieldTypeSupported(value.getClass().getName());
            if (fieldTypeSupported) {
                return super.toJson();
            } else {
                // 复合类型直接序列化，
                // 客户端端用 data.field1,data.field2访问
                return App.gson.toJson(value);
            }
        }
    }

    public static class NullResult extends Result {
    }

    public static class VoidResult extends Result {
    }

    public static class ErrorResult extends Result {
        private final Integer code;
        public String errClass;

        public ErrorResult(String err) {
            this(err, null);
        }

        public ErrorResult(Throwable err) {
            this(err, null);
        }

        public ErrorResult(String err, Integer code) {
            this.err = err;
            this.code = code;
        }

        public ErrorResult(Throwable err, Integer code) {
            this.err = err.getMessage();
            this.errClass = err.getClass().getSimpleName();
            if (Strings.isNullOrEmpty(this.err)) {
                this.err = "内部错误";
            }
            this.errCause = new ArrayList<>();
            this.errStack = new ArrayList<>();
            Throwable inner = err;
            while (inner != null) {
                this.errStack.add(inner.toString());
                this.errCause.add(inner.getMessage());
                for (StackTraceElement stackTraceElement : inner.getStackTrace()) {
                    this.errStack.add(stackTraceElement.toString());
                }
                inner = inner.getCause();
            }
            this.code = code;
        }

        protected String wrap(Throwable ex) {
            StringBuilder sb = new StringBuilder();
            sb.append(ex.toString());
            for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
                sb.append("\r\n    at " + stackTraceElement.toString());
            }
            if (ex.getCause() != null) {
                sb.append("\n");
                sb.append(wrap(ex.getCause()));
            }
            return sb.toString();
        }

        public boolean isInternalError() {
            return code == 500;
        }
    }

    public static class AlertData extends Result {
        private Date createTime;
        private boolean useToast;
        private String title;
        private String message;
        private Object id; // server generated id used to flag what question to be confirm
        private int result;

        public AlertData(String title, String content) {
            this(title, content, null, false);
        }


        public AlertData(String title, String message, Object id, boolean useToast) {
            super();
            this.id = id;
            this.title = title;
            this.message = message;
            this.createTime = new Date();
            this.useToast = useToast;
        }

        public int getResult() {
            return result;
        }

        public Object getId() {
            return id;
        }
    }

    public static abstract class ContentResult extends Result {
        private String contentType;

        public ContentResult(String contentType) {
            super();
            this.contentType = contentType;
        }

        public String contentType(String provideDefault) {
            if (this.contentType != null && !this.contentType.equals(""))
                return this.contentType;
            return provideDefault;
        }
    }

    public static class FileResult extends ContentResult {
        private final File file;
        private long length = -1;

        public FileResult(File file, String contentType) {
            super(contentType);
            this.file = file;
        }

        public FileResult(File file, long length, String contentType) {
            super(contentType);
            this.file = file;
            this.length = length;
        }

        public long getLength() {
            return length;
        }

        public File getFile() {
            return file;
        }

        public FileInputStream createStream() throws FileNotFoundException {
            return new FileInputStream(file);
        }
    }

    public static class StreamResult extends ContentResult {

        private final InputStream stream;
        private final long length;

        public StreamResult(InputStream stream, String contentType) {
            super(contentType);
            this.stream = stream;
            this.length = -1;
        }

        public StreamResult(InputStream stream, long length, String contentType) {
            super(contentType);
            this.stream = stream;
            this.length = length;
        }

        public InputStream getStream() {
            return stream;
        }

        public long getLength() {
            return length;
        }
    }

    public static class ByteArrayResult extends StreamResult {

        public ByteArrayResult(byte[] d, String contentType) {
            super(new ByteArrayInputStream(d), contentType);
        }
    }
}
