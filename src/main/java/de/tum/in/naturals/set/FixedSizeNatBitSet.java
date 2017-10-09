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

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;

final class FixedSizeNatBitSet extends AbstractBoundedNatBitSet {
  private final boolean complement;
  private final FixedSizeNatBitSet complementView;

  FixedSizeNatBitSet(int domainSize) {
    super(domainSize);
    if (domainSize == 0) {
      complement = true;
      complementView = this;
    } else {
      this.complement = false;
      this.complementView = new FixedSizeNatBitSet(this);
    }
  }

  private FixedSizeNatBitSet(FixedSizeNatBitSet other) {
    super(other.domainSize());
    complement = !other.complement;
    complementView = other;
  }

  @Override
  public void clear(int index) {
    checkInDomain(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear(int from, int to) {
    checkInDomain(from, to);
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FixedSizeNatBitSet clone() {
    // Immutable object
    return this;
  }

  @Override
  public BoundedNatBitSet complement() {
    return complementView;
  }

  @Override
  public boolean contains(int index) {
    return !complement && inDomain(index);
  }

  @Override
  public boolean containsAll(IntCollection indices) {
    if (indices.isEmpty()) {
      return true;
    }
    if (complement) {
      // indices is not empty here
      return false;
    }
    if (indices instanceof NatBitSet) {
      NatBitSet natBitSet = (NatBitSet) indices;
      return 0 <= natBitSet.firstInt() && natBitSet.lastInt() < domainSize();
    }
    if (indices instanceof IntSortedSet) {
      IntSortedSet sortedSet = (IntSortedSet) indices;
      return 0 <= sortedSet.firstInt() && sortedSet.lastInt() < domainSize();
    }
    if (indices instanceof SortedSet<?>) {
      //noinspection unchecked
      SortedSet<Integer> sortedSet = (SortedSet<Integer>) indices;
      return 0 <= sortedSet.first() && sortedSet.last() < domainSize();
    }

    IntIterator iterator = indices.iterator();
    while (iterator.hasNext()) {
      if (!contains(iterator.nextInt())) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int firstInt() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return 0;
  }

  @Override
  public void flip(int index) {
    checkInDomain(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void flip(int from, int to) {
    checkInDomain(from, to);
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean intersects(IntCollection indices) {
    if (isEmpty() || indices.isEmpty()) {
      return false;
    }
    if (indices instanceof NatBitSet) {
      NatBitSet natBitSet = (NatBitSet) indices;
      return contains(natBitSet.firstInt());
    }
    if (indices instanceof IntSortedSet) {
      IntSortedSet sortedInts = (IntSortedSet) indices;
      return !sortedInts.subSet(0, domainSize()).isEmpty();
    }
    if (indices.size() <= domainSize()) {
      return IntIterators.any(indices.iterator(), this::contains);
    }
    return IntIterators.any(iterator(), indices::contains);
  }

  @Override
  boolean isComplement() {
    return complement;
  }

  @Override
  public boolean isEmpty() {
    return complement;
  }

  @Override
  public IntIterator iterator() {
    return isEmpty() ? IntIterators.EMPTY_ITERATOR : IntIterators.fromTo(0, domainSize());
  }

  @Override
  public int lastInt() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return domainSize() - 1;
  }

  @Override
  public int nextAbsentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (complement) {
      return index;
    }
    return Math.max(index, domainSize());
  }

  @Override
  public int nextPresentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (complement) {
      return -1;
    }
    return index < domainSize() ? index : -1;
  }

  @Override
  public int previousAbsentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (complement) {
      return index;
    }
    return index >= domainSize() ? index : -1;
  }

  @Override
  public int previousPresentIndex(int index) {
    NatBitSetsUtil.checkNonNegative(index);
    if (complement) {
      return -1;
    }
    return index >= domainSize() ? domainSize() - 1 : index;
  }

  @Override
  public IntIterator reverseIterator() {
    return isEmpty() ? IntIterators.EMPTY_ITERATOR : new ReverseRangeIterator(0, domainSize());
  }

  @Override
  public void set(int index) {
    checkInDomain(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(int index, boolean value) {
    checkInDomain(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(int from, int to) {
    checkInDomain(from, to);
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return isEmpty() ? 0 : domainSize();
  }
}
