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

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.NoSuchElementException;
import javax.annotation.Nonnegative;

class BoundedMutableSingletonNatBitSet extends AbstractBoundedNatBitSet {
  private static final int EMPTY = Integer.MIN_VALUE;
  private final boolean complement;
  private final BoundedMutableSingletonNatBitSet complementView;
  private final int[] store;

  private BoundedMutableSingletonNatBitSet(int element, @Nonnegative int domainSize,
      boolean complement) {
    super(domainSize);
    checkNonNegative(element);
    this.store = new int[] {element};
    this.complement = complement;
    this.complementView = new BoundedMutableSingletonNatBitSet(this);
  }

  private BoundedMutableSingletonNatBitSet(BoundedMutableSingletonNatBitSet other) {
    super(other.domainSize());
    this.complementView = other;
    this.complement = !other.complement;
    this.store = other.store;
  }

  BoundedMutableSingletonNatBitSet(@Nonnegative int element, @Nonnegative int domainSize) {
    this(element, domainSize, false);
  }

  BoundedMutableSingletonNatBitSet(@Nonnegative int domainSize) {
    this(EMPTY, domainSize, false);
  }

  private static void throwOperationUnsupported() {
    throw new UnsupportedOperationException("Singleton can hold at most one value");
  }

  @Override
  public void clear(int index) {
    checkInDomain(index);

    if (complement) {
      if (isStoreEmpty()) {
        setStoreValue(index);
      } else if (index != store[0]) {
        throwOperationUnsupported();
      }
    } else {
      assert index != EMPTY;
      if (index == store[0]) {
        setStoreEmpty();
      }
    }
  }

  @Override
  public void clear(int from, int to) {
    checkInDomain(from, to);
    if (from == to) {
      return;
    }
    if (isEmpty()) {
      return;
    }

    if (complement) {
      if (from + 1 != to) {
        throwOperationUnsupported();
      }
      if (isStoreEmpty()) {
        setStoreValue(from);
      } else if (from != store[0]) {
        throwOperationUnsupported();
      }
    } else if (from <= store[0] && store[0] < to) {
      setStoreEmpty();
    }
  }

  @Override
  public void clear() {
    if (complement) {
      if (domainSize() > 1) {
        throwOperationUnsupported();
      }
      setStoreValue(0);
    } else {
      setStoreEmpty();
    }
  }

  @Override
  public BoundedMutableSingletonNatBitSet clone() {
    return (BoundedMutableSingletonNatBitSet) super.clone();
  }

  @Override
  boolean isComplement() {
    return complement;
  }

  @Override
  public BoundedNatBitSet complement() {
    return complementView;
  }

  @Override
  public boolean contains(int index) {
    return index >= 0 && (complement ? store[0] != index : store[0] == index);
  }

  @Override
  public int firstInt() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    if (complement) {
      return store[0] == 0 ? 1 : 0;
    }
    return store[0];
  }

  @Override
  public void flip(int index) {
    checkInDomain(index);
    if (index == store[0]) {
      setStoreEmpty();
    } else if (isStoreEmpty()) {
      setStoreValue(index);
    } else {
      throwOperationUnsupported();
    }
  }

  @Override
  public void flip(int from, int to) {
    checkInDomain(from, to);
    if (from == to) {
      return;
    }
    if (from + 1 != to) {
      throwOperationUnsupported();
    }
    flip(from);
  }

  @Override
  public boolean isEmpty() {
    return complement ? (domainSize() == 1 && !isStoreEmpty()) : isStoreEmpty();
  }

  private boolean isStoreEmpty() {
    return store[0] == EMPTY;
  }

  @Override
  public IntIterator iterator() {
    if (isStoreEmpty()) {
      return complement ? IntIterators.fromTo(0, domainSize()) : IntIterators.EMPTY_ITERATOR;
    }
    if (complement) {
      IntListIterator first = IntIterators.fromTo(0, store[0]);
      IntListIterator second = IntIterators.fromTo(store[0] + 1, domainSize());
      return IntIterators.concat(new IntIterator[] {first, second});
    }
    return IntIterators.singleton(store[0]);
  }

  @Override
  public int lastInt() {
    if (isEmpty()) {
      throw new NoSuchElementException();
    }
    if (complement) {
      int domainSize = domainSize();
      return domainSize - (store[0] == domainSize - 1 ? 2 : 1);
    }
    return store[0];
  }

  @Override
  public int nextAbsentIndex(int index) {
    checkNonNegative(index);
    if (complement) {
      return index < store[0] ? store[0] : domainSize();
    }
    return (!isEmpty() && index == store[0]) ? store[0] + 1 : index;
  }

  @Override
  public int nextPresentIndex(int index) {
    checkNonNegative(index);
    if (isEmpty()) {
      return -1;
    }
    if (domainSize() <= index) {
      return -1;
    }

    if (complement) {
      return index == store[0] ? index + 1 : index;
    }
    return index <= store[0] ? store[0] : -1;
  }

  @Override
  public void set(int index) {
    checkInDomain(index);

    if (complement) {
      if (index == store[0]) {
        setStoreEmpty();
      }
    } else {
      if (isStoreEmpty()) {
        store[0] = index;
      } else if (index != store[0]) {
        throwOperationUnsupported();
      }
    }
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
    checkInDomain(from, to);
    if (from == to) {
      return;
    }
    if (from + 1 != to) {
      throwOperationUnsupported();
    }
    set(from);
  }

  private void setStoreEmpty() {
    store[0] = EMPTY;
  }

  private void setStoreValue(int value) {
    assert value >= 0 && isEmpty();
    store[0] = value;
  }

  @Override
  public int size() {
    if (complement) {
      return isStoreEmpty() ? domainSize() : domainSize() - 1;
    }
    return isStoreEmpty() ? 0 : 1;
  }
}
