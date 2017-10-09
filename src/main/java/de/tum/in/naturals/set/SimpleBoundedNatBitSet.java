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
import it.unimi.dsi.fastutil.HashCommon;
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
    assert checkConsistency();
  }

  private SimpleBoundedNatBitSet(BitSet bitSet, @Nonnegative int domainSize, boolean complement) {
    super(domainSize);
    this.bitSet = bitSet;
    this.complement = complement;
    this.complementView = new SimpleBoundedNatBitSet(this);
    assert checkConsistency();
  }

  SimpleBoundedNatBitSet(BitSet bitSet, @Nonnegative int domainSize) {
    this(bitSet, domainSize, false);
  }

  @Override
  public void and(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      clear();
    } else if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      if (complement) {
        bitSet.or(other.complement ? other.bitSet : other.complementBits());

        int domainSize = domainSize();
        int otherDomainSize = other.domainSize();
        if (domainSize < otherDomainSize) {
          bitSet.clear(domainSize, otherDomainSize);
        } else {
          bitSet.set(otherDomainSize, domainSize);
        }
      } else {
        if (other.complement) {
          bitSet.andNot(other.bitSet);

          int domainSize = domainSize();
          int otherDomainSize = other.domainSize();
          if (otherDomainSize < domainSize) {
            bitSet.clear(otherDomainSize, domainSize);
          }
        } else {
          bitSet.and(other.bitSet);
        }
      }
    } else {
      super.and(indices);
    }
    assert checkConsistency();
  }

  @Override
  public void andNot(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;

      if (complement) {
        bitSet.or(other.complement ? other.complementBits() : other.bitSet);

        int domainSize = domainSize();
        int otherDomainSize = other.domainSize();
        if (domainSize < otherDomainSize) {
          bitSet.clear(domainSize, otherDomainSize);
        }
      } else {
        if (other.complement) {
          int domainSize = domainSize();
          int otherDomainSize = other.domainSize();
          if (otherDomainSize < domainSize) {
            BitSet clone = (BitSet) other.bitSet.clone();
            clone.set(otherDomainSize, domainSize);
            bitSet.and(clone);
          } else {
            bitSet.and(other.bitSet);
          }
        } else {
          bitSet.andNot(other.bitSet);
        }
      }
    } else {
      super.andNot(indices);
    }
    assert checkConsistency();
  }

  private boolean checkConsistency() {
    return bitSet.length() <= domainSize();
  }

  @Override
  public void clear(int index) {
    assert checkConsistency();
    checkInDomain(index);
    bitSet.set(index, complement);
    assert checkConsistency();
  }

  @Override
  public void clear(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    if (complement) {
      bitSet.set(from, to);
    } else {
      bitSet.clear(from, to);
    }
    assert checkConsistency();
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public SimpleBoundedNatBitSet clone() {
    assert checkConsistency();
    return new SimpleBoundedNatBitSet((BitSet) bitSet.clone(), domainSize(), complement);
  }

  @Override
  public SimpleBoundedNatBitSet complement() {
    return complementView;
  }

  private BitSet complementBits() {
    BitSet copy = (BitSet) bitSet.clone();
    copy.flip(0, domainSize());
    return copy;
  }

  @Override
  public boolean contains(int k) {
    return 0 <= k && (complement ? k < domainSize() && !bitSet.get(k) : bitSet.get(k));
  }

  @Override
  public boolean containsAll(IntCollection indices) {
    assert checkConsistency();
    if (isEmpty()) {
      return indices.isEmpty();
    }
    if (indices.isEmpty()) {
      return true;
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      int otherLastInt = other.lastInt();
      if (!inDomain(otherLastInt) || lastInt() < otherLastInt) {
        return false;
      }

      BitSet otherSetBits = other.complement ? other.complementBits() : other.bitSet;
      BitSet unsetBits = complement ? bitSet : complementBits();

      return !unsetBits.intersects(otherSetBits);
    }
    return super.containsAll(indices);
  }

  @Override
  public boolean equals(Object o) {
    assert checkConsistency();
    if (this == o) {
      return true;
    }
    if (o instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) o;
      if (isEmpty()) {
        return other.isEmpty();
      }

      int domainSize = domainSize();
      int otherDomainSize = other.domainSize();

      if (complement) {
        if (other.complement) {
          if (domainSize == otherDomainSize) {
            return bitSet.equals(other.bitSet);
          }
          SimpleBoundedNatBitSet smaller;
          SimpleBoundedNatBitSet larger;
          int smallerSize;
          int largerSize;
          if (domainSize < otherDomainSize) {
            smaller = this;
            larger = other;
            smallerSize = domainSize;
            largerSize = otherDomainSize;
          } else {
            smaller = other;
            larger = this;
            smallerSize = otherDomainSize;
            largerSize = domainSize;
          }

          if (larger.bitSet.nextClearBit(smallerSize) < largerSize) {
            return false;
          }
          BitSet largerBitSet = (BitSet) larger.bitSet.clone();
          largerBitSet.clear(smallerSize, largerSize);
          return smaller.bitSet.equals(largerBitSet);
        }
      } else if (!other.complement) {
        return bitSet.equals(other.bitSet);
      }

      // complement != otherComplement
      int complementDomainSize = complement ? domainSize : otherDomainSize;
      SimpleBoundedNatBitSet nonComplementSet = complement ? other : this;
      assert !nonComplementSet.complement;

      return !bitSet.intersects(other.bitSet)
          && bitSet.cardinality() + other.bitSet.cardinality() == complementDomainSize
          && (domainSize == otherDomainSize || nonComplementSet.lastInt() < complementDomainSize);
    }
    return super.equals(o);
  }

  @Override
  public void flip(int index) {
    assert checkConsistency();
    checkInDomain(index);
    bitSet.flip(index);
    assert checkConsistency();
  }

  @Override
  public void flip(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    bitSet.flip(from, to);
    assert checkConsistency();
  }

  BitSet getBitSet() {
    return bitSet;
  }

  @Override
  public int hashCode() {
    return (complement ? ~bitSet.hashCode() : bitSet.hashCode()) ^ HashCommon.mix(domainSize());
  }

  @Override
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
    assert lastInt < domainSize();
    return lastInt;
  }

  @Override
  public int nextAbsentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    if (index >= domainSize()) {
      return index;
    }
    if (complement) {
      int nextSet = bitSet.nextSetBit(index);
      return nextSet == -1 ? domainSize() : nextSet;
    }
    return bitSet.nextClearBit(index);
  }

  @Override
  public int nextPresentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    if (index >= domainSize()) {
      return -1;
    }
    if (complement) {
      int nextClear = bitSet.nextClearBit(index);
      return nextClear >= domainSize() ? -1 : nextClear;
    }
    return bitSet.nextSetBit(index);
  }

  @Override
  public void or(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      checkInDomain(other.lastInt());

      if (complement) {
        if (other.complement) {
          int otherDomainSize = other.domainSize();
          int domainSize = domainSize();

          BitSet otherBitSet;
          if (otherDomainSize <= domainSize) {
            otherBitSet = (BitSet) other.bitSet.clone();
            otherBitSet.set(otherDomainSize, domainSize);
          } else {
            otherBitSet = other.bitSet;
          }

          bitSet.and(otherBitSet);
        } else {
          bitSet.andNot(other.bitSet);
        }
      } else {
        if (other.complement) {
          this.bitSet.or(other.complementBits());
        } else {
          this.bitSet.or(other.bitSet);
        }
      }
    } else {
      super.or(indices);
    }
    assert checkConsistency();
  }

  @Override
  public int previousAbsentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    if (index >= domainSize()) {
      return index;
    }
    return complement ? bitSet.previousSetBit(index) : bitSet.previousClearBit(index);
  }

  @Override
  public int previousPresentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    int clampedIndex = Math.min(index, domainSize() - 1);
    return complement
        ? bitSet.previousClearBit(clampedIndex)
        : bitSet.previousSetBit(clampedIndex);
  }

  @Override
  public void orNot(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      if (complement) {
        bitSet.clear();
      } else {
        bitSet.set(0, domainSize());
      }
    } else if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      int domainSize = domainSize();
      int otherDomainSize = other.domainSize();

      if (complement) {
        if (other.complement) {
          bitSet.andNot(other.bitSet);
          if (otherDomainSize < domainSize) {
            bitSet.clear(otherDomainSize, domainSize);
          }
        } else {
          bitSet.and(other.bitSet);
        }
      } else {
        if (other.complement) {
          bitSet.or(other.bitSet);
          if (otherDomainSize < domainSize) {
            bitSet.set(otherDomainSize, domainSize);
          } else {
            bitSet.clear(domainSize, otherDomainSize);
          }
        } else {
          int minDomainSize = Math.min(domainSize, otherDomainSize);
          other.bitSet.flip(0, minDomainSize);
          bitSet.or(other.bitSet);
          other.bitSet.flip(0, minDomainSize);

          if (otherDomainSize < domainSize) {
            bitSet.set(otherDomainSize, domainSize);
          } else {
            bitSet.clear(domainSize, otherDomainSize);
          }
        }
      }
    } else {
      super.orNot(indices);
    }
    assert checkConsistency();
  }

  @Override
  public void set(int index) {
    assert checkConsistency();
    checkInDomain(index);
    bitSet.set(index, !complement);
    assert checkConsistency();
  }

  @Override
  public void set(int index, boolean value) {
    assert checkConsistency();
    checkInDomain(index);
    bitSet.set(index, value ^ complement);
    assert checkConsistency();
  }

  @Override
  public void set(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
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

  @Override
  public void xor(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet other = (SimpleBoundedNatBitSet) indices;
      int otherDomainSize = other.domainSize();

      checkInDomain(otherDomainSize - 1);
      bitSet.xor(other.bitSet);
      if (other.complement) {
        bitSet.flip(0, otherDomainSize);
      }
    } else {
      super.xor(indices);
    }
    assert checkConsistency();
  }
}
