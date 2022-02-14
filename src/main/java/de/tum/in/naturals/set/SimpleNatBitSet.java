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
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

class SimpleNatBitSet extends AbstractNatBitSet {
  private final BitSet bitSet;

  SimpleNatBitSet(BitSet bitSet) {
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

    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      return BitSets.isSubset(other.bitSet, bitSet);
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      int lastInt = lastInt();
      if (lastInt < other.lastInt()) {
        return false;
      }

      return other.isComplement()
          ? BitSets.isSubsetConsuming(other.complementBits(), bitSet)
          : BitSets.isSubset(other.getBitSet(), bitSet);
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
    return bitSet.previousSetBit(index);
  }

  @Override
  public int previousAbsentIndex(int index) {
    return bitSet.previousClearBit(index);
  }


  @Override
  public IntStream intStream() {
    return bitSet.stream();
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
  public void flip(int index) {
    bitSet.flip(index);
  }

  @Override
  public void flip(int from, int to) {
    bitSet.flip(from, to);
  }


  @Override
  public boolean intersects(Collection<Integer> indices) {
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      return bitSet.intersects(other.bitSet);
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      return bitSet.intersects(other.isComplement() ? other.complementBits() : other.getBitSet());
    }
    return super.intersects(indices);
  }

  @Override
  public void and(IntCollection indices) {
    if (indices.isEmpty()) {
      clear();
    } else if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      bitSet.and(other.bitSet);
    } else if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
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
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      bitSet.andNot(other.bitSet);
    } else if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;

      int domainSize = other.domainSize();
      if (other.isComplement()) {
        int ownSize = lastInt();
        if (ownSize < domainSize) {
          bitSet.and(other.getBitSet());
        } else {
          BitSet clone = BitSets.copyOf(bitSet);
          this.bitSet.and(other.getBitSet());
          clone.clear(0, domainSize);
          this.bitSet.or(clone);
        }
      } else {
        bitSet.andNot(other.getBitSet());
      }
    } else {
      super.andNot(indices);
    }
  }

  @Override
  public void or(IntCollection indices) {
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      bitSet.or(other.bitSet);
    } else if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      bitSet.or(other.isComplement() ? other.complementBits() : other.getBitSet());
    } else {
      super.or(indices);
    }
  }

  @Override
  public void xor(IntCollection indices) {
    if (indices instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) indices;
      bitSet.xor(other.bitSet);
    } else if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;

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
  public SimpleNatBitSet clone() {
    return new SimpleNatBitSet((BitSet) bitSet.clone());
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

    if (o instanceof SimpleNatBitSet) {
      SimpleNatBitSet other = (SimpleNatBitSet) o;
      return bitSet.equals(other.bitSet);
    }
    if (o instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) o;

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

  BitSet getBitSet() {
    return bitSet;
  }
}
