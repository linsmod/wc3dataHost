package com.linsmod.common;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringField {
    public final String key;
    public List<StringField> parts = new LinkedList<>();
    public String value;
    public String kind;

    public StringField(String key, String v, String kind) {
        this.key = key;
        this.value = v;
        this.kind = kind;
    }

    public static StringField parse(String line) {

        // Use a regular expression to match the key-value pairs
        Pattern pattern = Pattern.compile("(.*?)[ ：:]+(.*)?");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String key = matcher.group(1).trim();
            if (matcher.groupCount() == 2) {
                String value = matcher.group(2).trim();
                String[] split = value.split("\\+");
                StringField field = new StringField(key, split[0], null);
                for (int i = 1; i < split.length; i++) {
                    String s = split[i];
                    if (s.matches("(\\d+(\\.\\d+)?)")) {
                        field.parts.add(new StringField(null, s, null));
                    } else {
                        field.parts.add(parse(s));
                    }
                }
                return field;
            } else {
                return new StringField(key, null, null);
            }
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

    public DoubleField toDoubleField() {
        if (!Strings.isNullOrEmpty(value) && value.matches("(\\d+(\\.\\d+)?)")) {
            DoubleField doubleField = new DoubleField(key, Double.parseDouble(value), kind);
            parts.forEach(x -> {
                doubleField.parts.add(x.toDoubleField());
            });
            return doubleField;
        }
        return null;
    }
}