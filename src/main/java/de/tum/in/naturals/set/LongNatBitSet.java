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

class LongNatBitSet extends AbstractNatBitSet {
  private long store = 0L;

  public static int maximalSize() {
    return Long.SIZE;
  }

  @Override
  public void and(IntCollection indices) {
    if (indices instanceof LongNatBitSet) {
      LongNatBitSet other = (LongNatBitSet) indices;
      store &= other.store;
    } else {
      super.and(indices);
    }
  }

  @Override
  public void andNot(IntCollection indices) {
    if (indices instanceof LongNatBitSet) {
      LongNatBitSet other = (LongNatBitSet) indices;
      store &= ~other.store;
    } else {
      super.andNot(indices);
    }
  }

  private void checkInDomain(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (!inDomain(index)) {
      throw new UnsupportedOperationException(String.format("Index %d out of bounds", index));
    }
  }

  @Override
  public void clear(int index) {
    if (inDomain(index)) {
      store &= ~(1L << index);
    }
  }

  @Override
  public void clear(int from, int to) {
    NatBitSetsUtil.checkRange(from, to);
    if (!inDomain(from)) {
      return;
    }
    store &= ~mask(from, inDomain(to) ? to : Long.SIZE);
  }

  @Override
  public void clear() {
    store = 0L;
  }

  @Override
  public void clearFrom(int from) {
    if (from < Long.SIZE) {
      store &= maskTo(from);
    }
  }

  @Override
  public LongNatBitSet clone() {
    return (LongNatBitSet) super.clone();
  }

  @Override
  public boolean contains(int index) {
    return inDomain(index) && containsIndex(index);
  }

  @Override
  public boolean containsAll(IntCollection indices) {
    if (indices instanceof LongNatBitSet) {
      LongNatBitSet other = (LongNatBitSet) indices;
      return (~store & other.store) == 0L;
    }
    return super.containsAll(indices);
  }

  private boolean containsIndex(int index) {
    return (store & (1L << index)) != 0L;
  }

  @Override
  public int firstInt() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    int first = Long.numberOfTrailingZeros(store);
    assert first < Long.SIZE && containsIndex(first);
    return first;
  }

  @Override
  public void flip(int index) {
    checkInDomain(index);
    store ^= (1L << index);
  }

  @Override
  public void flip(int from, int to) {
    NatBitSetsUtil.checkRange(from, to);
    checkInDomain(to);
    store ^= mask(from, to);
  }

  long getStore() {
    return store;
  }

  private boolean inDomain(int index) {
    return 0 <= index && index < Long.SIZE;
  }

  @Override
  public boolean intersects(Collection<Integer> indices) {
    if (indices instanceof LongNatBitSet) {
      LongNatBitSet other = (LongNatBitSet) indices;
      return (store & other.store) != 0L;
    }
    return super.intersects(indices);
  }

  @Override
  public boolean isEmpty() {
    return store == 0L;
  }

  @Override
  public IntIterator iterator() {
    return new NatBitSetIterator(this);
  }

  @Override
  public int lastInt() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    int last = Long.SIZE - Long.numberOfLeadingZeros(store) - 1;
    assert 0 <= last && containsIndex(last);
    return last;
  }

  @Override
  public int nextAbsentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (isEmpty() || index >= Long.SIZE) {
      return index;
    }

    long masked = ~(store | maskTo(index));
    if (masked == 0L) {
      return Long.SIZE;
    }
    int next = Long.numberOfTrailingZeros(masked);
    assert next < Long.SIZE && !containsIndex(next);
    return next;
  }

  @Override
  public int nextPresentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (index >= Long.SIZE || isEmpty()) {
      return -1;
    }

    long masked = store & ~maskTo(index);
    if (masked == 0L) {
      return -1;
    }
    int next = Long.numberOfTrailingZeros(masked);
    assert next < Long.SIZE && containsIndex(next) : next;
    return next;
  }

  @Override
  public void or(IntCollection indices) {
    if (indices instanceof LongNatBitSet) {
      LongNatBitSet other = (LongNatBitSet) indices;
      store |= other.store;
    } else {
      super.or(indices);
    }
  }

  @Override
  public int previousAbsentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (isEmpty() || index >= Long.SIZE) {
      return index;
    }

    long mask = maskTo(index + 1);
    long masked = ~store & mask;
    if (masked == 0L) {
      return -1;
    }
    int previous = Long.SIZE - Long.numberOfLeadingZeros(masked) - 1;
    assert !containsIndex(previous);
    return previous;
  }

  @Override
  public int previousPresentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (isEmpty()) {
      return -1;
    }

    long masked = store & maskTo(index + 1);
    if (masked == 0L) {
      return -1;
    }
    int previous = Long.SIZE - Long.numberOfLeadingZeros(masked) - 1;
    assert containsIndex(previous) : previous;
    return previous;
  }

  @Override
  public void set(int index) {
    checkInDomain(index);
    store |= (1L << index);
  }

  @Override
  public void set(int index, boolean value) {
    if (value) {
      set(index);
    } else {
      clear(index);
    }
  }

  @Override
  public void set(int from, int to) {
    NatBitSetsUtil.checkRange(from, to);
    checkInDomain(to);
    store |= mask(from, to);
  }

  @Override
  public int size() {
    return Long.bitCount(store);
  }

  @Override
  public void xor(IntCollection indices) {
    if (indices instanceof LongNatBitSet) {
      LongNatBitSet other = (LongNatBitSet) indices;
      store ^= other.store;
    } else {
      super.xor(indices);
    }
  }
}
