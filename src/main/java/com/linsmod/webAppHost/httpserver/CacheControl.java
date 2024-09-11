package com.linsmod.webAppHost.httpserver;

import com.linsmod.common.HeaderParser;
import com.linsmod.common.HttpDate;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class CacheControl {
    private final boolean ccPresent;
    Date servedDate;
    /**
     * Etag from request.
     */
    String ifMatch;
    Date lastModified;
    String etag;
    NanoHttpd.Response.IStatus statusCode = NanoHttpd.Response.Status.OK;
    /**
     * Etag from request.
     */
    private String ifNoneMatch;
    private boolean noCache;
    private boolean noStore;
    private boolean mustRevalidate;
    private boolean isPublic;
    private int sMaxAgeSeconds;
    private int maxAgeSeconds;
    private Date ifModifiedSince;

    public CacheControl(Map<String, String> requestHeaders) {
        HeaderParser.CacheControlHandler handler = new HeaderParser.CacheControlHandler() {
            @Override
            public void handle(String directive, String parameter) {
                if (directive.equalsIgnoreCase("no-cache")) {
                    noCache = true;
                } else if (directive.equalsIgnoreCase("no-store")) {
                    noStore = true;
                } else if (directive.equalsIgnoreCase("max-age")) {
                    maxAgeSeconds = HeaderParser.parseSeconds(parameter);
                } else if (directive.equalsIgnoreCase("s-maxage")) {
                    sMaxAgeSeconds = HeaderParser.parseSeconds(parameter);
                } else if (directive.equalsIgnoreCase("public")) {
                    isPublic = true;
                } else if (directive.equalsIgnoreCase("must-revalidate")) {
                    mustRevalidate = true;
                }
            }
        };
        if (requestHeaders.containsKey("Cache-Control")) {
            HeaderParser.parseCacheControl(requestHeaders.get("Cache-Control"), handler);
            this.ccPresent = true;
        } else {
            this.ccPresent = false;
        }
        for (String s : requestHeaders.keySet()) {
            if ("If-None-Match".equalsIgnoreCase(s)) {
                ifNoneMatch = requestHeaders.get(s);
                // prefer 412
            }
            if ("If-Match".equalsIgnoreCase(s)) {
                ifMatch = requestHeaders.get(s);
                // prefer 412
            } else if ("IF-Modified-Since".equalsIgnoreCase(s)) {
                ifModifiedSince = HttpDate.parse(requestHeaders.get(s));
                // prefer 304
            }
        }
//        for (String s : responseHeaders.keySet()) {
//            parseServerHeaders(s, responseHeaders.get(s));
//        }
    }

    public void setLastModified(long lastModified) {
        setLastModified(new Date(lastModified), null);
    }

    public void setLastModified(long lastModified, String resourceETag) {
        setLastModified(new Date(lastModified), resourceETag);
    }

    public void setLastModified(Date lastModified, String resourceETag) {
        this.lastModified = new Date(lastModified.getYear(),
                lastModified.getMonth(),
                lastModified.getDate(),
                lastModified.getHours(),
                lastModified.getMinutes(),
                lastModified.getSeconds()
        );
        if (resourceETag == null && this.lastModified.equals(ifModifiedSince)) {
            this.statusCode = NanoHttpd.Response.Status.NOT_MODIFIED;
        } else if (resourceETag != null) {
            this.etag = resourceETag;
            if (this.ifNoneMatch != null && this.ifNoneMatch.equals(resourceETag)) {
                this.statusCode = NanoHttpd.Response.Status.NOT_MODIFIED;
            }
        }
    }

    public Map<String, String> createCcHeaders() {
        // response headers
        Map<String, String> headers = new LinkedHashMap<>();
        if (lastModified != null) {
            headers.put("Last-Modified", HttpDate.format(lastModified));
        }
        if (etag != null) {
            headers.put("ETag", etag);
        }
//        Cache-Control是HTTP/1.1中引入的，用于定义缓存策略，它比Expires头提供了更细粒度的控制。
//        常见的指令包括：max-age（定义资源在本地缓存中可以保留的最大时间，以秒为单位）、public（资源可以被任何缓存存储）、private（资源只能被用户代理（如浏览器）缓存）、no-cache（强制每次请求都直接发送给服务器，但允许缓存存储请求的响应，以便离线使用）、no-store（不允许缓存响应的任何内容）。
//        当Cache-Control和Expires同时存在时，Cache-Control具有更高的优先级。
//        headers.put("Cache-Control", "max-age=14400; public");
        return headers;
    }
}
