// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import de.tum.in.naturals.Indices;
import de.tum.in.naturals.set.NatBitSet;
import de.tum.in.naturals.set.NatBitSets;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.BitSet;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import org.roaringbitmap.RelativeRangeConsumer;
import org.roaringbitmap.RoaringBitmap;

/**
 * Utility class to help interacting with {@link BitSet}.
 */
public final class BitSets {
    /** Runs inspected before deciding how to traverse; see {@link #forEach(BitSet, IntConsumer)}. */
    static final int SAMPLE_RUNS = 32;
    /** Bits the sample looks at, so that it costs a bounded number of word reads on a sparse set. */
    private static final int SAMPLE_SPAN = 1024;
    /** Average run length from which one bit search per run beats one word step per element. */
    static final int RUN_LENGTH_THRESHOLD = 4;

    private BitSets() {}

    public static BitSet of() {
        return new BitSet(0);
    }

    public static BitSet of(int index) {
        BitSet bitSet = new BitSet(index + 1);
        bitSet.set(index);
        return bitSet;
    }

    public static BitSet of(int... indices) {
        BitSet bitSet = new BitSet();
        for (int index : indices) {
            bitSet.set(index);
        }
        return bitSet;
    }

    public static BitSet of(IntIterable iterable) {
        if (iterable instanceof NatBitSet) {
            return NatBitSets.toBitSet((NatBitSet) iterable);
        }

        BitSet bitSet;
        if (iterable instanceof IntSortedSet) {
            IntSortedSet sortedSet = (IntSortedSet) iterable;
            if (sortedSet.comparator() == null) {
                bitSet = new BitSet(sortedSet.lastInt() + 1);
            } else {
                bitSet = new BitSet();
            }
        } else {
            bitSet = new BitSet();
        }
        iterable.forEach((IntConsumer) bitSet::set);
        return bitSet;
    }

    public static BitSet of(Iterable<Integer> iterable) {
        if (iterable instanceof IntIterable) {
            return of((IntIterable) iterable);
        }

        BitSet bitSet = new BitSet();
        for (Integer integer : iterable) {
            bitSet.set(integer);
        }
        return bitSet;
    }

    public static BitSet of(PrimitiveIterator.OfInt iterator) {
        BitSet bitSet = new BitSet();
        iterator.forEachRemaining((IntConsumer) bitSet::set);
        return bitSet;
    }

    public static BitSet of(boolean... indices) {
        BitSet bitSet = new BitSet(indices.length);
        Indices.forEach(indices, bitSet::set);
        return bitSet;
    }

    public static BitSet of(RoaringBitmap bitmap) {
        return of(bitmap, bitmap.last() + 1);
    }

    /**
     * Copies {@code [0, length)} of the given bitmap into a {@link BitSet}.
     */
    public static BitSet of(RoaringBitmap bitmap, int length) {
        BitSet bitSet = new BitSet(length);
        if (length > 0) {
            bitmap.forAllInRange(0, length, new BitSetRangeConsumer(bitSet));
        }
        return bitSet;
    }

    public static BitSet copyOf(BitSet bitset) {
        return (BitSet) bitset.clone();
    }

    public static BitSet trimmedCopy(BitSet bitSet) {
        return BitSet.valueOf(bitSet.toLongArray());
    }

    public static IntIterator complementIterator(BitSet bitSet, int length) {
        return new BitSetComplementIterator(bitSet, length);
    }

    /**
     * Feeds every set bit to the consumer, in ascending order.
     */
    public static void forEach(BitSet bitSet, IntConsumer consumer) {
        // Depending on the shape of the bitset, different strategies are (vastly) more efficient
        // In essence, its about how many runs (continuous blocks of 1s) we have.
        // We consider three cases:
        //  * Long runs: Figure out start and end, do a simple for-loop in between
        //  * Few short runs: Just go over the map element-wise
        //  * Many short runs: Use .stream() which extracts word-wise
        // Unfortunately, we do not know the shape beforehand, so we sample:
        //   Test how many runs appear in the first SAMPLE_SPAN elements

        int elements = 0;
        int runs = 0;
        int from = bitSet.nextSetBit(0);
        while (from >= 0 && from < SAMPLE_SPAN && runs < SAMPLE_RUNS) {
            int end = bitSet.nextClearBit(from);
            assert end > from;
            for (int i = from; i < end; i++) {
                consumer.accept(i);
            }
            elements += end - from;
            runs += 1;
            from = bitSet.nextSetBit(end);
        }

        // Already covered the entire map
        if (from < 0) {
            return;
        }
        if (elements < RUN_LENGTH_THRESHOLD * runs) {
            // Runs are short
            if (runs == SAMPLE_RUNS) {
                // Only the word scan cannot be resumed - it re-walks the sampled bits and drops them
                bitSet.stream().skip(elements).forEach(consumer);
            } else {
                for (int i = from; i >= 0; i = bitSet.nextSetBit(i + 1)) {
                    consumer.accept(i);
                }
            }
            return;
        }
        // Runs are long
        int currentBlock = from;
        while (currentBlock > -1) {
            int blockEnd = bitSet.nextClearBit(currentBlock);
            for (int i = currentBlock; i < blockEnd; i++) {
                consumer.accept(i);
            }
            currentBlock = bitSet.nextSetBit(blockEnd);
        }
    }

    /**
     * Whether any set bit satisfies the predicate, testing them in ascending order.
     *
     * <p>Traverses as {@link #forEach(BitSet, IntConsumer)} does and for the same reasons, with the sample
     * testing the elements it walks - so a predicate that is satisfied early is answered inside it.
     */
    public static boolean anyMatch(BitSet bitSet, IntPredicate predicate) {
        int elements = 0;
        int runs = 0;
        int from = bitSet.nextSetBit(0);
        while (from >= 0 && from < SAMPLE_SPAN && runs < SAMPLE_RUNS) {
            int end = bitSet.nextClearBit(from);
            assert end > from;
            for (int i = from; i < end; i++) {
                if (predicate.test(i)) {
                    return true;
                }
            }
            elements += end - from;
            runs += 1;
            from = bitSet.nextSetBit(end);
        }

        // Already covered the entire map
        if (from < 0) {
            return false;
        }
        if (elements < RUN_LENGTH_THRESHOLD * runs) {
            // Runs are short
            if (runs == SAMPLE_RUNS) {
                // Only the word scan cannot be resumed - it re-walks the sampled bits and drops them
                return bitSet.stream().skip(elements).anyMatch(predicate);
            }
            for (int i = from; i >= 0; i = bitSet.nextSetBit(i + 1)) {
                if (predicate.test(i)) {
                    return true;
                }
            }
            return false;
        }
        // Runs are long
        int currentBlock = from;
        while (currentBlock > -1) {
            int blockEnd = bitSet.nextClearBit(currentBlock);
            for (int i = currentBlock; i < blockEnd; i++) {
                if (predicate.test(i)) {
                    return true;
                }
            }
            currentBlock = bitSet.nextSetBit(blockEnd);
        }
        return false;
    }

    /**
     * Checks if {@code first} is a subset of {@code second}.
     */
    public static boolean isSubset(BitSet first, BitSet second) {
        // Strategy: First, trivial checks, then similar to forEach, sample what "run structure" we have and decide
        // based on that.
        if (first.isEmpty()) {
            return true;
        }
        if (!second.get(first.length() - 1)) {
            return false;
        }
        int elements = 0;
        int runs = 0;
        int gap = -1;
        int from = first.nextSetBit(0);
        while (from >= 0 && from < SAMPLE_SPAN && runs < SAMPLE_RUNS) {
            int end = first.nextClearBit(from);
            assert end > from;
            if (end - from < Long.SIZE) {
                // If the run is short, just probe second element wise.
                // If second has a long run at from, we would scan towards the end, wasting a lot of work.
                for (int i = from; i < end; i++) {
                    if (!second.get(i)) {
                        return false;
                    }
                }
            } else {
                // Check if the current or next run in second covers this run
                if (gap < from) {
                    gap = second.nextClearBit(from);
                }
                if (gap < end) {
                    return false;
                }
            }
            elements += end - from;
            runs += 1;
            from = first.nextSetBit(end);
        }
        // Tested everything
        if (from < 0) {
            return true;
        }
        if (runs == SAMPLE_RUNS) {
            // There are many short, go to andNot based bulk operation
            return isSubsetByDifference((BitSet) first.clone(), second);
        }
        if (elements < RUN_LENGTH_THRESHOLD * runs) {
            // Few short runs, just go through our set elementwise
            for (int i = from; i >= 0; i = first.nextSetBit(i + 1)) {
                if (!second.get(i)) {
                    return false;
                }
            }
            return true;
        }
        // Long runs, check run-wise -- the structure of second does not matter, the number of operations is bounded
        // by the number of runs in first.
        int currentBlock = from;
        while (currentBlock > -1) {
            int blockEnd = first.nextClearBit(currentBlock);
            if (gap < currentBlock) {
                gap = second.nextClearBit(currentBlock);
            }
            // The whole block is contained exactly if second has no gap before the block ends
            if (gap < blockEnd) {
                return false;
            }
            currentBlock = first.nextSetBit(blockEnd);
        }
        return true;
    }

    /**
     * Checks if {@code first} is a subset of {@code second}, consuming {@code first}.
     */
    public static boolean isSubsetConsuming(BitSet first, BitSet second) {
        // Try some O(1) fail-fast checks to avoid scanning the entire array
        return first.isEmpty() || second.get(first.length() - 1) && isSubsetByDifference(first, second);
    }

    private static boolean isSubsetByDifference(BitSet first, BitSet second) {
        first.andNot(second);
        return first.isEmpty();
    }

    /**
     * Whether the set has at most {@code limit} maximal blocks of consecutive set bits.
     */
    public static boolean hasAtMostRuns(BitSet bitSet, int limit) {
        if (limit < 0) {
            return false;
        }
        int remaining = limit;
        int from = bitSet.nextSetBit(0);
        while (from >= 0) {
            if (remaining == 0) {
                return false;
            }
            remaining -= 1;
            from = bitSet.nextSetBit(bitSet.nextClearBit(from));
        }
        return true;
    }

    public static boolean isDisjoint(BitSet first, BitSet second) {
        return !first.intersects(second);
    }

    public static IntIterator iterator(BitSet bitSet) {
        return new BitSetIterator(bitSet);
    }

    /**
     * Returns the set containing all subsets of the given basis.
     * <strong>Warning</strong>: For performance reasons, the iterator of this set may modify the
     * returned elements in place.
     */
    public static Set<BitSet> powerSet(BitSet basis) {
        int length = basis.length();
        if (length == basis.cardinality()) {
            return powerSet(length);
        }
        return new PowerBitSet(basis);
    }

    public static Set<BitSet> powerSet(int i) {
        return new PowerBitSetSimple(i);
    }

    private static final class BitSetRangeConsumer implements RelativeRangeConsumer {
        private final BitSet bitSet;

        BitSetRangeConsumer(BitSet bitSet) {
            this.bitSet = bitSet;
        }

        @Override
        public void acceptPresent(int relativeIndex) {
            bitSet.set(relativeIndex);
        }

        @Override
        public void acceptAbsent(int relativeIndex) {
            // Nothing to do
        }

        @Override
        public void acceptAllPresent(int relativeFrom, int relativeTo) {
            bitSet.set(relativeFrom, relativeTo);
        }

        @Override
        public void acceptAllAbsent(int relativeFrom, int relativeTo) {
            // Nothing to do
        }
    }
}
