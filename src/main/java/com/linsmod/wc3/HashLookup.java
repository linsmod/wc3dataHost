package com.linsmod.wc3;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class HashLookup implements Serializable {
    // different types of hashes to make with HashString
    public static final int MPQ_HASH_TABLE_OFFSET = 0;
    public static final int MPQ_HASH_NAME_A = 1;
    public static final int MPQ_HASH_NAME_B = 2;
    public static final int MPQ_HASH_FILE_KEY = 3;
    private static final long serialVersionUID = -731458056988218435L;
    private static final int[] CRYPT_TABLE = new int[0x500];

    static {
        int seed = 0x00100001;

        for (int index1 = 0; index1 < 0x100; index1++) {
            for (int index2 = index1, i = 0; i < 5; i++, index2 += 0x100) {
                seed = ((seed * 125) + 3) % 0x2AAAAB;
                final int temp1 = (seed & 0xFFFF) << 0x10;

                seed = ((seed * 125) + 3) % 0x2AAAAB;
                final int temp2 = (seed & 0xFFFF);

                CRYPT_TABLE[index2] = (temp1 | temp2);
            }
        }
    }

    public final byte[] lookup;
    public final long hash;
    public final int index;
    public final String hash_str;
    private final int hash_name_a;
    private final int hash_name_b;

    public HashLookup(String path) {
        // *** convert string to 8 bit ascii
        byte[] raw = stringToHashable(path);

        hash_name_a = HashString(raw, MPQ_HASH_NAME_A);
        hash_name_b = HashString(raw, MPQ_HASH_NAME_B);
        hash_str = String.format("%08x", hash_name_b) + String.format("%08x", hash_name_a);
        // *** generate hashtable lookup arguments
        hash = HashString(raw, MPQ_HASH_NAME_A) & 0xFFFFFFFFL | (long) HashString(raw, MPQ_HASH_NAME_B) << 32;
        index = HashString(raw, MPQ_HASH_TABLE_OFFSET);

        // *** find file name
        int index = 0;
        for (int i = raw.length; --i >= 0; ) {
            if (raw[i] == (byte) '\\' || raw[i] == (byte) '/') {
                index = i + 1;
                break;
            }
        }

        // *** save raw ascii file name in-case file is encrypted
        lookup = new byte[raw.length - index];
        System.arraycopy(raw, index, lookup, 0, lookup.length);
    }

    public static int HashString(final String in, final int HashType) {
        return HashString(stringToHashable(in), HashType);
    }

    // Based on code from StormLib.
    public static int HashString(final byte[] in, final int HashType) {
        int seed1 = 0x7FED7FED;
        int seed2 = 0xEEEEEEEE;
        for (final byte ch : in) {
            seed1 = CRYPT_TABLE[(HashType * 0x100) + ch] ^ (seed1 + seed2);
            seed2 = ch + seed1 + seed2 + (seed2 << 5) + 3;
        }
        return seed1;
    }

    public static byte[] stringToHashable(final String in) {
        return in.toUpperCase(Locale.US).replace("/", "\\").getBytes(StandardCharsets.UTF_8); // UTF_8 defined for platform independence
    }
}
