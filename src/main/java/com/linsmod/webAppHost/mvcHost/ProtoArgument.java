package com.linsmod.webAppHost.mvcHost;

import com.linsmod.common.Strings;

public class ProtoArgument {
    public String name;
    public Class<?> type;

    public ProtoArgument(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public boolean hasName() {
        return !Strings.isNullOrEmpty(name);
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getType() {
        return this.type;
    }
}
