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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * A int set which uses a sorted array as backing data structure. This gives excellent lookup
 * complexity of {@code O(log n)}, but very costly worst case insertion time of {@code O(n)}.
 */
public class IntArraySortedSet extends AbstractIntSet {
  /**
   * The backing array (valid up to {@link #size}, excluded).
   */
  private int[] array;
  /**
   * The number of valid entries in {@link #array}.
   */
  private int size;

  /**
   * Creates a new empty array set.
   */
  public IntArraySortedSet() {
    this.array = IntArrays.EMPTY_ARRAY;
  }

  /**
   * Creates a new empty array set of given initial capacity.
   *
   * @param capacity
   *     the initial capacity.
   */
  public IntArraySortedSet(int capacity) {
    this.array = new int[capacity];
  }

  /**
   * Creates a new array set copying the contents of a given collection.
   *
   * @param c
   *     a collection.
   */
  public IntArraySortedSet(IntCollection c) {
    this(c.size());
    addAll(c);
  }

  /**
   * Creates a new array set copying the contents of a given set.
   *
   * @param c
   *     a collection.
   */
  public IntArraySortedSet(Collection<? extends Integer> c) {
    this(c.size());
    addAll(c);
  }

  /**
   * Creates a new array set using the given backing array and the given number of elements of the
   * array.
   *
   * <p>It is responsibility of the caller that the first {@code size} elements of {@code a} are
   * distinct and sorted.
   *
   * @param array
   *     the backing array.
   * @param size
   *     the number of valid elements in {@code a}.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Intended to expose")
  public IntArraySortedSet(int[] array, int size) {
    if (size > array.length) {
      throw new IllegalArgumentException(String.format("Size %d larger than array %d",
          size, array.length));
    }
    assert isSortedUnique(array, size);
    this.array = array;
    this.size = size;
  }

  /**
   * Creates a new array set using the given backing array. The resulting set will have as many
   * elements as the array.
   *
   * <p>It is responsibility of the caller that the elements of {@code a} are distinct and sorted.
   *
   * @param array
   *     the backing array.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Intended to expose")
  public IntArraySortedSet(int[] array) {
    assert isSortedUnique(array, array.length);
    this.array = array;
    size = array.length;
  }

  private static boolean isSortedUnique(int[] array, int size) {
    assert size <= array.length;
    if (array.length <= 1) {
      return true;
    }
    int val = array[0];
    for (int i = 1; i < size; i++) {
      int next = array[i];
      if (next <= val) {
        return false;
      }
      val = next;
    }
    return true;
  }

  @Override
  public boolean add(int k) {
    if (array.length == 0) {
      array = new int[] {k, 0};
      size = 1;
      return true;
    }

    int index = keyIndex(k);
    if (index >= 0) {
      return false;
    }

    int insertionPoint = -(index + 1);
    int tailLength = size - insertionPoint;

    if (size == array.length) {
      int newSize = size * 2;
      if (insertionPoint == size) {
        array = Arrays.copyOf(array, newSize);
        array[insertionPoint] = k;
      } else {
        int[] newArray = new int[newSize];
        System.arraycopy(array, 0, newArray, 0, insertionPoint);
        System.arraycopy(array, insertionPoint, newArray, insertionPoint + 1, tailLength);

        array = newArray;
        newArray[insertionPoint] = k;
      }
    } else if (insertionPoint == size) {
      array[size] = k;
    } else {
      System.arraycopy(array, insertionPoint, array, insertionPoint + 1, tailLength);
      array[insertionPoint] = k;
    }
    size++;
    return true;
  }

  @Override
  public void clear() {
    size = 0;
  }

  /**
   * Returns a deep copy of this set.
   */
  @Override
  public IntArraySortedSet clone() {
    IntArraySortedSet clone;
    try {
      clone = (IntArraySortedSet) super.clone();
    } catch (CloneNotSupportedException ignored) {
      throw new InternalError(ignored);
    }
    clone.array = array.clone();
    return clone;
  }

  @Override
  public boolean contains(int k) {
    return keyIndex(k) >= 0;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public IntIterator iterator() {
    return new Iterator(this);
  }

  private int keyIndex(int key) {
    return Arrays.binarySearch(array, 0, size, key);
  }

  @Override
  public boolean remove(int k) {
    int index = keyIndex(k);
    if (index < 0) {
      return false;
    }
    removeIndex(index);
    return true;
  }

  private void removeIndex(int index) {
    int nextIndex = index + 1;
    if (nextIndex < size) {
      int tail = size - nextIndex;
      System.arraycopy(array, nextIndex, array, index, tail);
    }
    size--;
  }

  @Override
  public int size() {
    return size;
  }

  public void trim() {
    if (array.length == size) {
      return;
    }
    array = Arrays.copyOf(array, size);
  }

  private static class Iterator implements IntIterator {
    private final IntArraySortedSet set;
    private int next = 0;

    Iterator(IntArraySortedSet set) {
      this.set = set;
    }

    @Override
    public boolean hasNext() {
      return next < set.size;
    }

    @Override
    public int nextInt() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int result = set.array[next];
      next += 1;
      return result;
    }

    @Override
    public void remove() {
      next -= 1;
      set.removeIndex(next);
    }
  }
}
