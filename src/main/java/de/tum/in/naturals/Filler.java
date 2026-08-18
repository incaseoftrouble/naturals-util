// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals;

import de.tum.in.naturals.map.Nat2DoubleDenseArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.IntToDoubleFunction;

public final class Filler {
    private Filler() {}

    public static void fill(Int2DoubleFunction function, int from, int to, double value) {
        if (to < from) {
            throw new IllegalArgumentException();
        }
        if (from == to) {
            return;
        }
        if (function instanceof Nat2DoubleDenseArrayMap) {
            ((Nat2DoubleDenseArrayMap) function).fill(from, to, value);
        } else {
            for (int i = from; i < to; i++) {
                function.put(i, value);
            }
        }
    }

    public static void fill(Int2DoubleFunction function, PrimitiveIterator.OfInt keys, double value) {
        if (function instanceof Nat2DoubleDenseArrayMap) {
            ((Nat2DoubleDenseArrayMap) function).fill(keys, value);
        } else {
            keys.forEachRemaining((IntConsumer) i -> function.put(i, value));
        }
    }

    public static void fill(Int2DoubleFunction function, int from, int to, IntToDoubleFunction valueMap) {
        if (to < from) {
            throw new IllegalArgumentException();
        }
        if (from == to) {
            return;
        }
        for (int i = from; i < to; i++) {
            function.put(i, valueMap.applyAsDouble(i));
        }
    }

    public static void fill(Int2DoubleFunction function, PrimitiveIterator.OfInt keys, IntToDoubleFunction valueMap) {
        keys.forEachRemaining((IntConsumer) i -> function.put(i, valueMap.applyAsDouble(i)));
    }
}
