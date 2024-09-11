package com.linsmod.webAppHost.io;

import java.io.ByteArrayInputStream;
import java.util.List;

public class Detector {
    private final String str;
    byte[] bytes;
    private int pos;
    private int bytesEqual;
    private int srcStartPos;

    public Detector(String str) {
        this.str = str;
        this.bytes = str.getBytes();
    }

    public void onRead(int b, int srcPos, List<Span> resultList) {
        if (pos == 0 && b == this.bytes[0]) {
            this.bytesEqual++;
            this.srcStartPos = srcPos;
            this.pos = 1;
        } else if (b == this.bytes[pos++]) {
            this.bytesEqual++;
        } else {
            pos = 0;
            bytesEqual = 0;
            srcStartPos = 0;
        }
        if (this.bytesEqual == bytes.length) {
            Span span = new Span();
            span.pos = this.srcStartPos;
            span.len = bytes.length;
            span.ref = this;
            resultList.add(span);

            pos = 0;
            bytesEqual = 0;
            srcStartPos = 0;
        }
    }

    public static class Span {
        public ByteArrayInputStream stream;
        Detector ref;
        int pos;
        int len;

        public String getKey() {
            return ref.str;
        }

        public boolean startsWith(int srcPos) {
            return srcPos == pos;
        }
    }
}
