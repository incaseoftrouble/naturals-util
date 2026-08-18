// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals;

public final class BitUtil {
    private BitUtil() {}

    public static long mask(int from, int to) {
        assert 0 <= from && from <= to && to <= Long.SIZE;
        if (from == to) {
            return 0L;
        }
        if (to == Long.SIZE) {
            return -(1L << from); // = ~((1L << from) - 1L)
        }

        return (1L << to) - 1L & -(1L << from);
    }

    public static long maskTo(int to) {
        assert 0 <= to && to <= Long.SIZE;
        return to == Long.SIZE ? -1L : (1L << to) - 1L;
    }

    public static int nextBit(long store, int index) {
        long shifted = store >>> (index - 1);
        int num = Long.numberOfTrailingZeros(shifted);
        if (num == Long.SIZE) {
            return -1;
        }
        assert num < Long.SIZE - index;
        return num + index + 1;
    }
}
