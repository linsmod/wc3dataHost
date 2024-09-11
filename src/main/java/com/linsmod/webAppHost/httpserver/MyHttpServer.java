package com.linsmod.webAppHost.httpserver;

import com.linsmod.webAppHost.App;
import com.linsmod.webAppHost.mvcHost.Result;
import com.linsmod.webAppHost.mvcHost.Route;
import com.linsmod.webAppHost.mvcHost.AssetMan;
import com.linsmod.webAppHost.mvcHost.ProtoHost;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.linsmod.webAppHost.httpserver.NanoHttpd.HttpMethod.OPTIONS;
import static com.linsmod.webAppHost.httpserver.NanoHttpd.HttpMethod.POST;
import static com.linsmod.webAppHost.httpserver.NanoHttpd.Response.Status.*;
import static com.linsmod.webAppHost.httpserver.Payload.Scope.*;

public class MyHttpServer extends NanoHttpd {
    // 声明服务端端口
    public static Integer HTTP_PORT = 5200;
    private volatile static MyHttpServer myHttpServer;
    private static Map<String, ResponseFactory> contentFactoryMap = new HashMap<>();
    private static Map<String, String> fileTypes = new HashMap<>();
    private static String errorHtmlFormat = "<pre>${error}</pre>";

    static {
        contentFactoryMap.put("application/json", ResponseFactory.JSON);
        contentFactoryMap.put("text/json", ResponseFactory.JSON);
        contentFactoryMap.put("text/html", ResponseFactory.HTML);
        contentFactoryMap.put("text/plain", ResponseFactory.PLAIN);
    }

    private AssetMan wwwFs;

    public MyHttpServer(String ipAddress, int port) {
        super(ipAddress, port);
    }

    // 单例模式，获取实例对象，并传入当前机器IP
    public static MyHttpServer getInstance(String ipAddress) {
        if (myHttpServer == null) {
            synchronized (MyHttpServer.class) {
                if (myHttpServer == null) {
                    myHttpServer = new MyHttpServer(ipAddress, HTTP_PORT);
                }
            }
        }
        return myHttpServer;
    }

    public static Response createJsonResponse(Result result) throws Exception {
        String json;
        try {
            json = result.toJson();
        } catch (Exception ex) {
            throw new Exception("Failed to serialize content: " + ex.getMessage(), ex);
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    public static Response createJsonResponse(InputStream d) {
        return newChunkedResponse(Response.Status.OK, "application/json", d);
    }

    public static Response ok(String content) {
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, content);
    }

    public static Response badRequest() {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_HTML, null);
    }

    public static Response createStreamResponse(InputStream stream, long length, String mimeType) {
        if (length == -1)
            return newChunkedResponse(Response.Status.OK, mimeType, stream);
        return newFixedLengthResponse(OK, mimeType, stream, length);
    }

    public static Response accessControl(IHTTPSession session, Response response) {
        String origin = session.getHeaders().get("origin");
        if (origin != null) {
            response.addHeader("Access-Control-Allow-Origin", origin);
            response.addHeader("Access-Control-Allow-Headers", "Content-Type,clientId");
        }
        return response;
    }

    @Override
    public Response serve(IHTTPSession session) {
        // 解决客户端请求参数携带中文，出现中文乱码问题
        ContentType contentType = new ContentType(session.getHeaders().get("content-type")).tryUTF8();
        try {
            session.getHeaders().put("content-type", contentType.getContentTypeHeader());
            if (OPTIONS.equals(session.getHttpMethod())) {
                return accessControl(session, ok(""));
            } else {
                Payload.Scope scope = new Payload.Scope(NS_HTTP);
                scope.put("httpMethod", session.getHttpMethod());
                scope.put("contentType", contentType.getContentType());
                scope.put("contentType.encoding", contentType.getEncoding());
                Payload[] payloads = getPayloads(session, contentType, scope);
                Response resp = accessControl(session, dealWith(session, payloads));
                resp.addHeader("Server", "nanohttpd:2.3.1 on Android");
                String origin = session.getHeaders().get("host");
                resp.addHeader("origin", "http://" + origin);
                return resp;
            }
        } catch (Exception e) {
            if (contentType.isJson()) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", JsonError.toJson(e));
            } else
                return createErrorResponse(e);
        }
    }

    private Payload[] getPayloads(IHTTPSession session, ContentType contentType, Payload.Scope rootScope) throws ResponseException, IOException {
        List<Payload> payloads = new ArrayList<>();
        if (POST.equals(session.getHttpMethod())) {
            Payload.Scope bodyScope = rootScope.sub(NS_HTTP_BODY);
            Payload[] body = session.parseBody(bodyScope);
            for (Payload payload : body) {
                payloads.add(payload);
            }
        }
        payloads.add(rootScope.sub(NS_HTTP_PARAMS).createListMapPayload(session.getParameters()));
        payloads.add(rootScope.sub(NS_HTTP_HEADER).createMapPayload(session.getHeaders()));
        payloads.add(rootScope.sub(NS_HTTP_COOKIE).createMapPayload(session.getCookies().getMap()));
        return (Payload[]) payloads.toArray(new Payload[0]);
    }

    private Response dealWith(IHTTPSession session, Payload[] payloads) {
        String path = session.getUri();
        int lastd = path.lastIndexOf('.');
        Route route = new Route(path, lastd != -1 && path.substring(lastd).contains("."));
        CacheControl cc = new CacheControl(session.getHeaders());
        ProtoHost host = App.hostFactory.create(route, cc);
        try {
            Result result = host.process(payloads);
            Map<String, String> responseHeaders = host.responseHeaders;
            Response response = null;
            if (result instanceof Result.FileResult) {
                Result.FileResult fileResult = ((Result.FileResult) result);
                if (fileResult.getFile() == null) {
                    return createNotFoundResponse();
                } else {
                    FileInputStream stream = fileResult.createStream();
                    long size = stream.getChannel().size();
                    response = createStreamResponse(stream, size, fileResult.contentType(MIME_HTML));
                }
            } else if (result instanceof Result.StreamResult) {
                Result.StreamResult streamResult = ((Result.StreamResult) result);
                response = createStreamResponse(streamResult.getStream(), streamResult.getLength(), streamResult.contentType(MIME_HTML));
            } else {
                response = createJsonResponse(result);
            }
            if (result instanceof Result.ErrorResult) {
                if ("FileNotFoundException".equals(((Result.ErrorResult) result).errClass)) {
                    return createNotFoundResponse();
                }
                return response;
            }
            response.addHeaders(responseHeaders);
            Map<String, String> ccHeaders = cc.createCcHeaders();
            response.addHeaders(ccHeaders);
            response.setStatus(cc.statusCode);
            return response;
        } catch (Throwable e) {
            e.printStackTrace();
            return createErrorResponse(e, MIME_HTML);
        }
    }

    private Response createNotFoundResponse() {
        return newFixedLengthResponse(NOT_FOUND, MIME_HTML, "file not found");
    }

    private Response createErrorResponse(Exception ex) {
        return newFixedLengthResponse(INTERNAL_ERROR, MIME_HTML, preHtml(ex));
    }

    private Response createErrorResponse(Throwable ex, String mime) {
        if (ex instanceof FileNotFoundException) {
            return newFixedLengthResponse(NOT_FOUND, mime, preHtml(ex));
        }
        return newFixedLengthResponse(INTERNAL_ERROR, mime, preHtml(ex));
    }

    private Response contentError(String err, String contentType) {
        ResponseFactory responseFactory = contentFactoryMap.get(contentType);
        if (responseFactory == null) {
            return ResponseFactory.HTML.create(INTERNAL_ERROR, err);
        }
        return responseFactory.create(INTERNAL_ERROR, err);
    }

    private String preHtml(Throwable ex) {
        if (errorHtmlFormat != null)
            return errorHtmlFormat.replace("${error}", wrap(ex));
        return wrap(ex);
    }

    private String preHtml(String str) {
        return "<pre>" + str + "</pre>";
    }

    private String wrap(Throwable ex) {
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

    public void setWwwFileSystem(AssetMan wwwFs) {
        this.wwwFs = wwwFs;
        try {
            wwwFs.readString("index.html");
        } catch (IOException e) {
            throw new RuntimeException("index.html is not found in WwwFs path that you specified");
        }
        try {
            errorHtmlFormat = wwwFs.readString("error.html");
        } catch (Exception e) {
            return;
        }
    }
}