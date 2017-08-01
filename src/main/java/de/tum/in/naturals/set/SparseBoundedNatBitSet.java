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
import javax.annotation.Nonnegative;

class SparseBoundedNatBitSet extends AbstractBoundedNatBitSet {
  private final SparseBitSet bitSet;
  private final boolean complement;
  private final SparseBoundedNatBitSet complementView;

  private SparseBoundedNatBitSet(SparseBoundedNatBitSet other) {
    // Complement constructor
    super(other.domainSize());
    this.bitSet = other.bitSet;
    this.complement = !other.complement;
    this.complementView = other;
  }

  SparseBoundedNatBitSet(SparseBitSet bitSet, @Nonnegative int domainSize) {
    super(domainSize);
    this.bitSet = bitSet;
    this.complement = false;
    this.complementView = new SparseBoundedNatBitSet(this);
  }

  @Override
  public void and(IntCollection ints) {
    assert checkConsistency();
    // retainAll
    if (ints instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) ints;
      checkIndex(other.lastInt());
      if (complement) {
        orWith(other.bitSet, !other.complement);
      } else {
        if (other.complement) {
          bitSet.andNot(other.bitSet);
        } else {
          bitSet.and(other.bitSet);
        }
      }
    } else {
      super.and(ints);
    }
    assert checkConsistency();
  }

  @Override
  public void andNot(IntCollection ints) {
    assert checkConsistency();
    // removeAll
    if (ints instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) ints;
      checkIndex(other.lastInt());
      if (complement) {
        orWith(other.bitSet, other.complement);
      } else {
        if (other.complement) {
          bitSet.and(other.bitSet);
        } else {
          bitSet.andNot(other.bitSet);
        }
      }
    } else {
      super.andNot(ints);
    }
    assert checkConsistency();
  }

  private boolean checkConsistency() {
    return bitSet.length() <= domainSize();
  }

  @Override
  public void clear(int index) {
    assert checkConsistency();
    checkIndex(index);
    bitSet.set(index, complement);
    assert checkConsistency();
  }

  @Override
  public void clear(int from, int to) {
    assert checkConsistency();
    checkRange(from, to);
    bitSet.clear(from, to);
    assert checkConsistency();
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public SparseBoundedNatBitSet clone() {
    assert checkConsistency();
    return new SparseBoundedNatBitSet(bitSet.clone(), domainSize());
  }

  @Override
  public BoundedNatBitSet complement() {
    return complementView;
  }

  @Override
  public boolean equals(Object o) {
    assert checkConsistency();
    if (this == o) {
      return true;
    }
    if (o instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) o;
      if (domainSize() != other.domainSize()) {
        return false;
      }
      if (other.complement == this.complement) {
        return bitSet.equals(other.bitSet);
      }
      return bitSet.cardinality() + other.bitSet.cardinality() == domainSize()
          && !bitSet.intersects(other.bitSet);
    }
    return super.equals(o);
  }

  @Override
  public int firstInt() {
    assert checkConsistency();
    if (complement) {
      int firstClear = bitSet.nextClearBit(0);
      if (firstClear >= domainSize()) {
        throw new NoSuchElementException();
      }
      return firstClear;
    }
    int firstSet = bitSet.nextSetBit(0);
    if (firstSet == -1) {
      throw new NoSuchElementException();
    }
    return firstSet;
  }

  @Override
  public void flip(int index) {
    assert checkConsistency();
    checkIndex(index);
    bitSet.flip(index);
    assert checkConsistency();
  }

  @Override
  public void flip(int from, int to) {
    assert checkConsistency();
    checkRange(from, to);
    bitSet.flip(from, to);
    assert checkConsistency();
  }

  SparseBitSet getSparseBitSet() {
    return bitSet;
  }

  @Override
  public int hashCode() {
    assert checkConsistency();
    return (complement ? ~bitSet.hashCode() : bitSet.hashCode()) + super.hashCode() * 31;
  }

  boolean isComplement() {
    return complement;
  }

  @Override
  public boolean isEmpty() {
    assert checkConsistency();
    return complement ? bitSet.nextClearBit(0) == domainSize() : bitSet.isEmpty();
  }

  @Override
  public IntIterator iterator() {
    assert checkConsistency();
    return complement
        ? BitSets.complementIterator(bitSet, domainSize())
        : BitSets.iterator(bitSet, domainSize());
  }

  private int lastClearBit() {
    // Binary search for the biggest clear bit with index <= length
    int firstClearBit = bitSet.nextClearBit(0);
    int domainSize = domainSize();
    if (firstClearBit > domainSize) {
      return -1;
    }
    if (bitSet.length() < domainSize) {
      // The highest set bit in the bit set (which is at length() - 1) comes before the "length"
      // Hence, the bit at length - 1 is contained in this set
      return domainSize - 1;
    }
    int rightPivot = domainSize;
    int leftPivot = firstClearBit;

    while (true) {
      assert leftPivot <= rightPivot;
      int middle = (rightPivot + leftPivot) / 2;
      int value = bitSet.nextClearBit(middle);
      while (value > domainSize) {
        assert leftPivot <= middle && middle <= rightPivot;
        rightPivot = middle;
        middle = (rightPivot + leftPivot) / 2;
        value = bitSet.nextClearBit(middle);
      }
      leftPivot = value;
      int nextClear = bitSet.nextClearBit(leftPivot + 1);
      if (nextClear >= domainSize) {
        return leftPivot;
      }
      leftPivot = nextClear;
    }
  }

  @Override
  public int lastInt() {
    assert checkConsistency();
    int lastInt = complement ? lastClearBit() : bitSet.length() - 1;
    if (lastInt == -1) {
      throw new NoSuchElementException();
    }
    return lastInt;
  }

  @Override
  public void or(IntCollection ints) {
    assert checkConsistency();
    // addAll
    if (ints instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) ints;
      checkIndex(other.lastInt());
      if (complement) {
        if (other.complement) {
          bitSet.and(other.bitSet);
        } else {
          bitSet.andNot(other.bitSet);
        }
      } else {
        orWith(other.bitSet, other.complement);
      }
    } else {
      super.or(ints);
    }
    assert checkConsistency();
  }

  @Override
  public void orNot(IntCollection ints) {
    assert checkConsistency();
    if (ints instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) ints;
      checkIndex(other.lastInt());
      if (complement) {
        if (other.complement) {
          bitSet.andNot(other.bitSet);
        } else {
          bitSet.and(other.bitSet);
        }
      } else {
        orWith(other.bitSet, !other.complement);
      }
    } else {
      super.orNot(ints);
    }
    assert checkConsistency();
  }

  private void orWith(SparseBitSet bitSet, boolean flip) {
    if (flip) {
      SparseBitSet otherBitSet = bitSet.clone();
      otherBitSet.flip(0, domainSize());
      this.bitSet.or(otherBitSet);
    } else {
      this.bitSet.or(bitSet);
    }
  }

  @Override
  public void set(int index) {
    assert checkConsistency();
    checkIndex(index);
    bitSet.set(index, !complement);
    assert checkConsistency();
  }

  @Override
  public void set(int index, boolean value) {
    assert checkConsistency();
    checkIndex(index);
    bitSet.set(index, value ^ complement);
    assert checkConsistency();
  }

  @Override
  public void set(int from, int to) {
    assert checkConsistency();
    checkRange(from, to);
    if (complement) {
      bitSet.clear(from, to);
    } else {
      bitSet.set(from, to);
    }
    assert checkConsistency();
  }

  @Override
  public int size() {
    assert checkConsistency();
    int bitSetCardinality = bitSet.cardinality();
    return complement ? domainSize() - bitSetCardinality : bitSetCardinality;
  }
}
