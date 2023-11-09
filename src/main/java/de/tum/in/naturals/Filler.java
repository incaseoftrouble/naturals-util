/*
 * Copyright (C) 2017 Tobias Meggendorfer
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
