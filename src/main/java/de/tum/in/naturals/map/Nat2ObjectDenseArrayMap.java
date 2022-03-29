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
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;

/**
 * An efficient representation of a total mapping from {0, ..., n} to objects.
 *
 * <p>This implementation does not allow {@code null} keys.</p>
 */
public class Nat2ObjectDenseArrayMap<V> extends AbstractInt2ObjectMap<V> {
  public static final int DEFAULT_SIZE = 16;
  private static final long serialVersionUID = 630710213786009957L;

  private V[] array;
  @Nullable
  private transient EntrySetView<V> entriesView = null;
  @Nullable
  private transient KeySetView keySetView = null;
  private int size = 0;
  @Nullable
  private transient ValuesView<V> valuesView = null;

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  public Nat2ObjectDenseArrayMap(V[] array) {
    this.array = array;
    for (V value : array) {
      if (!isAbsent(value)) {
        size++;
      }
    }
  }

  @SuppressWarnings({"unchecked", "SuspiciousArrayCast"})
  public Nat2ObjectDenseArrayMap(int initialSize) {
    this.array = (V[]) new Object[initialSize];
  }

  @SuppressWarnings({"unchecked", "SuspiciousArrayCast"})
  public Nat2ObjectDenseArrayMap(int initialSize, V initialValue) {
    checkNotAbsent(initialValue);
    this.array = (V[]) new Object[initialSize];
    Arrays.fill(array, initialValue);
    this.size = initialSize;
  }

  @SuppressWarnings({"unchecked", "SuspiciousArrayCast"})
  public Nat2ObjectDenseArrayMap(int initialSize, IntFunction<V> initialValues) {
    this.array = (V[]) new Object[initialSize];
    for (int i = 0; i < array.length; i++) {
      V value = initialValues.apply(i);
      checkNotAbsent(value);
      array[i] = value;
    }
    this.size = initialSize;
  }

  private void checkNotAbsent(@Nullable V value) {
    if (isAbsent(value)) {
      // noinspection ProhibitedExceptionThrown
      throw new NullPointerException("Null value not allowed"); // NOPMD
    }
  }

  private boolean ensureSize(int index) {
    if (this.array.length <= index) {
      this.array = Arrays.copyOf(this.array, Math.max(this.array.length * 2, index + 1));
      return true;
    }
    return false;
  }

  @Override
  public void clear() {
    Arrays.fill(array, null);
    size = 0;
  }

  @Override
  public boolean containsKey(int key) {
    return 0 <= key && key < array.length && !isAbsent(array[key]);
  }

  @Override
  public boolean containsValue(@Nullable Object v) {
    if (isAbsent(v)) {
      return false;
    }
    for (V value : array) {
      if (!isAbsent(value) && value.equals(v)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("NonFinalFieldReferenceInEquals")
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Nat2ObjectDenseArrayMap) {
      Nat2ObjectDenseArrayMap<?> other = (Nat2ObjectDenseArrayMap<?>) o;
      if (size != other.size) {
        return false;
      }
      if (array.length == other.array.length) {
        return Arrays.equals(array, other.array);
      }
      for (int i = 0; i < Math.min(array.length, other.array.length); i++) {
        if (!Objects.equals(array[i], other.array[i])) {
          return false;
        }
      }
      return true;
    }
    return super.equals(o);
  }

  public void fill(int from, int to, V value) {
    checkNotAbsent(value);
    ensureSize(to);
    Arrays.fill(array, from, to, value);
  }

  public void fill(PrimitiveIterator.OfInt iterator, V value) {
    checkNotAbsent(value);
    while (iterator.hasNext()) {
      int index = iterator.nextInt();
      ensureSize(index);
      array[index] = value;
    }
  }

  @Override
  public V get(int key) {
    if (array.length <= key) {
      return defaultReturnValue();
    }
    V value = array[key];
    return isAbsent(value) ? defaultReturnValue() : value;
  }

  @SuppressWarnings("NonFinalFieldReferencedInHashCode")
  @Override
  public int hashCode() {
    int hash = HashCommon.mix(size);
    int elements = 0;
    int index = 0;
    while (elements < size) {
      V element = array[index];
      if (!isAbsent(element)) {
        hash ^= element.hashCode() ^ HashCommon.mix(index);
        elements += 1;
      }
      index += 1;
    }
    return hash;
  }

  @Override
  public ObjectSet<Entry<V>> int2ObjectEntrySet() {
    if (entriesView == null) {
      entriesView = new EntrySetView<>(this);
    }
    return new EntrySetView<>(this);
  }

  private boolean isAbsent(@Nullable Object value) {
    return value == null;
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
  public V put(int key, V value) {
    checkNotAbsent(value);
    if (ensureSize(key)) {
      assert isAbsent(array[key]);
      array[key] = value;
      size++;
      return defaultReturnValue();
    }

    V previous = array[key];
    array[key] = value;
    if (isAbsent(previous)) {
      size++;
      return defaultReturnValue();
    }
    return previous;
  }

  @SuppressWarnings("AssignmentToNull")
  @Override
  public V remove(int key) {
    if (array.length <= key) {
      return defaultReturnValue();
    }
    V previous = array[key];
    if (isAbsent(previous)) {
      return defaultReturnValue();
    }
    array[key] = null;
    size--;
    return previous;
  }

  @Override
  public int size() {
    return size;
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public ObjectCollection<V> values() {
    if (valuesView == null) {
      valuesView = new ValuesView<>(this);
    }
    return valuesView;
  }

  @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
  private static class EntryIterator<V> implements ObjectIterator<Int2ObjectMap.Entry<V>> {
    private final Nat2ObjectDenseArrayMap<V> map;
    private int next;

    EntryIterator(Nat2ObjectDenseArrayMap<V> map) {
      this.map = map;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next != -1;
    }

    @Override
    public Int2ObjectMap.Entry<V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int index = next;
      next = map.nextKey(next + 1);
      assert map.containsKey(index);
      return new BasicEntry<>(index, map.array[index]);
    }
  }

  private static class EntrySetView<V>
      extends AbstractInt2ObjectEntrySet<V, Nat2ObjectDenseArrayMap<V>> {
    EntrySetView(Nat2ObjectDenseArrayMap<V> map) {
      super(map);
    }

    @Override
    public EntrySetView<V> clone() throws CloneNotSupportedException {
      return (EntrySetView<V>) super.clone();
    }

    @Override
    public ObjectIterator<Entry<V>> fastIterator() {
      return new FastEntryIterator<>(map);
    }

    @Override
    public ObjectIterator<Entry<V>> iterator() {
      return new EntryIterator<>(map);
    }
  }

  private static class FastEntryIterator<V> implements ObjectIterator<Entry<V>> {
    private final FastMapEntry<V> entry;
    private final Nat2ObjectDenseArrayMap<V> map;
    private int next;

    FastEntryIterator(Nat2ObjectDenseArrayMap<V> map) {
      entry = new FastMapEntry<>(map);
      this.map = map;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next >= 0;
    }

    @Override
    public Entry<V> next() {
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
  private static class FastMapEntry<V> extends AbstractInt2ObjectMap.BasicEntry<V> {
    int index = -1;
    private final Nat2ObjectDenseArrayMap<V> map;

    FastMapEntry(Nat2ObjectDenseArrayMap<V> map) {
      this.map = map;
    }

    @Override
    public int getIntKey() {
      return index;
    }

    @Override
    public V getValue() {
      return map.array[index];
    }

    @Override
    public V setValue(V v) {
      return map.put(index, v);
    }
  }

  private static class KeySetIterator implements IntIterator {
    private final Nat2ObjectDenseArrayMap<?> map;
    private int current = -1;
    private int next;

    KeySetIterator(Nat2ObjectDenseArrayMap<?> map) {
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
    private final Nat2ObjectDenseArrayMap<?> map;

    KeySetView(Nat2ObjectDenseArrayMap<?> map) {
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

  private static class ValuesIterator<V> implements ObjectIterator<V> {
    private final Nat2ObjectDenseArrayMap<V> map;
    private int current;
    private int next;

    ValuesIterator(Nat2ObjectDenseArrayMap<V> map) {
      this.map = map;
      current = -1;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next >= 0;
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    @Override
    public V next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      V value = map.array[next];
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

  private static class ValuesView<V> extends AbstractObjectSet<V> {
    private final Nat2ObjectDenseArrayMap<V> map;

    ValuesView(Nat2ObjectDenseArrayMap<V> map) {
      this.map = map;
    }

    @Override
    public void clear() {
      map.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ValuesView<V> clone() throws CloneNotSupportedException {
      return (ValuesView<V>) super.clone();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean contains(Object v) {
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
    public ObjectIterator<V> iterator() {
      return new ValuesIterator<>(map);
    }

    @Override
    public int size() {
      return map.size();
    }
  }
}