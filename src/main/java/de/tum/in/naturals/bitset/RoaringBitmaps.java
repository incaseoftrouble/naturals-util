// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import de.tum.in.naturals.set.NatBitSet;
import de.tum.in.naturals.set.NatBitSets;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

/**
 * Utility class to help interacting with {@link org.roaringbitmap.RoaringBitmap}.
 */
public final class RoaringBitmaps {
    private RoaringBitmaps() {}

    public static RoaringBitmap of(int... ints) {
        return RoaringBitmap.bitmapOfUnordered(ints);
    }

    public static RoaringBitmap of(IntIterable iterable) {
        if (iterable instanceof NatBitSet) {
            return NatBitSets.toRoaringBitmap((NatBitSet) iterable);
        }

        RoaringBitmap bitmap = new RoaringBitmap();
        iterable.forEach((IntConsumer) bitmap::add);
        return bitmap;
    }

    public static RoaringBitmap of(Iterable<Integer> iterable) {
        if (iterable instanceof IntIterable) {
            return of((IntIterable) iterable);
        }

        RoaringBitmap bitmap = new RoaringBitmap();
        for (Integer integer : iterable) {
            bitmap.add(integer);
        }
        return bitmap;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static RoaringBitmap of(PrimitiveIterator.OfInt iterator) {
        RoaringBitmap bitmap = new RoaringBitmap();
        iterator.forEachRemaining((IntConsumer) bitmap::add);
        return bitmap;
    }

    public static RoaringBitmap of(BitSet bitSet) {
        RoaringBitmap bitmap = new RoaringBitmap();
        add(bitmap, bitSet);
        return bitmap;
    }

    public static void add(RoaringBitmap bitmap, BitSet bitSet) {
        int from = bitSet.nextSetBit(0);
        while (from >= 0) {
            int to = bitSet.nextClearBit(from);
            if (to == from + 1) {
                bitmap.add(from);
            } else {
                bitmap.add(from, (long) to);
            }
            from = bitSet.nextSetBit(to);
        }
    }

    public static IntIterator iterator(RoaringBitmap bitmap) {
        return new RoaringIterator(bitmap.getIntIterator());
    }

    public static RoaringBitmap subset(RoaringBitmap bitmap, long from, long to) {
        if (bitmap.isEmpty()) {
            return new RoaringBitmap();
        }
        RoaringBitmap selector = new RoaringBitmap();
        selector.add(from, to);
        selector.and(bitmap);
        return selector;
    }
}
