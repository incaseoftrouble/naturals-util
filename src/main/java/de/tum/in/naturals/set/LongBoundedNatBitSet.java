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

import static de.tum.in.naturals.BitUtil.mask;
import static de.tum.in.naturals.BitUtil.maskTo;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collection;
import java.util.NoSuchElementException;
import javax.annotation.Nonnegative;

class LongBoundedNatBitSet extends AbstractBoundedNatBitSet {
  private final boolean complement;
  private final LongBoundedNatBitSet complementView;
  private final long domainMask;
  private final long[] store;

  private LongBoundedNatBitSet(long store, int domainSize, boolean complement) {
    super(domainSize);
    if (Long.SIZE < domainSize) {
      throw new IllegalArgumentException();
    }
    this.store = new long[] {store};
    this.complement = complement;
    domainMask = maskTo(domainSize());
    this.complementView = new LongBoundedNatBitSet(this);
    assert checkConsistency();
  }

  private LongBoundedNatBitSet(LongBoundedNatBitSet other) {
    super(other.domainSize());
    this.complementView = other;
    this.complement = !other.complement;
    this.store = other.store;
    domainMask = other.domainMask;
  }

  LongBoundedNatBitSet(long store, @Nonnegative int domainSize) {
    this(store, domainSize, false);
  }

  LongBoundedNatBitSet(int domainSize) {
    this(0L, domainSize, false);
  }

  public static int maximalSize() {
    return Long.SIZE;
  }

  @Override
  public void and(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      clear();
    } else if (indices instanceof LongBoundedNatBitSet) {
      LongBoundedNatBitSet other = (LongBoundedNatBitSet) indices;

      if (complement) {
        store[0] |= other.complement ? other.store[0] | ~other.domainMask : ~other.store[0];
        store[0] &= domainMask;
      } else {
        store[0] &= other.complement ? other.complementBits() : other.store[0];
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
    if (indices instanceof LongBoundedNatBitSet) {
      LongBoundedNatBitSet other = (LongBoundedNatBitSet) indices;

      if (complement) {
        store[0] |= other.complement ? other.complementBits() : other.store[0];
        store[0] &= domainMask;
      } else {
        store[0] &= other.complement ? other.store[0] | ~other.domainMask : ~other.store[0];
      }
    } else {
      super.andNot(indices);
    }
    assert checkConsistency();
  }

  private boolean checkConsistency() {
    return (store[0] & ~domainMask) == 0L;
  }

  @Override
  public void clear(int index) {
    assert checkConsistency();
    checkInDomain(index);
    if (complement) {
      store[0] |= 1L << index;
    } else {
      store[0] &= ~(1L << index);
    }
    assert checkConsistency();
  }

  @Override
  public void clear(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    if (complement) {
      store[0] |= mask(from, to);
    } else {
      store[0] &= ~mask(from, to);
    }
    assert checkConsistency();
  }

  @Override
  public void clear() {
    assert checkConsistency();
    if (complement) {
      store[0] = domainMask;
    } else {
      store[0] = 0L;
    }
    assert checkConsistency();
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public LongBoundedNatBitSet clone() {
    assert checkConsistency();
    return new LongBoundedNatBitSet(store[0], domainSize(), complement);
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public BoundedNatBitSet complement() {
    return complementView;
  }

  protected long complementBits() {
    return ~store[0] & domainMask;
  }

  @Override
  public boolean contains(int index) {
    assert checkConsistency();
    return inDomain(index) && containsIndex(index);
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
    if (indices instanceof LongBoundedNatBitSet) {
      LongBoundedNatBitSet other = (LongBoundedNatBitSet) indices;

      long otherSetBits = other.complement ? other.complementBits() : other.store[0];
      long unsetBits = complement ? store[0] | ~domainMask : ~store[0];

      return (unsetBits & otherSetBits) == 0L;
    }
    return super.containsAll(indices);
  }

  private boolean containsIndex(int index) {
    return ((store[0] & (1L << index)) == 0L) == complement;
  }

  @Override
  public int firstInt() {
    assert checkConsistency();
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    int first = Long.numberOfTrailingZeros(complement ? ~store[0] : store[0]);
    assert first < Long.SIZE && containsIndex(first);
    return first;
  }

  @Override
  public void flip(int index) {
    assert checkConsistency();
    checkInDomain(index);
    store[0] ^= (1L << index);
    assert checkConsistency();
  }

  @Override
  public void flip(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    store[0] ^= mask(from, to);
    assert checkConsistency();
  }

  @Override
  public boolean intersects(Collection<Integer> indices) {
    assert checkConsistency();
    if (indices instanceof LongBoundedNatBitSet) {
      LongBoundedNatBitSet other = (LongBoundedNatBitSet) indices;
      long store = complement ? complementBits() : this.store[0];
      long otherStore = other.complement ? other.complementBits() : other.store[0];
      return (store & otherStore) != 0L;
    }
    return super.intersects(indices);
  }

  @Override
  boolean isComplement() {
    return complement;
  }

  @Override
  public boolean isEmpty() {
    assert checkConsistency();
    return store[0] == (complement ? domainMask : 0L);
  }

  @Override
  public IntIterator iterator() {
    return new NatBitSetIterator(this);
  }

  @Override
  public int lastInt() {
    assert checkConsistency();
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    int last = Long.SIZE - Long.numberOfLeadingZeros(complement ? complementBits() : store[0]) - 1;
    assert 0 <= last && containsIndex(last);
    return last;
  }

  @Override
  public int nextAbsentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    if (index >= domainSize() || isEmpty()) {
      return index;
    }
    long masked = (complement ? store[0] : complementBits()) & ~maskTo(index);
    if (masked == 0L) {
      return domainSize();
    }
    int next = Long.numberOfTrailingZeros(masked);
    assert next < Long.SIZE && !containsIndex(next) : next;
    return next;
  }

  @Override
  public int nextPresentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    if (index >= domainSize() || isEmpty()) {
      return -1;
    }
    long masked = (complement ? complementBits() : store[0]) & ~maskTo(index);
    if (masked == 0L) {
      return -1;
    }
    int next = Long.numberOfTrailingZeros(masked);
    assert next < Long.SIZE && containsIndex(next) : next;
    return next;
  }

  @Override
  public void or(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof LongBoundedNatBitSet) {
      LongBoundedNatBitSet other = (LongBoundedNatBitSet) indices;
      checkInDomain(other.lastInt());
      if (complement) {
        store[0] &= other.complement ? (other.store[0] | ~other.domainMask) : ~other.store[0];
      } else {
        store[0] |= other.complement ? other.complementBits() : other.store[0];
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
      store[0] = complement ? 0L : domainMask;
    } else if (indices instanceof LongBoundedNatBitSet) {
      LongBoundedNatBitSet other = (LongBoundedNatBitSet) indices;

      if (complement) {
        store[0] &= other.complement ? other.complementBits() : other.store[0];
      } else {
        store[0] |= ((other.complement ? other.store[0] : ~other.store[0]) | ~other.domainMask)
            & domainMask;
      }
    } else {
      super.orNot(indices);
    }
    assert checkConsistency();
  }

  @Override
  public int previousAbsentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    if (index >= domainSize() || isEmpty()) {
      return index;
    }
    long mask = maskTo(index + 1);
    long masked = (complement ? store[0] : complementBits()) & mask;
    if (masked == 0L) {
      return -1;
    }
    int previous = Long.SIZE - Long.numberOfLeadingZeros(masked) - 1;
    assert !containsIndex(previous) : previous;
    return previous;
  }

  @Override
  public int previousPresentIndex(int index) {
    assert checkConsistency();
    NatBitSetsUtil.checkNonNegative(index);
    if (isEmpty()) {
      return -1;
    }

    long masked = (complement ? complementBits() : store[0]) & maskTo(index + 1);
    if (masked == 0L) {
      return -1;
    }
    int previous = Long.SIZE - Long.numberOfLeadingZeros(masked) - 1;
    assert containsIndex(previous) : previous;
    return previous;
  }

  @Override
  public void set(int index) {
    assert checkConsistency();
    checkInDomain(index);
    if (complement) {
      store[0] &= ~(1L << index);
    } else {
      store[0] |= (1L << index);
    }
    assert checkConsistency();
  }

  @Override
  public void set(int index, boolean value) {
    assert checkConsistency();
    if (value) {
      set(index);
    } else {
      clear(index);
    }
    assert checkConsistency();
  }

  @Override
  public void set(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    if (complement) {
      store[0] &= ~mask(from, to);
    } else {
      store[0] |= mask(from, to);
    }
    assert checkConsistency();
  }

  @Override
  public int size() {
    assert checkConsistency();
    int bitCount = Long.bitCount(store[0]);
    return complement ? domainSize() - bitCount : bitCount;
  }

  @Override
  public void xor(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof LongBoundedNatBitSet) {
      LongBoundedNatBitSet other = (LongBoundedNatBitSet) indices;
      checkInDomain(other.domainSize() - 1);
      store[0] ^= other.store[0];
      if (other.complement) {
        store[0] ^= other.domainMask;
      }
    } else {
      super.xor(indices);
    }
    assert checkConsistency();
  }
}
