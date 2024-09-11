package com.linsmod.common;

public class NotImplemented extends RuntimeException {
    public NotImplemented(String e) {
        super("notImplemented:" + e);
    }
}
