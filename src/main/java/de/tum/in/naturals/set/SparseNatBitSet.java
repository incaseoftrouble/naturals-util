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

package de.tum.in.naturals.set;

import com.zaxxer.sparsebits.SparseBitSet;
import de.tum.in.naturals.bitset.SparseBitSets;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntConsumer;

class SparseNatBitSet extends AbstractNatBitSet {
    private final SparseBitSet bitSet;

    SparseNatBitSet(SparseBitSet bitSet) {
        this.bitSet = bitSet;
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

        if (indices instanceof SparseNatBitSet) {
            SparseNatBitSet other = (SparseNatBitSet) indices;
            if (lastInt() < other.lastInt()) {
                return false;
            }
            return SparseBitSets.isSubset(other.bitSet, bitSet);
        }
        if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
            int lastInt = lastInt();
            if (lastInt < other.lastInt()) {
                return false;
            }

            return other.isComplement()
                    ? SparseBitSets.isSubsetConsuming(other.complementBits(), bitSet)
                    : SparseBitSets.isSubset(other.getBitSet(), bitSet);
        }

        return super.containsAll(indices);
    }

    @Override
    public int firstInt() {
        int firstSet = bitSet.nextSetBit(0);
        if (firstSet == -1) {
            throw new NoSuchElementException();
        }
        return firstSet;
    }

    @Override
    public int lastInt() {
        int lastSet = bitSet.length() - 1;
        if (lastSet == -1) {
            throw new NoSuchElementException();
        }
        return lastSet;
    }

    @Override
    public int nextPresentIndex(int index) {
        return bitSet.nextSetBit(index);
    }

    @Override
    public int nextAbsentIndex(int index) {
        return bitSet.nextClearBit(index);
    }

    @Override
    public int previousPresentIndex(int index) {
        return SparseBitSets.previousPresentIndex(bitSet, index);
    }

    @Override
    public int previousAbsentIndex(int index) {
        return SparseBitSets.previousAbsentIndex(bitSet, index);
    }

    @Override
    public IntIterator iterator() {
        return SparseBitSets.iterator(bitSet);
    }

    @Override
    public void forEach(IntConsumer consumer) {
        SparseBitSets.forEach(bitSet, consumer);
    }

    @Override
    public void set(int index) {
        bitSet.set(index);
    }

    @Override
    public void set(int index, boolean value) {
        bitSet.set(index, value);
    }

    @Override
    public void set(int from, int to) {
        bitSet.set(from, to);
    }

    @Override
    public void clear() {
        bitSet.clear();
    }

    @Override
    public void clear(int index) {
        bitSet.clear(index);
    }

    @Override
    public void clear(int from, int to) {
        bitSet.clear(from, to);
    }

    @Override
    public void clearFrom(int from) {
        bitSet.clear(from, Integer.MAX_VALUE);
    }

    @Override
    public void flip(int from, int to) {
        bitSet.flip(from, to);
    }

    @Override
    public void flip(int index) {
        bitSet.flip(index);
    }

    @Override
    public boolean intersects(Collection<Integer> indices) {
        if (indices instanceof SparseNatBitSet) {
            SparseNatBitSet other = (SparseNatBitSet) indices;
            return bitSet.intersects(other.bitSet);
        }
        if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
            return bitSet.intersects(other.isComplement() ? other.complementBits() : other.getBitSet());
        }
        return super.intersects(indices);
    }

    @Override
    public void and(IntCollection indices) {
        if (indices instanceof SparseNatBitSet) {
            SparseNatBitSet other = (SparseNatBitSet) indices;
            bitSet.and(other.bitSet);
        } else if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
            bitSet.clear(other.domainSize(), Integer.MAX_VALUE);
            if (other.isComplement()) {
                bitSet.andNot(other.getBitSet());
            } else {
                bitSet.and(other.getBitSet());
            }
        } else {
            super.and(indices);
        }
    }

    @Override
    public void andNot(IntCollection indices) {
        if (isEmpty() || indices.isEmpty()) {
            return;
        }
        if (indices instanceof SparseNatBitSet) {
            SparseNatBitSet other = (SparseNatBitSet) indices;
            bitSet.andNot(other.bitSet);
        } else if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;

            int domainSize = other.domainSize();
            if (other.isComplement()) {
                bitSet.and(0, domainSize, other.getBitSet());
            } else {
                bitSet.andNot(other.getBitSet());
            }
        } else {
            super.andNot(indices);
        }
    }

    @Override
    public void or(IntCollection indices) {
        if (indices instanceof SparseNatBitSet) {
            SparseNatBitSet other = (SparseNatBitSet) indices;
            bitSet.or(other.bitSet);
        } else if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
            bitSet.or(other.isComplement() ? other.complementBits() : other.getBitSet());
        } else {
            super.or(indices);
        }
    }

    @Override
    public void xor(IntCollection indices) {
        if (indices instanceof SparseNatBitSet) {
            SparseNatBitSet other = (SparseNatBitSet) indices;
            bitSet.xor(other.bitSet);
        } else if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;

            bitSet.xor(other.getBitSet());
            if (other.isComplement()) {
                bitSet.flip(0, other.domainSize());
            }
        } else {
            super.xor(indices);
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public SparseNatBitSet clone() {
        return new SparseNatBitSet(bitSet.clone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        if (isEmpty()) {
            return ((Collection<?>) o).isEmpty();
        }
        if (((Collection<?>) o).isEmpty()) {
            return false;
        }

        if (o instanceof SparseNatBitSet) {
            SparseNatBitSet other = (SparseNatBitSet) o;
            return bitSet.equals(other.bitSet);
        }
        if (o instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) o;

            if (lastInt() >= other.domainSize()) {
                return false;
            }
            if (other.isComplement()) {
                return size() == other.size() && !other.getBitSet().intersects(bitSet);
            }
            return bitSet.equals(other.getBitSet());
        }
        return super.equals(o);
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    SparseBitSet getBitSet() {
        return bitSet;
    }
}
