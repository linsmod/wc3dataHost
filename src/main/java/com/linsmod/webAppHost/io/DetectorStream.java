package com.linsmod.webAppHost.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DetectorStream extends InputStream {
    private final InputStream source;
    private final List<Detector> detectors = new ArrayList<>();
    private final Map<String, String> replacements;
    Detector.Span current = null;
    private int pos = 0;
    private List<Detector.Span> resultList = new ArrayList<>();

    public DetectorStream(InputStream source, Map<String, String> replacements) {
        this.replacements = replacements;
        this.source = source;
        for (String s : replacements.keySet()) {
            this.detectors.add(new Detector(s));
        }
        try {
            source.mark(-1); // readlimit has no effect on AssertInputStream
            while (scanRead(true) != -1) {
            }
            pos = 0;
            source.reset();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private int scanRead(boolean scan) throws IOException {
        int b = source.read();
        if (b != -1 && scan) {
            for (Detector detector : detectors) {
                detector.onRead(b, pos, resultList);
            }
        }
        pos++;
        return b;
    }

    public int read() throws IOException {
        for (Detector.Span span : resultList) {
            if (span.startsWith(pos)) {
                if (span.stream == null) {
                    span.stream = new ByteArrayInputStream(replacements.get(span.getKey()).getBytes());
                    current = span;
                }
                break;
            }
        }
        if (current == null) {
            return scanRead(false);
        }
        int b = current.stream.read();
        if (b == -1) {
            pos += current.len;
            source.skip(current.len);
            current.stream.close();
            current.stream = null;
            current = null;
            return read();
        }
        return b;
    }

    @Override
    public long skip(long n) throws IOException {
        return super.skip(n);
    }
}
