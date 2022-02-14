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

import static de.tum.in.naturals.set.NatBitSetsUtil.checkNonNegative;

import com.zaxxer.sparsebits.SparseBitSet;
import de.tum.in.naturals.bitset.SparseBitSets;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
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
    assert checkConsistency();
  }

  private SparseBoundedNatBitSet(SparseBitSet bitSet, @Nonnegative int domainSize,
      boolean complement) {
    super(domainSize);
    this.bitSet = bitSet;
    this.complement = complement;
    this.complementView = new SparseBoundedNatBitSet(this);
    assert checkConsistency();
  }

  SparseBoundedNatBitSet(SparseBitSet bitSet, @Nonnegative int domainSize) {
    this(bitSet, domainSize, false);
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
  public int size() {
    assert checkConsistency();
    int bitSetCardinality = bitSet.cardinality();
    return complement ? domainSize() - bitSetCardinality : bitSetCardinality;
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

    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
      int otherLastInt = other.lastInt();
      if (!inDomain(otherLastInt) || lastInt() < otherLastInt) {
        return false;
      }

      SparseBitSet otherSetBits = other.complement ? other.complementBits() : other.bitSet;
      SparseBitSet unsetBits = complement ? bitSet : complementBits();

      return !unsetBits.intersects(otherSetBits);
    }
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      int otherLastInt = other.lastInt();
      if (!inDomain(otherLastInt) || lastInt() < otherLastInt) {
        return false;
      }

      SparseBitSet otherSetBits = other.getBitSet();
      SparseBitSet unsetBits = complement ? bitSet : complementBits();

      return !unsetBits.intersects(otherSetBits);
    }
    return super.containsAll(indices);
  }


  @Override
  public int lastInt() {
    int lastInt = complement
        ? SparseBitSets.previousAbsentIndex(bitSet, domainSize() - 1)
        : bitSet.length() - 1;
    assert checkConsistency();
    if (lastInt == -1) {
      throw new NoSuchElementException();
    }
    assert 0 <= lastInt && lastInt < domainSize();
    return lastInt;
  }


  @Override
  public int nextPresentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
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
  public int nextAbsentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
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
  public int previousPresentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
    int clampedIndex = Math.min(index, domainSize() - 1);
    return complement
        ? SparseBitSets.previousAbsentIndex(bitSet, clampedIndex)
        : SparseBitSets.previousPresentIndex(bitSet, clampedIndex);
  }

  @Override
  public int previousAbsentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
    if (index >= domainSize()) {
      return index;
    }
    return complement
        ? SparseBitSets.previousPresentIndex(bitSet, index)
        : SparseBitSets.previousAbsentIndex(bitSet, index);
  }


  @Override
  public IntIterator iterator() {
    assert checkConsistency();
    return complement
        ? SparseBitSets.complementIterator(bitSet, domainSize())
        : SparseBitSets.iterator(bitSet);
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
  public void clear() {
    assert checkConsistency();
    if (complement) {
      bitSet.set(0, domainSize());
    } else {
      bitSet.clear();
    }
    assert checkConsistency();
  }

  @Override
  public void clear(int index) {
    assert checkConsistency();
    if (index >= domainSize()) {
      return;
    }
    bitSet.set(index, complement);
    assert checkConsistency();
  }

  @Override
  public void clear(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    int domainSize = domainSize();
    if (from >= domainSize) {
      return;
    }

    if (complement) {
      bitSet.set(from, Math.min(to, domainSize));
    } else {
      bitSet.clear(from, Math.min(to, domainSize));
    }
    assert checkConsistency();
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


  @Override
  public boolean intersects(Collection<Integer> indices) {
    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;

      return (complement ? complementBits() : bitSet).intersects(
          other.isComplement() ? other.complementBits() : other.getBitSet());
    }
    if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      return (complement ? complementBits() : bitSet).intersects(other.getBitSet());
    }
    return super.intersects(indices);
  }

  @Override
  public void and(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      clear();
    } else if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
      if (complement) {
        int domainSize = domainSize();
        int otherDomainSize = other.domainSize();
        bitSet.or(0, domainSize, other.complement ? other.bitSet : other.complementBits());

        if (otherDomainSize < domainSize) {
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
    } else if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      if (complement) {
        // TODO More efficient?
        bitSet.flip(0, domainSize());
        bitSet.and(other.getBitSet());
        bitSet.flip(0, domainSize());
      } else {
        bitSet.and(other.getBitSet());
      }
    } else {
      super.and(indices);
    }
    assert checkConsistency();
  }

  @Override
  public void andNot(IntCollection indices) {
    assert checkConsistency();
    if (isEmpty() || indices.isEmpty()) {
      return;
    }
    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;

      if (complement) {
        bitSet.or(0, domainSize(), other.complement ? other.complementBits() : other.bitSet);
      } else {
        if (other.complement) {
          bitSet.and(0, other.domainSize(), other.bitSet);
        } else {
          bitSet.andNot(other.bitSet);
        }
      }
    } else if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;

      if (complement) {
        bitSet.or(0, domainSize(), other.getBitSet());
      } else {
        bitSet.andNot(other.getBitSet());
      }
    } else {
      super.andNot(indices);
    }
    assert checkConsistency();
  }

  @Override
  public void or(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
      checkInDomain(other.lastInt());

      if (complement) {
        if (other.complement) {
          bitSet.and(0, other.domainSize(), other.bitSet);
        } else {
          bitSet.andNot(other.bitSet);
        }
      } else {
        if (other.complement) {
          bitSet.or(other.complementBits());
        } else {
          bitSet.or(other.bitSet);
        }
      }
    } else if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      checkInDomain(other.lastInt());

      if (complement) {
        bitSet.andNot(other.getBitSet());
      } else {
        bitSet.or(0, domainSize(), other.getBitSet());
      }
    } else {
      super.or(indices);
    }
    assert checkConsistency();
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
    } else if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
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
          bitSet.or(0, domainSize(), other.bitSet);
        } else {
          int minDomainSize = Math.min(domainSize, otherDomainSize);
          other.bitSet.flip(0, minDomainSize);
          bitSet.or(0, domainSize, other.bitSet);
          other.bitSet.flip(0, minDomainSize);
        }
        if (otherDomainSize < domainSize) {
          bitSet.set(otherDomainSize, domainSize);
        }
      }
    } else if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;

      if (complement) {
        bitSet.and(other.getBitSet());
      } else {
        SparseBitSet bitSet = other.getBitSet();
        bitSet.flip(0, domainSize());
        this.bitSet.or(0, domainSize(), bitSet);
        bitSet.flip(0, domainSize());
      }
    } else {
      super.orNot(indices);
    }
    assert checkConsistency();
  }

  @Override
  public void xor(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) indices;
      int otherDomainSize = other.domainSize();
      checkInDomain(otherDomainSize - 1);

      bitSet.xor(other.bitSet);
      if (other.complement) {
        bitSet.flip(0, otherDomainSize);
      }
    } else if (indices instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) indices;
      checkInDomain(other.lastInt());

      bitSet.xor(other.getBitSet());
    } else {
      super.xor(indices);
    }
    assert checkConsistency();
  }


  @SuppressWarnings("OverridableMethodCallDuringObjectConstruction")
  @Override
  public SparseBoundedNatBitSet clone() {
    assert checkConsistency();
    return new SparseBoundedNatBitSet(bitSet.clone(), domainSize(), complement);
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
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
    if (!(o instanceof Set)) {
      return false;
    }
    if (isEmpty()) {
      return ((Collection<?>) o).isEmpty();
    }
    if (((Collection<?>) o).isEmpty()) {
      return false;
    }

    if (o instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet other = (SparseBoundedNatBitSet) o;

      int domainSize = domainSize();
      int otherDomainSize = other.domainSize();

      if (complement) {
        if (other.complement) {
          if (domainSize == otherDomainSize) {
            return bitSet.equals(other.bitSet);
          }

          SparseBoundedNatBitSet smaller;
          SparseBoundedNatBitSet larger;
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
          // Avoid cloning the array - might be slower but less memory consumption
          smaller.bitSet.xor(0, smallerSize, larger.bitSet);
          boolean equals = smaller.bitSet.isEmpty();
          smaller.bitSet.xor(0, smallerSize, larger.bitSet);
          return equals;
        }
      } else if (!other.complement) {
        return bitSet.equals(other.bitSet);
      }

      // complement != otherComplement
      int complementDomainSize = complement ? domainSize : otherDomainSize;
      SparseBoundedNatBitSet nonComplementSet = complement ? other : this;
      assert !nonComplementSet.complement;

      return !bitSet.intersects(other.bitSet)
          && bitSet.cardinality() + other.bitSet.cardinality() == complementDomainSize
          && (domainSize == otherDomainSize || nonComplementSet.lastInt() < complementDomainSize);
    }
    if (o instanceof SparseNatBitSet) {
      SparseNatBitSet other = (SparseNatBitSet) o;

      if (other.lastInt() >= domainSize()) {
        return false;
      }
      if (isComplement()) {
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

  SparseBitSet complementBits() {
    SparseBitSet copy = bitSet.clone();
    copy.flip(0, domainSize());
    return copy;
  }


  private boolean checkConsistency() {
    return bitSet.length() <= domainSize();
  }

}
