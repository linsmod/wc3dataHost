package com.linsmod.common;


import java.util.HashMap;

public class ListMap<T> extends HashMap<String, LinqList<T>> {
    public LinqList<T> valuesList() {
        LinqList<T> list = new LinqList<>();
        this.values().forEach(list::addAll);
        return list;
    }

    public LinqList<String> keyList() {
        return new LinqList<>(keySet());
    }

    public boolean keyMatch(String key, String text) {
        return key.equals(text);
    }

    public LinqList<T> match(String text) {
        LinqList<T> items = new LinqList<>();
        for (String s : keySet()) {
            if (keyMatch(s, text)) {
                items.addAll(get(s));
            }
        }
        return items;
    }

    @Override
    public LinqList<T> get(Object key) {
        return super.getOrDefault(key, new LinqList<>());
    }

    public void match(String text, LinqList<T> items) {
        for (String s : keySet()) {
            if (keyMatch(s, text)) {
                items.addAll(get(s));
            }
        }
    }

    public static class KeyStartsWith<T> extends ListMap<T> {
        @Override
        public boolean keyMatch(String key, String text) {
            return key.startsWith(text);
        }
    }

    public static class KeyEndsWith<T> extends ListMap<T> {
        @Override
        public boolean keyMatch(String key, String text) {
            return key.endsWith(text);
        }
    }

    public static class KeyContains<T> extends ListMap<T> {
        @Override
        public boolean keyMatch(String key, String text) {
            return key.contains(text);
        }
    }
}
