package com.linsmod.webAppHost.mvcHost;

import com.linsmod.webAppHost.App;
import com.linsmod.webAppHost.httpserver.CacheControl;

import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultHostFactory implements HostFactory {
    private final HostFactory WwwHost = (route, cc) -> new VueHost(App.WwwFS, route, cc, false);
    private final HostFactory AssetHost = (route, cc) -> new VueHost(App.DataFs, route, cc, false);
    private final HostFactory ApiHost = (route, cc) -> new VueHost(App.DataFs, route, cc, true);
    Map<String, HostFactory> services = new LinkedHashMap<>();
    Map<String, String> rewrites = new LinkedHashMap<>();

    public DefaultHostFactory() {
        services.put("^/[0-9]+", WwwHost);
        services.put("^/files/images.dat", AssetHost);
        services.put("^/files", ApiHost);

        rewrites.put("^/api/", "/files/");
        rewrites.put("^/[0-9]+", "/www/index.html");
    }

    @Override
    public ProtoHost create(Route route, CacheControl cc) {
        String path = route.getPath();

        // rewrite path
        for (String key : rewrites.keySet()) {
            String v = rewrites.get(key);
            if (match(path, key)) {
                path = path.replaceFirst(key, v);
                break;
            }
        }

        // match by rewritten path
        for (String servicePath : services.keySet()) {
            HostFactory hostFactory = services.get(servicePath);
            if (match(path, servicePath)) {
                route.setRewrittenPath(path);
                return hostFactory.create(route, cc);
            }
        }
        return WwwHost.create(route, cc);
    }


    /**
     * Determines if the given servicePath matches the path as a prefix, considering directories and static files.
     * Supports matching of subdirectories and files within those subdirectories.
     *
     * @param servicePath The servicePath to match against the path.
     * @return true if the servicePath is a prefix of the path leading to a directory or a static file, false otherwise.
     */
    public boolean match(String path, String servicePath) {
        return path.matches(servicePath + ".*");
    }
}
