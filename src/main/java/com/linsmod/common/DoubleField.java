package com.linsmod.common;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DoubleField {
    public final String key;
    public List<DoubleField> parts = new LinkedList<>();
    public String stringValue;
    public double value;
    public String kind = "";

    public DoubleField(String key, double v, String kind) {
        this.key = key;
        this.value = v;
        this.kind = kind;
    }

    public static DoubleField parse(String line) {
        StringField field = StringField.parse(line);
        if (field != null) {
            return field.toDoubleField();
        }
        return null;
    }

    @Override
    public String toString() {
        return "kv{" +
                "key='" + key + '\'' +
                ", parts=" + parts +
                ", v=" + value +
                ", kind='" + kind + '\'' +
                '}';
    }

}