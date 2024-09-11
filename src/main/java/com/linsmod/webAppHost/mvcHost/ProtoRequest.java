package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.LinqList;
import com.linsmod.webAppHost.httpserver.Payload;

public class ProtoRequest {
    private final Route route;
    private LinqList<Payload> payloads = new LinqList<>();

    public ProtoRequest(Route route, Payload[] payloads) {
        this.route = route;
        this.payloads = new LinqList<>(payloads);
    }

    public String getClientId() {
        Payload first = payloads.first(x -> x.getScope().ns() == Payload.Scope.NS_HTTP_HEADER);
        if (first != null) {
            return (String) first.parse(String.class, "clientId");
        }
        return null;
    }
}
