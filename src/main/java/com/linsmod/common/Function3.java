package com.linsmod.common;

import java.util.List;

public interface Function3<LIST extends List<ELEMENT>, ELEMENT, INDEX, TResult> {

    TResult apply(LIST list, ELEMENT element, INDEX t2);
}
