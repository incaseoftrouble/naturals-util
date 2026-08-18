// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static de.tum.in.naturals.set.NatBitSetsUtil.checkNonNegative;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkOrdered;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkRange;

import de.tum.in.naturals.bitset.BitSets;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;
import org.jspecify.annotations.Nullable;

/**
 * A bounded set backed by a {@link BitSet}. Membership is stored directly - {@link #complement()} flips the
 * contents rather than toggling a view flag, which is what frees this class of the
 * complement-times-complement case analysis it used to carry.
 */
class SimpleBoundedNatBitSet extends AbstractBoundedNatBitSet {
    private final BitSet bitSet;

    SimpleBoundedNatBitSet(BitSet bitSet, @Nonnegative int domainSize) {
        super(domainSize);
        this.bitSet = bitSet;
        assert checkConsistency();
    }

    /** The backing words of a word backed operand, or {@code null} if it has none. */
    @Nullable
    private static BitSet words(Object indices) {
        return NatBitSetsUtil.words(indices);
    }

    @Override
    public boolean isEmpty() {
        return bitSet.isEmpty();
    }

    @Override
    public int size() {
        return bitSet.cardinality();
    }

    @Override
    public boolean contains(int index) {
        return 0 <= index && bitSet.get(index);
    }

    @Override
    public boolean containsAll(IntCollection indices) {
        if (isEmpty()) {
            return indices.isEmpty();
        }
        if (indices.isEmpty()) {
            return true;
        }
        if (NatBitSetsUtil.lastOf(indices) >= domainSize()) {
            return false;
        }
        BitSet other = words(indices);
        return other == null ? super.containsAll(indices) : BitSets.isSubset(other, bitSet);
    }

    @Override
    public int firstInt() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return bitSet.nextSetBit(0);
    }

    @Override
    public int lastInt() {
        int lastInt = bitSet.length() - 1;
        if (lastInt == -1) {
            throw new NoSuchElementException();
        }
        assert lastInt < domainSize();
        return lastInt;
    }

    @Override
    public int nextPresentIndex(int index) {
        checkNonNegative(index);
        return index >= domainSize() ? -1 : bitSet.nextSetBit(index);
    }

    @Override
    public int nextAbsentIndex(int index) {
        checkNonNegative(index);
        return index >= domainSize() ? index : bitSet.nextClearBit(index);
    }

    @Override
    public int previousPresentIndex(int index) {
        checkNonNegative(index);
        return bitSet.previousSetBit(Math.min(index, domainSize() - 1));
    }

    @Override
    public int previousAbsentIndex(int index) {
        checkNonNegative(index);
        return index >= domainSize() ? index : bitSet.previousClearBit(index);
    }

    @Override
    public IntIterator iterator() {
        return BitSets.iterator(bitSet);
    }

    @Override
    public void forEach(IntConsumer consumer) {
        BitSets.forEach(bitSet, consumer);
    }

    @Override
    public void set(int index) {
        checkInDomain(index);
        bitSet.set(index);
    }

    @Override
    public void set(int index, boolean value) {
        // Only the adding half can leave the domain
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
        bitSet.set(from, to);
    }

    @Override
    public void clear() {
        bitSet.clear();
    }

    @Override
    public void clear(int index) {
        if (inDomain(index)) {
            bitSet.clear(index);
        }
    }

    @Override
    public void clear(int from, int to) {
        checkOrdered(from, to);
        int start = Math.max(0, from);
        if (start < domainSize()) {
            bitSet.clear(start, Math.min(to, domainSize()));
        }
    }

    @Override
    public void flip(int index) {
        checkInDomain(index);
        bitSet.flip(index);
    }

    @Override
    public void flip(int from, int to) {
        checkRange(from, to);
        if (from == to) {
            return;
        }
        checkInDomain(from, to);
        bitSet.flip(from, to);
    }

    @Override
    public boolean intersects(Collection<Integer> indices) {
        BitSet other = words(indices);
        return other == null ? super.intersects(indices) : bitSet.intersects(other);
    }

    @Override
    public void and(IntCollection indices) {
        if (indices.isEmpty()) {
            clear();
            return;
        }
        BitSet other = words(indices);
        if (other == null) {
            // TODO Pick iteration order
            super.and(indices);
        } else {
            bitSet.and(other);
        }
        assert checkConsistency();
    }

    @Override
    public void andNot(IntCollection indices) {
        if (isEmpty() || indices.isEmpty()) {
            return;
        }
        BitSet other = words(indices);
        if (other == null) {
            // TODO Pick iteration order
            super.andNot(indices);
        } else {
            bitSet.andNot(other);
        }
        assert checkConsistency();
    }

    @Override
    public void or(IntCollection indices) {
        if (indices.isEmpty()) {
            return;
        }
        BitSet other = words(indices);
        if (other == null) {
            // TODO Pick iteration order
            super.or(indices);
        } else {
            checkInDomain(Math.max(0, other.length() - 1));
            bitSet.or(other);
        }
        assert checkConsistency();
    }

    @Override
    public void orNot(IntCollection indices) {
        if (indices.isEmpty()) {
            bitSet.set(0, domainSize());
            return;
        }
        BitSet other = words(indices);
        if (other == null) {
            super.orNot(indices);
        } else {
            // Walk the operand's gaps rather than materialising its complement
            // TODO Run optimization?
            int domainSize = domainSize();
            for (int i = other.nextClearBit(0); i < domainSize; i = other.nextClearBit(i + 1)) {
                bitSet.set(i);
            }
        }
        assert checkConsistency();
    }

    @Override
    public void xor(IntCollection indices) {
        if (indices.isEmpty()) {
            return;
        }
        BitSet other = words(indices);
        if (other == null) {
            super.xor(indices);
        } else {
            checkInDomain(Math.max(0, other.length() - 1));
            bitSet.xor(other);
        }
        assert checkConsistency();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public SimpleBoundedNatBitSet clone() {
        return new SimpleBoundedNatBitSet((BitSet) bitSet.clone(), domainSize());
    }

    @Override
    public void complement() {
        bitSet.flip(0, domainSize());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        BitSet other = words(o);
        return other == null ? super.equals(o) : bitSet.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    BitSet getBitSet() {
        return bitSet;
    }

    private boolean checkConsistency() {
        return bitSet.length() <= domainSize();
    }
}
