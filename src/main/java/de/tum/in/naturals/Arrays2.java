/*
 * Copyright (C) 2018 Tobias Meggendorfer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tum.in.naturals;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Arrays2 {
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
}
