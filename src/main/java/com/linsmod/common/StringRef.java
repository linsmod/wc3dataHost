package com.linsmod.common;

public class StringRef {
    private final String str;

    public StringRef(String str) {
        this.str = str;
    }

    public String str() {
        return this.str;
    }

    public int length() {
        return str.length();
    }

    public String substring(int i, int i1) {
        return str.substring(i, i1);
    }
}
