package com.linsmod.common;

public class Pad {
    public static String L(Object o, int n) {
        String input = o.toString();
        int pad = n - input.length();
        if (pad > 0) {
            for (int i = 0; i < pad; i++) {
                input = ' ' + input;
            }
        }
        return input;
    }

    public static String R(Object o, int n) {
        String input = o == null ? "<null>" : o.toString();
        int pad = n - input.length();
        if (pad > 0) {
            for (int i = 0; i < pad; i++) {
                input = input + ' ';
            }
        }
        return input;
    }

    public static String L10(Object o) {
        String input = o.toString();
        int pad = 10 - input.length();
        if (pad > 0) {
            for (int i = 0; i < pad; i++) {
                input = "　" + input;
            }
        }
        return input;
    }

    public static String FR(String input, int n) {
        int pad = n - input.length();
        if (pad > 0) {
            for (int i = 0; i < pad; i++) {
                input = input + "　"; //全角空格
            }
        }
        return input;
    }

    public static String R10(Object v) {
        return R(v, 10);
    }

    public static String LR10(Object v) {
        return LR(v, 10);
    }

    public static String LR(Object v, int n) {
        return L(R(v, n), n * 2);
    }

    public static String R3(Object s) {
        return R(s, 3);
    }

    public static String R4(Object s) {
        return R(s, 4);
    }

    public static String R5(Object s) {
        return R(s, 5);
    }

    public static String R6(Object s) {
        return R(s, 6);
    }

    public static String R7(Object s) {
        return R(s, 7);
    }
}
