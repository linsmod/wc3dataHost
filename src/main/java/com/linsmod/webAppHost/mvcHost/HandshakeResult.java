package com.linsmod.webAppHost.mvcHost;

public class HandshakeResult {
    public String clientId;
    public long createDate;

    public HandshakeResult(String clientId, Long createDate) {
        this.clientId = clientId;
    }
}
