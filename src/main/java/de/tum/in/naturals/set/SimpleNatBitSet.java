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
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

class SimpleNatBitSet extends AbstractNatBitSet {
  private final BitSet bitSet;

  SimpleNatBitSet(BitSet bitSet) {
    this.bitSet = bitSet;
  }

  @Override
  public void and(IntCollection indices) {
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      bitSet.and(other.bitSet);
    } else {
      super.and(indices);
    }
  }

  @Override
  public void andNot(IntCollection indices) {
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
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
  public SimpleNatBitSet clone() {
    return new SimpleNatBitSet((BitSet) bitSet.clone());
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
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      return BitSets.isSubset(other.bitSet, bitSet);
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;

      return other.isComplement()
          ? BitSets.isSubsetConsuming(other.complementBits(), bitSet)
          : BitSets.isSubset(other.getBitSet(), bitSet);
    }
    return super.containsAll(indices);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) o;
      return bitSet.equals(other.bitSet);
    }
    return super.equals(o);
  }

  @Override
  public void flip(int index) {
    bitSet.flip(index);
  }

  @Override
  public void flip(int from, int to) {
    bitSet.flip(from, to);
  }

  @Override
  public void forEach(IntConsumer consumer) {
    BitSets.forEach(bitSet, consumer);
  }

  BitSet getBitSet() {
    return bitSet;
  }

  @Override
  public int hashCode() {
    return bitSet.hashCode();
  }

  @Override
  public IntStream intStream() {
    return bitSet.stream();
  }

  @Override
  public boolean intersects(Collection<Integer> indices) {
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
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
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      bitSet.or(other.bitSet);
    } else {
      super.or(indices);
    }
  }

  @Override
  public int previousAbsentIndex(int index) {
    return bitSet.previousClearBit(index);
  }

  @Override
  public int previousPresentIndex(int index) {
    return bitSet.previousSetBit(index);
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
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      bitSet.xor(other.bitSet);
    } else {
      super.xor(indices);
    }
  }
}
