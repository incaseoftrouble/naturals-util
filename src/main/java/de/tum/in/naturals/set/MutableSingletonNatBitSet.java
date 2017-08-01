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
import java.util.NoSuchElementException;

class MutableSingletonNatBitSet extends AbstractNatBitSet {
  private static final int EMPTY = Integer.MIN_VALUE;
  private int element;

  MutableSingletonNatBitSet() {
    setEmpty();
  }

  MutableSingletonNatBitSet(int element) {
    this.element = element;
  }

  @Override
  public boolean add(int key) {
    if (key < 0) {
      throw new IllegalArgumentException(String.format("Negative index %d", key));
    }
    if (key == element) {
      return false;
    }
    if (!isEmpty()) {
      throw new UnsupportedOperationException("Singleton can hold at most one value");
    }
    setValue(key);
    return true;
  }

  @Override
  public void clear(int i) {
    if (i == element) {
      setEmpty();
    }
  }

  @Override
  public void clear(int from, int to) {
    if (from > to) {
      throw new IllegalArgumentException(String.format("From %d larger than to %d", from, to));
    }
    if (from == to || isEmpty()) {
      return;
    }
    if (from <= element && element < to) {
      setEmpty();
    }
  }

  @Override
  public void clear() {
    setEmpty();
  }

  @Override
  public MutableSingletonNatBitSet clone() {
    return (MutableSingletonNatBitSet) super.clone();
  }

  @Override
  public boolean contains(int key) {
    return !isEmpty() && key == element;
  }

  @Override
  public int firstInt() {
    if (element == EMPTY) {
      throw new NoSuchElementException();
    }
    return element;
  }

  @Override
  public void flip(int index) {
    if (isEmpty()) {
      element = index;
    } else if (index == element) {
      element = EMPTY;
    } else {
      throw new UnsupportedOperationException("Singleton can hold at most one value");
    }
  }

  @Override
  public void flip(int from, int to) {
    if (from > to) {
      throw new IllegalArgumentException(String.format("From %d larger than to %d", from, to));
    }
    if (from == to) {
      return;
    }
    if (from + 1 != to) {
      throw new UnsupportedOperationException("Singleton can hold at most one value");
    }
    if (isEmpty()) {
      setValue(from);
    } else if (from == element) {
      setEmpty();
    }
  }

  @Override
  public boolean isEmpty() {
    return element == EMPTY;
  }

  @Override
  public IntIterator iterator() {
    return new SingletonSetIterator(this);
  }

  @Override
  public int lastInt() {
    if (element == EMPTY) {
      throw new NoSuchElementException();
    }
    return element;
  }

  @Override
  public boolean remove(int key) {
    if (isEmpty()) {
      return false;
    }
    if (key != element) {
      return false;
    }
    setEmpty();
    return true;
  }

  @Override
  public void set(int index) {
    if (index < 0) {
      throw new IllegalArgumentException(String.format("Negative index %d", index));
    }
    if (isEmpty()) {
      element = index;
    } else if (index != element) {
      throw new UnsupportedOperationException("Singleton can hold at most one value");
    }
  }

  @Override
  public void set(int index, boolean value) {
    if (index < 0) {
      throw new IllegalArgumentException(String.format("Negative index %d", index));
    }
    if (isEmpty()) {
      if (value) {
        setValue(index);
      }
    } else if ((index == element) != value) {
      throw new UnsupportedOperationException("Singleton can hold at most one value");
    }
  }

  @Override
  public void set(int from, int to) {
    if (from > to) {
      throw new IllegalArgumentException(String.format("From %d larger than to %d", from, to));
    }
    if (from == to) {
      return;
    }
    if (from + 1 != to) {
      throw new UnsupportedOperationException("Singleton can hold at most one value");
    }
    if (isEmpty()) {
      setValue(from);
    } else if (from != element) {
      throw new UnsupportedOperationException("Singleton can hold at most one value");
    }
  }

  private void setEmpty() {
    element = EMPTY;
  }

  private void setValue(int value) {
    assert value >= 0 && isEmpty();
    element = value;
  }

  @Override
  public int size() {
    return isEmpty() ? 0 : 1;
  }

  @Override
  public int[] toIntArray() {
    return new int[] {element};
  }

  private static class SingletonSetIterator implements IntIterator {
    private final MutableSingletonNatBitSet set;
    private int element;

    public SingletonSetIterator(MutableSingletonNatBitSet set) {
      this.set = set;
      element = set.element;
    }

    @Override
    public boolean hasNext() {
      return element != EMPTY;
    }

    @Override
    public int nextInt() {
      if (element == EMPTY) {
        throw new NoSuchElementException();
      }
      element = EMPTY;
      return set.element;
    }

    @Override
    public void remove() {
      if (!(element == EMPTY && !set.isEmpty())) {
        throw new IllegalStateException();
      }
      set.clear();
    }
  }
}
