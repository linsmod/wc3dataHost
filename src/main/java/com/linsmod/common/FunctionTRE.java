package com.linsmod.common;

public interface FunctionTRE<T, TResult, E extends Exception> {
    TResult apply(T t) throws E;
}