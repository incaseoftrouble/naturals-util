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
import java.util.Set;
import java.util.function.IntConsumer;

class SparseNatBitSet extends AbstractNatBitSet {
  private final SparseBitSet bitSet;

  SparseNatBitSet(SparseBitSet bitSet) {
    this.bitSet = bitSet;
  }

  @Override
  public void and(IntCollection ints) {
    if (ints instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) ints;
      bitSet.and(other.bitSet);
    } else {
      super.and(ints);
    }
  }

  @Override
  public void andNot(IntCollection ints) {
    if (ints instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) ints;
      bitSet.andNot(other.bitSet);
    } else {
      super.andNot(ints);
    }
  }

  @Override
  public void clear(int i) {
    bitSet.clear(i);
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
  public boolean contains(int key) {
    return bitSet.get(key);
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
    if (!(o instanceof Set<?>)) {
      return false;
    }
    Set<?> set = (Set<?>) o;
    return set.size() == size() && set.containsAll(this);
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
  public boolean intersects(IntCollection ints) {
    if (ints instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) ints;
      return bitSet.intersects(other.bitSet);
    }
    return super.intersects(ints);
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
  public void or(IntCollection ints) {
    if (ints instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) ints;
      bitSet.or(other.bitSet);
    } else {
      super.or(ints);
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
  public void xor(IntCollection ints) {
    if (ints instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) ints;
      bitSet.xor(other.bitSet);
    } else {
      super.xor(ints);
    }
  }
}
