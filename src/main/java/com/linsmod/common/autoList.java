package com.linsmod.common;

import java.util.Iterator;

public class autoList<T> extends LinqList<T> {
    private Iterator<T> iterator;

    public autoList(LinqList<T> ts) {
        super(ts);
    }

    public T next() {
        if (size() == 1) {
            return get(0);
        }
        if (size() == 0) {
            return null;
        }
        if (iterator == null) {
            iterator = this.iterator();
        }
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            iterator = null;
            return next();
        }
    }
}
