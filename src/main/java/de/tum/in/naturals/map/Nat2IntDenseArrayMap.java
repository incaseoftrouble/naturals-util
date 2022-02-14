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

package de.tum.in.naturals.map;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.AbstractInt2IntMap;
import it.unimi.dsi.fastutil.ints.AbstractIntCollection;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;

/**
 * An efficient representation of a total mapping from {0, ..., n} to {{@link Integer#MIN_VALUE} +
 * 1, ..., {@link Integer#MAX_VALUE}}.
 *
 * <p><strong>Warning:</strong> For performance, missing keys are stored as
 * {@link Integer#MIN_VALUE}. Thus, this value cannot be inserted into this map.</p>
 */
public class Nat2IntDenseArrayMap extends AbstractInt2IntMap {
  private static final long serialVersionUID = 5185461790033343414L;

  private final int[] array;
  @Nullable
  private transient EntrySetView entriesView = null;
  @Nullable
  private transient KeySetView keySetView = null;
  private int size = 0;
  @Nullable
  private transient ValuesView valuesView = null;

  public Nat2IntDenseArrayMap(int[] array) {
    this.array = array;
    for (int value : array) {
      if (!isAbsent(value)) {
        size++;
      }
    }
  }

  public Nat2IntDenseArrayMap(int size) {
    this.array = new int[size];
    Arrays.fill(this.array, Integer.MIN_VALUE);
  }

  public Nat2IntDenseArrayMap(int size, int initialValue) {
    checkNotAbsent(initialValue);
    this.array = new int[size];
    if (initialValue != 0) {
      Arrays.fill(array, initialValue);
    }
    this.size = size;
  }

  public Nat2IntDenseArrayMap(int size, IntUnaryOperator initialValues) {
    this.array = new int[size];
    for (int i = 0; i < array.length; i++) {
      int value = initialValues.applyAsInt(i);
      checkNotAbsent(value);
      array[i] = value;
    }
    this.size = size;
  }

  private void checkNotAbsent(int value) {
    if (isAbsent(value)) {
      throw new IllegalArgumentException(String.format("Value %d not allowed", value));
    }
  }

  @Override
  public void clear() {
    Arrays.fill(array, Integer.MIN_VALUE);
    size = 0;
  }

  @Override
  public boolean containsKey(int key) {
    return 0 <= key && key < array.length && !isAbsent(array[key]);
  }

  @Override
  public boolean containsValue(int v) {
    if (isAbsent(v)) {
      return false;
    }
    for (int value : array) {
      if (value == v) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Nat2IntDenseArrayMap) {
      Nat2IntDenseArrayMap other = (Nat2IntDenseArrayMap) o;
      return size == other.size && Arrays.equals(array, other.array);
    }
    return super.equals(o);
  }

  public void fill(int from, int to, int value) {
    checkNotAbsent(value);
    Arrays.fill(array, from, to, value);
  }

  public void fill(PrimitiveIterator.OfInt iterator, int value) {
    checkNotAbsent(value);
    while (iterator.hasNext()) {
      array[iterator.next()] = value;
    }
  }

  @Override
  public int get(int key) {
    int value = array[key];
    return isAbsent(value) ? defaultReturnValue() : value;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(array) ^ HashCommon.mix(size);
  }

  @Override
  public ObjectSet<Int2IntMap.Entry> int2IntEntrySet() {
    if (entriesView == null) {
      entriesView = new EntrySetView(this);
    }
    return new EntrySetView(this);
  }

  private boolean isAbsent(int value) {
    return value == Integer.MIN_VALUE;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public IntSet keySet() {
    if (keySetView == null) {
      keySetView = new KeySetView(this);
    }
    return keySetView;
  }

  private int nextKey(int index) {
    for (int i = index; i < array.length; i++) {
      if (!isAbsent(array[i])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public int put(int key, int value) {
    checkNotAbsent(value);
    int previous = array[key];
    array[key] = value;
    if (isAbsent(previous)) {
      size++;
      return defaultReturnValue();
    }
    return previous;
  }

  @Override
  public int remove(int key) {
    int previous = array[key];
    if (isAbsent(previous)) {
      return defaultReturnValue();
    }
    array[key] = Integer.MIN_VALUE;
    size--;
    return previous;
  }

  @Override
  public int size() {
    return size;
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public IntCollection values() {
    if (valuesView == null) {
      valuesView = new ValuesView(this);
    }
    return valuesView;
  }

  @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
  private static class EntryIterator implements ObjectIterator<Int2IntMap.Entry> {
    private final Nat2IntDenseArrayMap map;
    private int next;

    EntryIterator(Nat2IntDenseArrayMap map) {
      this.map = map;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next != -1;
    }

    @Override
    public Int2IntMap.Entry next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int index = next;
      next = map.nextKey(next + 1);
      assert map.containsKey(index);
      return new AbstractInt2IntMap.BasicEntry(index, map.array[index]);
    }
  }

  private static class EntrySetView extends AbstractInt2IntEntrySet<Nat2IntDenseArrayMap> {
    EntrySetView(Nat2IntDenseArrayMap map) {
      super(map);
    }

    @Override
    public EntrySetView clone() throws CloneNotSupportedException {
      return (EntrySetView) super.clone();
    }

    @Override
    public ObjectIterator<Int2IntMap.Entry> fastIterator() {
      return new FastEntryIterator(map);
    }

    @Override
    public ObjectIterator<Int2IntMap.Entry> iterator() {
      return new EntryIterator(map);
    }
  }

  private static class FastEntryIterator implements ObjectIterator<Int2IntMap.Entry> {
    private final FastMapEntry entry;
    private final Nat2IntDenseArrayMap map;
    private int next;

    FastEntryIterator(Nat2IntDenseArrayMap map) {
      entry = new FastMapEntry(map);
      this.map = map;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next >= 0;
    }

    @Override

    public Int2IntMap.Entry next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      entry.index = next;
      next = map.nextKey(next + 1);
      return entry;
    }

    @Override
    public void remove() {
      if (entry.index == -1) {
        throw new IllegalStateException();
      }
      map.remove(entry.index);
      entry.index = -1;
    }
  }

  @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
  private static class FastMapEntry extends AbstractInt2IntMap.BasicEntry {
    private final Nat2IntDenseArrayMap map;
    int index = -1;

    public FastMapEntry(Nat2IntDenseArrayMap map) {
      this.map = map;
    }

    @Override
    public int getIntKey() {
      return index;
    }

    @Override
    public int getIntValue() {
      return map.array[index];
    }

    @Override
    public int setValue(int v) {
      int oldValue = map.array[index];
      map.array[index] = v;
      return oldValue;
    }
  }

  private static class KeySetIterator implements IntIterator {
    private final Nat2IntDenseArrayMap map;
    private int current = -1;
    private int next;

    public KeySetIterator(Nat2IntDenseArrayMap map) {
      this.map = map;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next >= 0;
    }

    @Override
    public int nextInt() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      current = next;
      next = map.nextKey(next + 1);
      return current;
    }

    @Override
    public void remove() {
      if (current == -1) {
        throw new IllegalStateException();
      }
      map.remove(current);
      current = -1;
    }
  }

  private static class KeySetView extends AbstractIntSet {
    private final Nat2IntDenseArrayMap map;

    KeySetView(Nat2IntDenseArrayMap map) {
      this.map = map;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public KeySetView clone() {
      return this;
    }

    @Override
    public void forEach(IntConsumer action) {
      for (int index = map.nextKey(0); index >= 0; index = map.nextKey(index + 1)) {
        action.accept(index);
      }
    }

    @Override
    public IntIterator iterator() {
      return new KeySetIterator(map);
    }

    @Override
    public boolean remove(int key) {
      if (!map.containsKey(key)) {
        return false;
      }
      map.remove(key);
      return true;
    }

    @Override
    public int size() {
      return map.size();
    }
  }

  @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
  private static class ValuesIterator implements IntIterator {
    private final Nat2IntDenseArrayMap map;
    private int current;
    private int next;

    ValuesIterator(Nat2IntDenseArrayMap map) {
      this.map = map;
      current = -1;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next >= 0;
    }

    @Override
    public int nextInt() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int value = map.array[next];
      current = next;
      next = map.nextKey(next + 1);
      return value;
    }

    @Override
    public void remove() {
      if (current == -1) {
        throw new IllegalStateException();
      }
      map.remove(current);
      current = -1;
    }
  }

  private static class ValuesView extends AbstractIntCollection {
    private final Nat2IntDenseArrayMap map;

    ValuesView(Nat2IntDenseArrayMap map) {
      this.map = map;
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public boolean contains(int v) {
      return map.containsValue(v);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof Collection)) {
        return false;
      }

      Collection<?> other = (Collection<?>) o;
      return other.size() == size() && containsAll(other);
    }

    @Override
    public int hashCode() {
      return HashCommon.mix(map.hashCode());
    }

    @Override
    public IntIterator iterator() {
      return new ValuesIterator(map);
    }

    @Override
    public int size() {
      return map.size();
    }
  }
}