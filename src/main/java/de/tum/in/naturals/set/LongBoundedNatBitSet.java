// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static de.tum.in.naturals.BitUtil.mask;
import static de.tum.in.naturals.BitUtil.maskTo;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkNonNegative;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkOrdered;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkRange;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;

/**
 * A bounded set over a domain of at most {@link Long#SIZE} values, held in a single word.
 */
class LongBoundedNatBitSet extends AbstractBoundedNatBitSet {
    private final long domainMask;
    private long store;

    LongBoundedNatBitSet(long store, @Nonnegative int domainSize) {
        super(domainSize);
        if (Long.SIZE < domainSize) {
            throw new IllegalArgumentException();
        }
        this.domainMask = maskTo(domainSize);
        this.store = store;
        assert checkConsistency();
    }

    LongBoundedNatBitSet(@Nonnegative int domainSize) {
        this(0L, domainSize);
    }

    public static int maximalSize() {
        return Long.SIZE;
    }

    private void checkWordInDomain(long word) {
        long excess = word & ~domainMask;
        if (excess != 0L) {
            checkInDomain(Long.numberOfTrailingZeros(excess));
        }
    }

    private boolean containsIndex(int index) {
        return (store & (1L << index)) != 0L;
    }

    @Override
    public boolean isEmpty() {
        return store == 0L;
    }

    @Override
    public int size() {
        return Long.bitCount(store);
    }

    @Override
    public boolean contains(int index) {
        return inDomain(index) && containsIndex(index);
    }

    @Override
    public boolean containsAll(IntCollection indices) {
        if (NatBitSetsUtil.isSingleWord(indices)) {
            return (~store & NatBitSetsUtil.word(indices)) == 0L;
        }
        if (indices.isEmpty()) {
            return true;
        }
        if (isEmpty() || NatBitSetsUtil.lastOf(indices) >= domainSize()) {
            return false;
        }
        return super.containsAll(indices);
    }

    @Override
    public int firstInt() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return Long.numberOfTrailingZeros(store);
    }

    @Override
    public int lastInt() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return Long.SIZE - Long.numberOfLeadingZeros(store) - 1;
    }

    @Override
    public int nextPresentIndex(int index) {
        checkNonNegative(index);
        if (index >= domainSize()) {
            return -1;
        }
        long masked = store & ~maskTo(index);
        return masked == 0L ? -1 : Long.numberOfTrailingZeros(masked);
    }

    @Override
    public int nextAbsentIndex(int index) {
        checkNonNegative(index);
        if (index >= domainSize()) {
            return index;
        }
        long masked = ~store & domainMask & ~maskTo(index);
        return masked == 0L ? domainSize() : Long.numberOfTrailingZeros(masked);
    }

    @Override
    public int previousPresentIndex(int index) {
        checkNonNegative(index);
        long masked = store & maskTo(Math.min(index, domainSize() - 1) + 1);
        return masked == 0L ? -1 : Long.SIZE - Long.numberOfLeadingZeros(masked) - 1;
    }

    @Override
    public int previousAbsentIndex(int index) {
        checkNonNegative(index);
        if (index >= domainSize()) {
            return index;
        }
        long masked = ~store & domainMask & maskTo(index + 1);
        return masked == 0L ? -1 : Long.SIZE - Long.numberOfLeadingZeros(masked) - 1;
    }

    @Override
    public IntIterator iterator() {
        return new WordIterator();
    }

    @Override
    public void forEach(IntConsumer consumer) {
        long remaining = store;
        while (remaining != 0L) {
            consumer.accept(Long.numberOfTrailingZeros(remaining));
            remaining &= remaining - 1;
        }
    }

    @Override
    public void set(int index) {
        checkInDomain(index);
        store |= 1L << index;
    }

    @Override
    public void set(int index, boolean value) {
        if (value) {
            set(index);
        } else {
            clear(index);
        }
    }

    @Override
    public void set(int from, int to) {
        checkRange(from, to);
        if (from == to) {
            return;
        }
        checkInDomain(from, to);
        store |= mask(from, to);
    }

    @Override
    public void clear() {
        store = 0L;
    }

    @Override
    public void clear(int index) {
        if (inDomain(index)) {
            store &= ~(1L << index);
        }
    }

    @Override
    public void clear(int from, int to) {
        checkOrdered(from, to);
        int start = Math.max(0, from);
        if (start < domainSize()) {
            store &= ~mask(start, Math.min(to, domainSize()));
        }
    }

    @Override
    public void flip(int index) {
        checkInDomain(index);
        store ^= 1L << index;
    }

    @Override
    public void flip(int from, int to) {
        checkRange(from, to);
        if (from == to) {
            return;
        }
        checkInDomain(from, to);
        store ^= mask(from, to);
    }

    @Override
    public boolean intersects(Collection<Integer> indices) {
        return NatBitSetsUtil.isSingleWord(indices)
                ? (store & NatBitSetsUtil.word(indices)) != 0L
                : super.intersects(indices);
    }

    @Override
    public void and(IntCollection indices) {
        if (NatBitSetsUtil.isSingleWord(indices)) {
            store &= NatBitSetsUtil.word(indices);
            return;
        }
        if (isEmpty()) {
            return;
        }
        if (indices.isEmpty()) {
            store = 0L;
            return;
        }
        long newStore = 0L;
        if (indices instanceof IntSet && indices.size() > Long.SIZE) {
            IntIterator iterator = intIterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (indices.contains(index)) {
                    newStore |= 1L << index;
                }
            }
        } else {
            IntIterator iterator = indices.intIterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (contains(index)) {
                    newStore |= 1L << index;
                }
            }
        }
        store = newStore;
        assert checkConsistency();
    }

    @Override
    public void andNot(IntCollection indices) {
        if (NatBitSetsUtil.isSingleWord(indices)) {
            store &= ~NatBitSetsUtil.word(indices);
            return;
        }
        if (isEmpty() || indices.isEmpty()) {
            return;
        }
        if (indices instanceof IntSet && indices.size() > Long.SIZE) {
            long newStore = 0L;
            IntIterator iterator = intIterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (!indices.contains(index)) {
                    newStore |= 1L << index;
                }
            }
            store = newStore;
            return;
        }
        long other = 0L;
        IntIterator iterator = indices.intIterator();
        while (iterator.hasNext()) {
            int index = iterator.nextInt();
            if (contains(index)) {
                other |= 1L << index;
            }
        }
        store &= ~other;
    }

    @Override
    public void or(IntCollection indices) {
        if (NatBitSetsUtil.isSingleWord(indices)) {
            long word = NatBitSetsUtil.word(indices);
            checkWordInDomain(word);
            store |= word;
        } else {
            super.or(indices);
        }
        assert checkConsistency();
    }

    @Override
    public void orNot(IntCollection indices) {
        if (NatBitSetsUtil.isSingleWord(indices)) {
            store |= ~NatBitSetsUtil.word(indices) & domainMask;
        } else {
            long other = 0L;
            IntIterator iterator = indices.intIterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (0 <= index && index < domainSize()) {
                    other |= 1L << index;
                }
            }
            store |= ~other & domainMask;
        }
        assert checkConsistency();
    }

    @Override
    public void xor(IntCollection indices) {
        if (NatBitSetsUtil.isSingleWord(indices)) {
            long word = NatBitSetsUtil.word(indices);
            checkWordInDomain(word);
            store ^= word;
        } else {
            long other = 0L;
            IntIterator iterator = indices.intIterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                checkInDomain(index);
                other |= 1L << index;
            }
            store ^= other;
        }
        assert checkConsistency();
    }

    @Override
    public LongBoundedNatBitSet clone() {
        return (LongBoundedNatBitSet) super.clone();
    }

    @Override
    public void complement() {
        store = ~store & domainMask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        return NatBitSetsUtil.isSingleWord(o) ? store == NatBitSetsUtil.word(o) : super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    long getStore() {
        return store;
    }

    private boolean checkConsistency() {
        return (store & ~domainMask) == 0L;
    }

    private final class WordIterator implements IntIterator {
        private long remaining = store;
        private int last = -1;

        @Override
        public boolean hasNext() {
            return remaining != 0L;
        }

        @Override
        public int nextInt() {
            if (remaining == 0L) {
                throw new NoSuchElementException();
            }
            int index = Long.numberOfTrailingZeros(remaining);
            remaining &= remaining - 1;
            last = index;
            return index;
        }

        @Override
        public void remove() {
            if (last == -1) {
                throw new IllegalStateException();
            }
            store &= ~(1L << last);
            last = -1;
        }
    }
}
