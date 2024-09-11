package com.linsmod.webAppHost.mvcHost;

import com.linsmod.webAppHost.httpserver.CacheControl;

public interface HostFactory {
    ProtoHost create(Route route, CacheControl cc);
}
