package com.linsmod.common;

public interface FunctionE<ELEMENT, E extends Exception> {
    Boolean apply(ELEMENT e) throws E;
}
