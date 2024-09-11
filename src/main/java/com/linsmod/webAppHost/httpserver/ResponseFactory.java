package com.linsmod.webAppHost.httpserver;

import java.io.InputStream;

public interface ResponseFactory {
    ResponseFactory JSON = new JsonFactory();
    ResponseFactory HTML = new HtmlFactory();
    ResponseFactory PLAIN = new PlainFactory();

    NanoHttpd.Response create(NanoHttpd.Response.IStatus status, String content);

    NanoHttpd.Response create(NanoHttpd.Response.IStatus status, InputStream content);
}

class JsonFactory implements ResponseFactory {

    @Override
    public NanoHttpd.Response create(NanoHttpd.Response.IStatus status, String content) {
        return null;
    }

    @Override
    public NanoHttpd.Response create(NanoHttpd.Response.IStatus status, InputStream content) {
        return null;
    }
}

class HtmlFactory implements ResponseFactory {

    @Override
    public NanoHttpd.Response create(NanoHttpd.Response.IStatus status, String content) {
        return null;
    }

    @Override
    public NanoHttpd.Response create(NanoHttpd.Response.IStatus status, InputStream content) {
        return null;
    }
}

class PlainFactory implements ResponseFactory {

    @Override
    public NanoHttpd.Response create(NanoHttpd.Response.IStatus status, String content) {
        return null;
    }

    @Override
    public NanoHttpd.Response create(NanoHttpd.Response.IStatus status, InputStream content) {
        return null;
    }
}
