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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;

/**
 * An efficient representation of a total mapping from {0, ..., n} to objects.
 *
 * <p>This implementation does not allow {@code null} keys.</p>
 */
@SuppressWarnings("AssignmentToNull")
public class Nat2ObjectDenseArrayMap<V> extends AbstractInt2ObjectMap<V> {
  private static final long serialVersionUID = 630710213786009957L;

  private final V[] array;
  @Nullable
  private transient EntrySetView<V> entriesView = null;
  @Nullable
  private transient KeySetView keySetView = null;
  private int size = 0;
  @Nullable
  private transient ValuesView<V> valuesView = null;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public Nat2ObjectDenseArrayMap(V[] array) {
    //noinspection AssignmentToCollectionOrArrayFieldFromParameter
    this.array = array;
    for (V value : array) {
      if (!isAbsent(value)) {
        size++;
      }
    }
  }

  @SuppressWarnings("unchecked")
  public Nat2ObjectDenseArrayMap(int size) {
    this.array = (V[]) new Object[size];
    Arrays.fill(this.array, Integer.MIN_VALUE);
  }

  @SuppressWarnings("unchecked")
  public Nat2ObjectDenseArrayMap(int size, V initialValue) {
    checkNotAbsent(initialValue);
    this.array = (V[]) new Object[size];
    Arrays.fill(array, initialValue);
    this.size = size;
  }

  @SuppressWarnings("unchecked")
  public Nat2ObjectDenseArrayMap(int size, IntFunction<V> initialValues) {
    this.array = (V[]) new Object[size];
    for (int i = 0; i < array.length; i++) {
      V value = initialValues.apply(i);
      checkNotAbsent(value);
      array[i] = value;
    }
    this.size = size;
  }

  private void checkNotAbsent(@Nullable V value) {
    if (isAbsent(value)) {
      //noinspection ProhibitedExceptionThrown
      throw new NullPointerException("Null value not allowed");
    }
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Nat2ObjectDenseArrayMap) {
      Nat2ObjectDenseArrayMap other = (Nat2ObjectDenseArrayMap) o;
      return size == other.size && Arrays.deepEquals(array, other.array);
    }
    return super.equals(o);
  }

  public void fill(int from, int to, V value) {
    checkNotAbsent(value);
    Arrays.fill(array, from, to, value);
  }

  public void fill(PrimitiveIterator.OfInt iterator, V value) {
    checkNotAbsent(value);
    while (iterator.hasNext()) {
      array[iterator.next()] = value;
    }
  }

  @Override
  public V get(int key) {
    V value = array[key];
    return isAbsent(value) ? defaultReturnValue() : value;
  }

  @Override
  public int hashCode() {
    return Arrays.deepHashCode(array) ^ HashCommon.mix(size);
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
    V previous = array[key];
    array[key] = value;
    if (isAbsent(previous)) {
      size++;
      return defaultReturnValue();
    }
    return previous;
  }

  @Override
  public V remove(int key) {
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

  @Override
  public ObjectCollection<V> values() {
    if (valuesView == null) {
      valuesView = new ValuesView<>(this);
    }
    return valuesView;
  }

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

  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  private static class FastMapEntry<V> extends AbstractInt2ObjectMap.BasicEntry<V> {
    private final Nat2ObjectDenseArrayMap<V> map;
    int index = -1;

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
      V oldValue = map.array[index];
      map.array[index] = v;
      return oldValue;
    }
  }

  private static class KeySetIterator implements IntIterator {
    private final Nat2ObjectDenseArrayMap map;
    private int current = -1;
    private int next;

    KeySetIterator(Nat2ObjectDenseArrayMap map) {
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
    private final Nat2ObjectDenseArrayMap map;

    KeySetView(Nat2ObjectDenseArrayMap map) {
      this.map = map;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public KeySetView clone() throws CloneNotSupportedException {
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

    @Override
    public ValuesView clone() throws CloneNotSupportedException {
      return (ValuesView) super.clone();
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