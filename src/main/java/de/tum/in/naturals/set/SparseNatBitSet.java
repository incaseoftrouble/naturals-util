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

import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.SparseBitSet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

class SparseNatBitSet extends AbstractNatBitSet {
  private final SparseBitSet bitSet;

  SparseNatBitSet(SparseBitSet bitSet) {
    this.bitSet = bitSet;
  }

  @Override
  public void and(IntCollection indices) {
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      bitSet.and(other.bitSet);
    } else {
      super.and(indices);
    }
  }

  @Override
  public void andNot(IntCollection indices) {
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      bitSet.andNot(other.bitSet);
    } else {
      super.andNot(indices);
    }
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
  public void clear() {
    bitSet.clear();
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public SparseNatBitSet clone() {
    return new SparseNatBitSet(bitSet.clone());
  }

  @Override
  public boolean contains(int index) {
    return 0 <= index && bitSet.get(index);
  }

  @Override
  public boolean containsAll(IntCollection indices) {
    if (indices.isEmpty()) {
      return true;
    }
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;

      SparseBitSet clone = other.bitSet.clone();
      clone.andNot(bitSet);
      return clone.isEmpty();
    }
    return super.containsAll(indices);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) o;
      return bitSet.equals(other.bitSet);
    }
    return super.equals(o);
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
  public void forEach(IntConsumer consumer) {
    BitSets.forEach(bitSet, consumer);
  }

  SparseBitSet getSparseBitSet() {
    return bitSet;
  }

  @Override
  public int hashCode() {
    return bitSet.hashCode();
  }

  @Override
  public boolean intersects(IntCollection indices) {
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      return bitSet.intersects(other.bitSet);
    }
    return super.intersects(indices);
  }

  @Override
  public boolean isEmpty() {
    return bitSet.isEmpty();
  }

  @Override
  public IntIterator iterator() {
    return BitSets.iterator(bitSet);
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
  public int nextAbsentIndex(int index) {
    return bitSet.nextClearBit(index);
  }

  @Override
  public int nextPresentIndex(int index) {
    return bitSet.nextSetBit(index);
  }

  @Override
  public void or(IntCollection indices) {
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      bitSet.or(other.bitSet);
    } else {
      super.or(indices);
    }
  }

  @Override
  public void set(int i) {
    bitSet.set(i);
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
  public int size() {
    return bitSet.cardinality();
  }

  @Override
  public void xor(IntCollection indices) {
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      bitSet.xor(other.bitSet);
    } else {
      super.xor(indices);
    }
  }
}
