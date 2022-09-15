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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.IntBinaryOperator;
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
@SuppressWarnings("PMD.AssignmentInOperand")
public class Nat2IntDenseArrayMap extends AbstractInt2IntMap {
  private static final long serialVersionUID = 5185461790033343414L;
  private static final int DEFAULT_INITIAL_SIZE = 1024;


  private int[] array;
  private int size = 0;

  @Nullable
  private transient EntrySetView entriesView = null;
  @Nullable
  private transient KeySetView keySetView = null;
  @Nullable
  private transient ValuesView valuesView = null;


  public Nat2IntDenseArrayMap() {
    this(DEFAULT_INITIAL_SIZE);
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  public Nat2IntDenseArrayMap(int[] array) {
    this.array = array;
    for (int value : array) {
      if (!isAbsent(value)) {
        size++;
      }
    }
  }

  public Nat2IntDenseArrayMap(int initialSize) {
    this.array = new int[initialSize];
    Arrays.fill(this.array, Integer.MIN_VALUE);
  }

  public Nat2IntDenseArrayMap(int initialSize, int initialValue) {
    checkNotAbsent(initialValue);
    this.array = new int[initialSize];
    if (initialValue != 0) {
      Arrays.fill(array, initialValue);
    }
    this.size = initialSize;
  }

  public Nat2IntDenseArrayMap(int initialSize, IntUnaryOperator initialValues) {
    this.array = new int[initialSize];
    for (int i = 0; i < array.length; i++) {
      int value = initialValues.applyAsInt(i);
      checkNotAbsent(value);
      array[i] = value;
    }
    this.size = initialSize;
  }


  private boolean isAbsent(int value) {
    return value == Integer.MIN_VALUE;
  }

  private void checkNotAbsent(int value) {
    if (isAbsent(value)) {
      throw new IllegalArgumentException(String.format("Value %d not allowed", value));
    }
  }

  private int nextKey(int index) {
    int[] array = this.array;
    for (int i = index; i < array.length; i++) {
      if (!isAbsent(array[i])) {
        return i;
      }
    }
    return -1;
  }

  private boolean ensureSize(int index) {
    int length = this.array.length;
    if (length <= index) {
      int newLength = Math.max(length * 2, index + 1);
      this.array = Arrays.copyOf(this.array, newLength);
      Arrays.fill(this.array, length, newLength, Integer.MIN_VALUE);
      return true;
    }
    return false;
  }


  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
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
    int[] array = this.array;
    for (int value : array) {
      if (value == v) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int get(int key) {
    if (array.length <= key) {
      return defaultReturnValue();
    }
    int value = array[key];
    return isAbsent(value) ? defaultReturnValue() : value;
  }

  @Override
  public int getOrDefault(int key, int defaultValue) {
    if (array.length <= key) {
      return defaultValue;
    }
    int value = array[key];
    return isAbsent(value) ? defaultValue : value;
  }


  @Override
  public int put(int key, int value) {
    checkNotAbsent(value);
    int previous;
    //noinspection NestedAssignment
    if (ensureSize(key) || isAbsent(previous = array[key])) {
      assert isAbsent(array[key]);
      array[key] = value;
      size++;
      return defaultReturnValue();
    }
    array[key] = value;
    return previous;
  }

  @Override
  public int putIfAbsent(int key, int value) {
    checkNotAbsent(value);
    int previous;
    //noinspection NestedAssignment
    if (ensureSize(key) || isAbsent(previous = array[key])) {
      array[key] = value;
      size++;
      return defaultReturnValue();
    }
    return previous;
  }

  @Override
  public int computeIfAbsent(int key, IntUnaryOperator mappingFunction) {
    int previous;
    //noinspection NestedAssignment
    if (ensureSize(key) || isAbsent(previous = array[key])) {
      int value = mappingFunction.applyAsInt(key);
      checkNotAbsent(value);
      array[key] = value;
      size++;
      return value;
    }
    return previous;
  }

  @Override
  public int merge(int key, int value, BiFunction<? super Integer, ? super Integer, ? extends Integer> remappingFunction) {
    checkNotAbsent(value);
    int previous;
    //noinspection NestedAssignment
    if (ensureSize(key) || isAbsent(previous = array[key])) {
      assert isAbsent(array[key]);
      array[key] = value;
      size++;
      return value;
    }
    Integer merge = remappingFunction.apply(previous, value);
    if (merge == null) {
      array[key] = Integer.MIN_VALUE;
      size--;
      return defaultReturnValue();
    }
    int mergeInt = merge;
    checkNotAbsent(mergeInt);
    array[key] = mergeInt;
    return mergeInt;
  }

  @Override
  public int mergeInt(int key, int value, IntBinaryOperator remappingFunction) {
    checkNotAbsent(value);
    int previous;
    //noinspection NestedAssignment
    if (ensureSize(key) || isAbsent(previous = array[key])) {
      assert isAbsent(array[key]);
      array[key] = value;
      size++;
      return value;
    }
    int merge = remappingFunction.applyAsInt(previous, value);
    checkNotAbsent(merge);
    array[key] = merge;
    return merge;
  }

  @Override
  public int remove(int key) {
    int previous;
    //noinspection NestedAssignment
    if (array.length <= key || isAbsent(previous = array[key])) {
      return defaultReturnValue();
    }
    array[key] = Integer.MIN_VALUE;
    size--;
    return previous;
  }


  public void fill(int from, int to, int value) {
    checkNotAbsent(value);
    ensureSize(to);
    Arrays.fill(array, from, to, value);
  }

  public void fill(PrimitiveIterator.OfInt iterator, int value) {
    checkNotAbsent(value);
    int[] array = this.array;
    while (iterator.hasNext()) {
      int index = iterator.nextInt();
      ensureSize(index);
      array[index] = value;
    }
  }

  public void setAll(int from, int to, IntUnaryOperator generator) {
    int length = array.length;
    if (length <= to) {
      int newLength = Math.max(length * 2, to + 1);
      this.array = Arrays.copyOf(this.array, newLength);
      Arrays.fill(this.array, to, newLength, Integer.MIN_VALUE);
    }
    int[] array = this.array;
    for (int i = from; i < to; i++) {
      int value = generator.applyAsInt(i);
      assert !isAbsent(value);
      if (isAbsent(array[i])) {
        size += 1;
      }
      array[i] = value;
    }
  }

  @Override
  public void clear() {
    if (isEmpty()) {
      return;
    }
    Arrays.fill(array, Integer.MIN_VALUE);
    size = 0;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Nat2IntDenseArrayMap) {
      Nat2IntDenseArrayMap other = (Nat2IntDenseArrayMap) o;
      if (size != other.size) {
        return false;
      }
      // Note: Number of elements in the two arrays is the same here
      int mismatch = Arrays.mismatch(this.array, other.array);
      return mismatch == -1 || mismatch == this.array.length || mismatch == other.array.length;
    }
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    int hash = HashCommon.mix(size);
    int elements = 0;
    int index = 0;
    int[] array = this.array;
    // Note: Cannot use Arrays.hashCode here, since the actual length of the array should not change the hash
    while (elements < size) {
      int element = array[index];
      if (!isAbsent(element)) {
        hash ^= HashCommon.mix(element) ^ HashCommon.mix(index);
        elements += 1;
      }
      index += 1;
    }
    return hash;
  }


  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public IntSet keySet() {
    if (keySetView == null) {
      keySetView = new KeySetView(this);
    }
    return keySetView;
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public IntCollection values() {
    if (valuesView == null) {
      valuesView = new ValuesView(this);
    }
    return valuesView;
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public ObjectSet<Int2IntMap.Entry> int2IntEntrySet() {
    if (entriesView == null) {
      entriesView = new EntrySetView(this);
    }
    return entriesView;
  }

  @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
  private static class EntryIterator implements ObjectIterator<Int2IntMap.Entry> {
    private final Nat2IntDenseArrayMap map;
    private int current = -1;
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
      current = next;
      next = map.nextKey(next + 1);
      assert map.containsKey(current);
      return new Entry(map, current);
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

  private static final class Entry implements Int2IntMap.Entry {
    private final Nat2IntDenseArrayMap map;
    private final int index;

    Entry(Nat2IntDenseArrayMap map, int index) {
      this.map = map;
      this.index = index;
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
    public int setValue(int value) {
      return map.put(index, value);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      if (o instanceof Int2IntMap.Entry) {
        Int2IntMap.Entry e = (Int2IntMap.Entry) o;
        return getIntKey() == e.getIntKey() && getIntValue() == e.getIntValue();
      }
      Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
      Object key = e.getKey();
      if (!(key instanceof Integer)) {
        return false;
      }
      Object value = e.getValue();
      if (!(value instanceof Integer)) {
        return false;
      }
      return getIntKey() == (Integer) key && getIntValue() == (Integer) value;
    }

    @Override
    public int hashCode() {
      return HashCommon.mix(index) ^ HashCommon.mix(getIntValue());
    }
  }

  private static class EntrySetView extends AbstractInt2IntEntrySet<Nat2IntDenseArrayMap> {
    EntrySetView(Nat2IntDenseArrayMap map) {
      super(map);
    }

    @Override
    public void clear() {
      map.clear();
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
      assert index >= 0;
      return index;
    }

    @Override
    public int getIntValue() {
      return map.array[index];
    }

    @Override
    public int setValue(int v) {
      return map.put(index, v);
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
    public void clear() {
      map.clear();
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