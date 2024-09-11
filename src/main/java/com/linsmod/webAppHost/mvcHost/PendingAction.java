package com.linsmod.webAppHost.mvcHost;

public interface PendingAction {
    Result perform(int code) throws Exception;
}
