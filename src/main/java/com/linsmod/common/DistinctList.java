package com.linsmod.common;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

public class DistinctList<T> extends LinqList<T> {
    private Function<T, Object> distinctBy;
    private Function2<T, T, Boolean> equalsBy;

    private Function2<T, T, Boolean> eqStub;

    /**
     * DistinctList.
     * <p></p>
     * Primitive types can use non-arguments constructor and got ability to distinct elements when
     * use methods like add(), addAll(), contains().
     * <p></p>
     * If no comparing method provided. Object.equals is used.
     */
    public DistinctList() {
        this.eqStub = this::applyEqCompareAndUpdateStub;
    }

    public DistinctList(Function<T, Object> distinctBy) {
        this.distinctBy = distinctBy;
    }

    public DistinctList(Function2<T, T, Boolean> equalsBy) {
        this.equalsBy = equalsBy;
    }

    /**
     * distinctBy function.
     * Please provide distinctBy by constructor or override distinctBy()
     *
     * @return
     */
    protected Function<T, Object> distinctBy() {
        return distinctBy;
    }

    /**
     * equalsBy function.
     * Please provide equalsBy by constructor or override equalsBy()
     *
     * @return
     */
    protected Function2<T, T, Boolean> equalsBy() {
        return equalsBy;
    }


    @Override
    public boolean add(T t) {
        if (t == null)
            throw new RuntimeException("DistinctList.add: null not allowed");
        if (!this.any(x -> applyEqCompareAndUpdateStub(x, t))) {
            return super.add(t);
        }
        return false;
    }

    private boolean applyEqCompareAndUpdateStub(T obj, T obj2) {

        // when user provides none compare functions
        // Using Objects.equals to compare if inputs is primitive type.
        // Otherwise, reject to compare.
        if (equalsBy() == null && distinctBy() == null) {
            if (simpleTyped(obj) || simpleTyped(obj2)) {

                // update stub to use efficient method (skip this check)
                this.eqStub = this::objectEquals;
                return obj.equals(obj2);
            } else {
                return obj.equals(obj2);
//                throw new RuntimeException("DistinctList: Neither equalsBy nor distinctBy is implemented or provided.");
            }
        } else if (equalsBy() != null) {
            this.eqStub = equalsBy();
            return equalsBy().apply(obj, obj2);
        } else {
            return distinctBy().apply(obj).equals(distinctBy().apply(obj2));
        }
    }

    private boolean objectEquals(T obj, T obj2) {
        return Objects.equals(obj, obj2);
    }

    private boolean simpleTyped(Object obj) {
        return obj != null && isSimpleType(obj.getClass());
    }

    private boolean isSimpleType(Class<?> t) {
        return t.isPrimitive()
                || t == Integer.class
                || t == String.class
                || t == Long.class
                || t == Double.class
                || t == Byte.class
                || t == Date.class
                || t == Boolean.class;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            this.add(t);
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return applyEqCompareAndUpdateStub((T) this, (T) o);
    }

    @Override
    public T set(int index, T element) {
        if (element == null) {
            throw new RuntimeException("DistinctList.set: Set null element is not allowed!");
        }
        return super.set(index, element);
    }
}
