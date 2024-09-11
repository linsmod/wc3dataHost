package com.linsmod.wc3;

import com.linsmod.webAppHost.mvcHost.AssetMan;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class wc3data {

    private static final int TABLE_SIZE = 1280;
    static String[] imagesFormats = ".blp|.dds|.gif|.jpg|.jpeg|.png|.tga".split("\\|");
    private static Map<Integer, Integer> cryptTable = new LinkedHashMap<>();

    static {
        int seed = 0x00100001;
        for (int i = 0; i < 256; i++) {
            for (int j = i; j < TABLE_SIZE; j += 256) {
                seed = (seed * 125 + 3) & 0x2AAAAB;
                int a = ((seed & 0xFFFF) << 16);
                seed = (seed * 125 + 3) & 0x2AAAAB;
                int b = (seed & 0xFFFF);
                cryptTable.put(j, a | b); // Assuming hashType 0 was a typo in original code, corrected to use index 0
            }
        }
    }

    public AssetMan files;

    public static ArchiveEntry loadResource(AssetMan files, String type, String matchesGroup2, int tileset) throws IOException {
        String idStrHi = matchesGroup2.substring(0, 8);
        String idStrLo = matchesGroup2.substring(8);
        long idHi = Long.parseLong(idStrHi, 16);
        long idLo = Long.parseLong(idStrLo, 16);

        ArchiveEntry res = null;

        if (tileset != 0) {
            long idHi2 = idHi;
            long idLo2 = idLo;
            long maxv = (2147483648L - tileset) + 2147483648L;

            if (idLo2 >= maxv) {
                idLo2 -= maxv;
                if (idHi2 == 4294967295L) {
                    idHi2 = 0;
                } else {
                    idHi2++;
                }
            } else {
                idLo2 += tileset;
            }

            res = loadFile(files, type, idHi2, idLo2);
        }
        if (res == null)
            res = loadFile(files, type, idHi, idLo);
        return res;
    }

    public static ArchiveEntry loadFile(AssetMan files, String type, long idHi, long idLo) throws IOException {
        String filename;
        if ("files".equals(type)) {
            filename = "files.gzx";
        } else if ("images".equals(type)) {
            int fileId = (int) (idLo & 0x7);
            filename = String.format("images%d.gzx", fileId);
        } else {
            return null;
        }
        byte[] bytes = new byte[1];
        int read = files.open(filename).read(bytes);
        if (read != 1) {
            return null;
        }
        return loadGZX(files, filename, idHi, idLo);
    }

    public static long pathHashTyped(String name, int hashType) {
        if (hashType < 0 || hashType > 2) {
            throw new IllegalArgumentException("hashType must be 0 or 1");
        }

        int seed1 = 0x7FED7FED;
        int seed2 = 0xEEEEEEEE;
        char[] bytes = name.toUpperCase().toCharArray(); // Java uses char[] for character representation

        for (char ch : bytes) {
            if ('a' <= ch && ch <= 'z') {
                ch = (char) (ch - 32); // Uppercase conversion
            } else if (ch == '/') {
                ch = '\\'; // Replace forward slash with backslash
            }
            seed1 = cryptTable.get(hashType * 256 + ch) ^ ((seed1 + seed2));
            seed2 = ((int) ch + seed1 + seed2 * 33 + 3);
        }

        return seed1;
    }

    static boolean imgExt(String name) {
        String lowerCase = name.toLowerCase();
        for (int i = 0; i < imagesFormats.length; i++) {
            return lowerCase.endsWith(imagesFormats[i]);
        }
        return false;
    }

    public static String rmExt(String name) {
        String lowerCase = name.toLowerCase();
        for (int i = 0; i < imagesFormats.length; i++) {
            String ext = imagesFormats[i];
            int i1 = lowerCase.indexOf(ext);
            if (i1 != -1) {
                return name.substring(0, i1);
            }
        }
        return name;
    }


    public static String pathHash(String name) {
        if (imgExt(name)) {
            Pattern pattern = Pattern.compile("^(.*)\\.(?:blp|dds|gif|jpg|jpeg|png|tga)$");
            Matcher matcher = pattern.matcher(name);
            if (matcher.find()) {
                name = matcher.group(1);
            }
        }

        Pattern deltaPattern = Pattern.compile("^([A-Z])\\.w3mod:(.*)$");
        Matcher deltaMatcher = deltaPattern.matcher(name);
        int delta = 0;
        if (deltaMatcher.find()) {
            delta = deltaMatcher.group(1).charAt(0) - 64;
            name = deltaMatcher.group(2);
        }

        long u0 = pathHashTyped(name, 1);
        long u1 = pathHashTyped(name, 2);

        if (delta != 0) {
            u0 += delta;
            u1 += (delta >> 32);
            u0 &= 0xFFFFFFFFL;
            u1 &= 0xFFFFFFFFL;
        } else {
            u0 &= 0xFFFFFFFFL;
            u1 &= 0xFFFFFFFFL;
        }
        return dechex8(u1) + dechex8(u0);
    }

    public static byte[] unpackGZX(AssetMan files, String path, String name) throws IOException {
        ArchiveEntry loadedData = loadGZXStr(files, path, name);
        if (loadedData == null) {
            return null;
        }
        if (loadedData.isZipped()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(loadedData.bytes());
                 GZIPInputStream gzis = new GZIPInputStream(bais)) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        } else {
            return loadedData.bytes();
        }
    }

    // Placeholder method for detecting gzip compression based on your data structure
    private static boolean isGZIPOrigin(byte[] data) {
        // Implement logic to determine if data is compressed, e.g., checking magic number for gzip
        // For simplicity, assuming first two bytes could indicate compression (real logic would differ)
        return (data.length > 1 && data[0] == (byte) 0x1f && data[1] == (byte) 0x8b);
    }

    public static String dechex8(long value) {
        value &= 0xFFFFFFFFL;
        // Convert to hexadecimal and ensure it's at least 2 characters long by padding with a leading zero if necessary
        String v = String.format("%08X", value);
        for (int i = 0; i < 8 - v.length(); i++) {
            v = '0' + v;
        }
        return v;
    }

    public static ArchiveEntry loadGZX(AssetMan files, String filename, long idHi, long idLo) throws IOException {
        try (RandomAccessFile raf = files.openExtractedRaf(filename)) {
            raf.seek(4);
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            raf.getChannel().read(buffer);
            buffer.flip();
            int count = buffer.getInt();
            long total = raf.getChannel().size();
            buffer = ByteBuffer.allocate(count * 20);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            raf.getChannel().read(buffer);
            buffer.flip();
            long left = 0;
            long right = count;
            while (right > left) {
                long mid = (left + right) / 2;
                buffer.position((int) (mid * 20));
                long id1 = buffer.getInt() & 0xFFFFFFFFL;
                long id2 = buffer.getInt() & 0xFFFFFFFFL;

                if (id2 == idLo && id1 == idHi)
                    break;
                if (id2 < idHi || (id2 == idHi && id1 < idLo)) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }

            if (left >= count) return null;
            buffer.position((int) (left * 20));
//            struct ArchiveEntry {
//                uint64 id;
//                uint32 offset;
//                uint32 size;
//                uint32 usize;
//            };
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            long id1 = buffer.getInt() & 0xFFFFFFFFL;
            long id2 = buffer.getInt() & 0xFFFFFFFFL;
            if (id1 == idLo && id2 == idHi) {
                long offset = buffer.getInt() & 0xFFFFFFFFL;
                long size = buffer.getInt() & 0xFFFFFFFFL;
                long uSize = buffer.getInt() & 0xFFFFFFFFL;
                raf.seek(offset);
                byte[] out = new byte[(int) size];
                raf.readFully(out);
//            String s = new String(out);
                return new ArchiveEntry(filename, offset, size, uSize, out);
            }
            return null;
        }
    }

    public static ArchiveEntry loadGZXStr(AssetMan files, String filename, String str) throws IOException {
        if (str.length() != 16) {
            throw new IllegalArgumentException("Input string must be 16 characters long.");
        }

        long idHi = Long.parseLong(str.substring(0, 8), 16);
        long idLo = Long.parseLong(str.substring(8), 16);

        return loadGZX(files, filename, idHi, idLo);
    }

    public static class ArchiveEntry {
        private final long uSize;
        private final byte[] out;
        String id;
        long offset;
        long size;

        public ArchiveEntry(String id, long offset, long size, long uSize, byte[] out) {
            this.id = id;
            this.offset = offset;
            this.size = size;
            this.uSize = uSize;
            this.out = out;
        }

        public String createETag() {
            return String.format("hash_%04X_%08X_%04X", id.hashCode(), offset, (int) size);
        }

        boolean isZipped() {
            return uSize > size;
        }

        public byte[] bytes() throws IOException {
            if (isZipped()) {
                try (ByteArrayInputStream bais = new ByteArrayInputStream(out);
                     GZIPInputStream gzis = new GZIPInputStream(bais)) {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = gzis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    return baos.toByteArray();
                }
            }
            return out;
        }
    }
}
