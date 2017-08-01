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
  private final boolean empty;
  private final FixedSizeNatBitSet emptyView;

  FixedSizeNatBitSet(int domainSize) {
    super(domainSize);
    this.empty = false;
    this.emptyView = new FixedSizeNatBitSet(this);
  }

  private FixedSizeNatBitSet(FixedSizeNatBitSet fullSet) {
    super(fullSet.domainSize());
    empty = true;
    emptyView = fullSet;
  }


  @Override
  public void clear(int index) {
    checkIndex(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear(int from, int to) {
    checkRange(from, to);
    throw new UnsupportedOperationException();
  }

  @Override
  public FixedSizeNatBitSet clone() {
    // Immutable object
    return this;
  }

  @Override
  public BoundedNatBitSet complement() {
    return emptyView;
  }

  @Override
  public boolean contains(int key) {
    return !empty && inRange(key);
  }

  @Override
  public boolean containsAll(IntCollection ints) {
    if (ints instanceof NatBitSet) {
      NatBitSet natBitSet = (NatBitSet) ints;
      return contains(natBitSet.lastInt());
    }
    if (ints instanceof IntSortedSet) {
      IntSortedSet sortedSet = (IntSortedSet) ints;
      return contains(sortedSet.lastInt());
    }
    if (ints instanceof SortedSet<?>) {
      SortedSet<?> sortedSet = (SortedSet<?>) ints;
      assert sortedSet.last() instanceof Integer;
      int last = (Integer) sortedSet.last();
      return contains(last);
    }

    IntIterator iterator = ints.iterator();
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
    checkIndex(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void flip(int from, int to) {
    checkRange(from, to);
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean intersects(IntCollection ints) {
    if (isEmpty()) {
      return false;
    }
    if (ints instanceof IntSortedSet) {
      IntSortedSet sortedInts = (IntSortedSet) ints;
      return sortedInts.subSet(0, domainSize()).isEmpty();
    }
    if (ints.size() <= domainSize()) {
      return IntIterators.any(ints.iterator(), this::contains);
    }
    return IntIterators.any(iterator(), ints::contains);
  }

  @Override
  public boolean isEmpty() {
    return empty || domainSize() == 0;
  }

  @Override
  public IntIterator iterator() {
    return IntIterators.fromTo(0, domainSize());
  }

  @Override
  public int lastInt() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    return domainSize() - 1;
  }

  @Override
  public void set(int index) {
    checkIndex(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(int index, boolean value) {
    checkIndex(index);
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(int from, int to) {
    checkRange(from, to);
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return empty ? 0 : domainSize();
  }
}
