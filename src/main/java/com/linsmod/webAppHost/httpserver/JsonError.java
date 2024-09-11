package com.linsmod.webAppHost.httpserver;

import com.linsmod.webAppHost.App;

import java.util.ArrayList;
import java.util.List;

public class JsonError {
    public String err;
    public String kind;
    public List<String> stackTrace;

    public static String toJson(Exception ex) {
        JsonError error = new JsonError();
        error.err = ex.getMessage();
        error.kind = ex.getClass().getName();
        error.stackTrace = new ArrayList<>();
        for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
            error.stackTrace.add(stackTraceElement.toString());
        }
        return App.gson.toJson(error);
    }
}
