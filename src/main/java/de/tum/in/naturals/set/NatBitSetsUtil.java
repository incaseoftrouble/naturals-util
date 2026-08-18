// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.Objects;
import java.util.Spliterator;
import org.jspecify.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

public final class NatBitSetsUtil {
    public static final int SPLITERATOR_CHARACTERISTICS =
            Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SIZED;
    static final int UNKNOWN_LAST = -2;

    private NatBitSetsUtil() {}

    @Nullable
    static BitSet words(Object indices) {
        if (indices instanceof SimpleBoundedNatBitSet) {
            return ((SimpleBoundedNatBitSet) indices).getBitSet();
        }
        if (indices instanceof HybridNatBitSet) {
            HybridNatBitSet hybrid = (HybridNatBitSet) indices;
            return hybrid.isWordBacked() ? hybrid.words() : null;
        }
        return null;
    }

    @Nullable
    static RoaringBitmap bitmap(Object indices) {
        if (indices instanceof HybridNatBitSet) {
            HybridNatBitSet hybrid = (HybridNatBitSet) indices;
            return hybrid.isBitmapBacked() ? hybrid.bitmap() : null;
        }
        return null;
    }

    static boolean isSingleWord(Object indices) {
        if (indices instanceof LongBoundedNatBitSet) {
            return true;
        }
        BitSet words = words(indices);
        return words != null && words.length() <= Long.SIZE;
    }

    static long word(Object indices) {
        if (indices instanceof LongBoundedNatBitSet) {
            return ((LongBoundedNatBitSet) indices).getStore();
        }
        long[] words = Objects.requireNonNull(words(indices)).toLongArray();
        return words.length == 0 ? 0L : words[0];
    }

    public static void checkInDomain(int domainSize, int index) {
        checkNonNegative(index);
        if (domainSize <= index) {
            throw new IndexOutOfBoundsException(
                    String.format("Index %d too large for domain [0, %d)", index, domainSize));
        }
    }

    public static void checkInDomain(int domainSize, int from, int to) {
        checkRange(from, to);
        if (domainSize < to) {
            throw new IndexOutOfBoundsException(
                    String.format("To index %d too large for domain [0, %d)", to, domainSize));
        }
    }

    public static void checkNonNegative(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException(String.format("Negative index %d ", index));
        }
    }

    public static void checkRange(int from, int to) {
        checkOrdered(from, to);
        if (from < 0) {
            throw new IndexOutOfBoundsException(String.format("Negative from index %d ", from));
        }
    }

    /**
     * Validates only that the range is not inverted. This is what the removing operations need: they clamp
     * their ends to the valid indices instead of rejecting them, so an out-of-range end is not an error -
     * an incoherent range still is.
     */
    public static void checkOrdered(int from, int to) {
        if (to < from) {
            throw new IndexOutOfBoundsException(String.format("From %d bigger than to %d", from, to));
        }
    }

    static IntCollection unbox(Collection<?> indices) {
        if (indices instanceof IntCollection) {
            return (IntCollection) indices;
        }
        IntCollection unboxed = new IntOpenHashSet(indices.size());
        for (Object index : indices) {
            if (index instanceof Integer) {
                unboxed.add((int) index);
            }
        }
        return unboxed;
    }

    static int lastOf(IntCollection operand) {
        if (operand instanceof NatBitSet) {
            return ((NatBitSet) operand).lastInt();
        }
        if (operand instanceof IntSortedSet) {
            IntSortedSet sorted = (IntSortedSet) operand;
            if (sorted.comparator() == null) {
                return sorted.lastInt();
            }
        }
        return UNKNOWN_LAST;
    }
}
