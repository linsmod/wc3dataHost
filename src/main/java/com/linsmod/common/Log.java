package com.linsmod.common;

public class Log {
    public static void d(String d) {
        System.out.println(d);
    }

    public static void d(String category, String s) {
        System.out.println("[" + category + "] " + s);
    }
    public static void e(String message) {
        System.err.println(message);
    }
    public static void e(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
    }
}
