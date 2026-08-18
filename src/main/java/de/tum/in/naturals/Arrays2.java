// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Arrays2 {
    /**
     * Where {@link #binaryWindow} stops halving and scans. Binary search only starts paying off around
     * 48 entries, and a scan of this many is cheaper than the branches needed to narrow it further.
     */
    private static final int LINEAR_SEARCH_WINDOW = 32;

    private Arrays2() {}

    public static int cardinality(boolean[] array) {
        int count = 0;
        for (boolean val : array) {
            if (val) {
                count += 1;
            }
        }
        return count;
    }

    public static int sum(int[] array) {
        int sum = 0;
        for (int value : array) {
            sum += value;
        }
        return sum;
    }

    public static long sumLong(int[] array) {
        long sum = 0L;
        for (int value : array) {
            sum += value;
        }
        return sum;
    }

    public static <E> void forEach(E[] array, Consumer<? super E> action) {
        for (E element : array) {
            action.accept(element);
        }
    }

    public static <E, S extends E> E[] mapInPlace(E[] array, Function<E, S> function) {
        for (int i = 0; i < array.length; i++) {
            array[i] = function.apply(array[i]);
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    public static <E, S> S[] map(E[] array, Function<E, S> function) {
        @SuppressWarnings("SuspiciousArrayCast")
        S[] result = (S[]) new Object[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = function.apply(array[i]);
        }
        return result;
    }

    public static <E> E[] trim(E[] array, int length) {
        assert length <= array.length;
        return length < array.length ? Arrays.copyOf(array, length) : array;
    }

    public static int[] trim(int[] array, int length) {
        assert length <= array.length;
        return length < array.length ? Arrays.copyOf(array, length) : array;
    }

    public static <E> E[] ensureSize(E[] array, int length) {
        return array.length <= length ? array : Arrays.copyOf(array, length);
    }

    public static int[] ensureSize(int[] array, int length) {
        return array.length <= length ? array : Arrays.copyOf(array, length);
    }

    public static int hybridBinarySearch(int[] array, int size, int value) {
        return size <= LINEAR_SEARCH_WINDOW ? linearSearch(array, 0, size, value) : binaryWindow(array, size, value);
    }

    public static int linearSearch(int[] array, int from, int to, int value) {
        // Assumption: Array is sorted + value cannot be in array[to] or beyond
        for (int i = from; i < to; i++) {
            int current = array[i];
            if (current >= value) {
                return current == value ? i : -(i + 1);
            }
        }
        return -(to + 1);
    }

    private static int binaryWindow(int[] array, int size, int value) {
        int low = 0;
        int high = size;
        do {
            int mid = (low + high) >>> 1;
            if (array[mid] < value) {
                low = mid + 1;
            } else {
                // Everything from mid onwards is >= value, so the answer cannot be past it
                high = mid;
            }
        } while (high - low > LINEAR_SEARCH_WINDOW);
        // If high < size, we have not excluded that value actually is there
        if (high < size && array[high] == value) {
            return high;
        }
        return linearSearch(array, low, high, value);
    }
}
