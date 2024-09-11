package com.linsmod.common;

public interface ActionE<ELEMENT, E extends Exception> {
    void apply(ELEMENT e) throws E;
}
