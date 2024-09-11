package com.linsmod.common;

public interface Action4<ELEMENT, INDEX> {
    void apply(ELEMENT e, INDEX i, boolean first, boolean last);
}
