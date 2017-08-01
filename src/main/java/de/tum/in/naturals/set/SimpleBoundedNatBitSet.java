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
import java.util.NoSuchElementException;
import javax.annotation.Nonnegative;

class SimpleBoundedNatBitSet extends AbstractBoundedNatBitSet {
  private final BitSet bitSet;
  private final boolean complement;
  private final SimpleBoundedNatBitSet complementView;

  private SimpleBoundedNatBitSet(SimpleBoundedNatBitSet other) {
    super(other.domainSize());
    // Complement constructor
    this.bitSet = other.bitSet;
    this.complement = !other.complement;
    this.complementView = other;
  }

  SimpleBoundedNatBitSet(BitSet bitSet, @Nonnegative int domainSize) {
    super(domainSize);
    this.bitSet = bitSet;
    this.complement = false;
    this.complementView = new SimpleBoundedNatBitSet(this);
  }

  @Override
  public void and(IntCollection ints) {
    assert checkConsistency();
    // retainAll
    if (ints instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) ints;
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
    if (ints instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) ints;
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
  public SimpleBoundedNatBitSet clone() {
    assert checkConsistency();
    return new SimpleBoundedNatBitSet((BitSet) bitSet.clone(), domainSize());
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
    if (o instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) o;
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

  BitSet getBitSet() {
    return bitSet;
  }

  @Override
  public int hashCode() {
    return (complement ? ~bitSet.hashCode() : bitSet.hashCode()) + domainSize() * 31;
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

  @Override
  public int lastInt() {
    int lastInt = complement ? bitSet.previousClearBit(domainSize() - 1) : bitSet.length() - 1;
    if (lastInt == -1) {
      throw new NoSuchElementException();
    }
    return lastInt;
  }

  @Override
  public void or(IntCollection ints) {
    assert checkConsistency();
    // addAll
    if (ints instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) ints;
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
    if (ints instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) ints;
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

  private void orWith(BitSet bitSet, boolean flip) {
    if (flip) {
      BitSet otherBitSet = (BitSet) bitSet.clone();
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
