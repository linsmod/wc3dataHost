package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.Strings;

public class Route {
    private final String path;
    private final String[] splitPathParts;
    private final boolean requestStaticFile;
    private String ext;
    private String controllerName;
    private String methodName;
    private int methodIndex = -1;
    private int controllerIndex = -1;
    private String rewrittenPath;

    public Route(String path, boolean requestStaticFile) {
        this.path = path;
        this.rewrittenPath = path;
        this.splitPathParts = path.split("/");
        this.requestStaticFile = requestStaticFile;
        for (int i = 0; i < this.splitPathParts.length; i++) {
            if (this.splitPathParts[i].equals(""))
                continue;
            if (controllerIndex == -1) {
                // for path which has at least two parts
                // parsing controller
                this.controllerIndex = i;
            } else {
                this.methodIndex = i;
                break;
            }
        }

        // for path which only have a part eg: /parseSql
        // treat as having method but no controller
        if (controllerIndex != -1 && methodIndex == -1) {
            methodIndex = controllerIndex;
            controllerIndex = -1;
        }

        if (controllerIndex != -1)
            this.controllerName = splitPathParts[controllerIndex];
        if (methodIndex != -1)
            this.methodName = splitPathParts[methodIndex];

        String[] split = path.split("/");
        if (split.length > 0) {
            int i = split[split.length - 1].indexOf(".");
            if (i != -1)
                this.ext = split[split.length - 1].substring(i);
        }
    }

    public boolean isRequestStaticFile() {
        return requestStaticFile;
    }

    public String getMethod() {
        return this.methodIndex != -1 ? splitPathParts[methodIndex] : "";
    }

    public String getPath() {
        return path;
    }

    public String getRewrittenPath() {
        return rewrittenPath;
    }

    public void setRewrittenPath(String path) {
        this.rewrittenPath = path;
    }

    public boolean hasExt() {
        return !Strings.isNullOrEmpty(ext);
    }
}
