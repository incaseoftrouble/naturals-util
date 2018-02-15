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
import it.unimi.dsi.fastutil.doubles.AbstractDoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.AbstractInt2DoubleMap;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.IntToDoubleFunction;
import javax.annotation.Nullable;

/**
 * An efficient representation of a total mapping from {0, ..., n} to R.
 *
 * <p><strong>Warning</strong>: This class uses {@link Double#NaN} to represent missing keys. Thus,
 * this value cannot be mapped.</p>
 */
public class Nat2DoubleDenseArrayMap extends AbstractInt2DoubleMap {
  private static final long serialVersionUID = 943823872741225228L;

  private final double[] array;
  @Nullable
  private transient EntrySetView entriesView = null;
  @Nullable
  private transient KeySetView keySetView = null;
  private int size = 0;
  @Nullable
  private transient ValuesView valuesView = null;

  public Nat2DoubleDenseArrayMap(double[] array) {
    //noinspection AssignmentToCollectionOrArrayFieldFromParameter
    this.array = array;
    for (double value : array) {
      if (!isAbsent(value)) {
        size++;
      }
    }
  }

  public Nat2DoubleDenseArrayMap(int size) {
    this.array = new double[size];
    Arrays.fill(this.array, Double.NaN);
  }

  public Nat2DoubleDenseArrayMap(int size, double initialValue) {
    checkNotAbsent(initialValue);
    this.array = new double[size];
    if (initialValue != 0d) {
      Arrays.fill(array, initialValue);
    }
    this.size = size;
  }

  public Nat2DoubleDenseArrayMap(int size, IntToDoubleFunction initialValues) {
    this.array = new double[size];
    for (int i = 0; i < array.length; i++) {
      double value = initialValues.applyAsDouble(i);
      checkNotAbsent(value);
      array[i] = value;
    }
    this.size = size;
  }

  private void checkNotAbsent(double value) {
    if (isAbsent(value)) {
      throw new IllegalArgumentException(String.format("Value %s not allowed", value));
    }
  }

  @Override
  public void clear() {
    Arrays.fill(array, Double.NaN);
    size = 0;
  }

  @Override
  public boolean containsKey(int key) {
    return 0 <= key && key < array.length && !isAbsent(array[key]);
  }

  @Override
  public boolean containsValue(double v) {
    if (isAbsent(v)) {
      return false;
    }
    for (double value : array) {
      if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(v)) {
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
    if (o instanceof Nat2DoubleDenseArrayMap) {
      Nat2DoubleDenseArrayMap other = (Nat2DoubleDenseArrayMap) o;
      return size == other.size && Arrays.equals(array, other.array);
    }
    return super.equals(o);
  }

  public void fill(int from, int to, double value) {
    checkNotAbsent(value);
    Arrays.fill(array, from, to, value);
  }

  public void fill(PrimitiveIterator.OfInt iterator, double value) {
    while (iterator.hasNext()) {
      array[iterator.next()] = value;
    }
  }

  @Override
  public double get(int key) {
    double value = array[key];
    return isAbsent(value) ? defaultReturnValue() : value;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(array) ^ HashCommon.mix(size);
  }

  @Override
  public ObjectSet<Int2DoubleMap.Entry> int2DoubleEntrySet() {
    if (entriesView == null) {
      entriesView = new EntrySetView(this);
    }
    return new EntrySetView(this);
  }

  private boolean isAbsent(double value) {
    return Double.isNaN(value);
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
  public double put(int key, double value) {
    checkNotAbsent(value);
    double previous = array[key];
    array[key] = value;
    if (isAbsent(previous)) {
      size++;
      return defaultReturnValue();
    }
    return previous;
  }

  @Override
  public double remove(int key) {
    double previous = array[key];
    if (isAbsent(previous)) {
      return defaultReturnValue();
    }
    array[key] = Double.NaN;
    size--;
    return previous;
  }

  @Override
  public int size() {
    return size;
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public DoubleCollection values() {
    if (valuesView == null) {
      valuesView = new ValuesView(this);
    }
    return valuesView;
  }

  private static class EntryIterator implements ObjectIterator<Entry> {
    private final Nat2DoubleDenseArrayMap map;
    private int next;

    EntryIterator(Nat2DoubleDenseArrayMap map) {
      this.map = map;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next != -1;
    }

    @Override
    public Entry next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      int index = next;
      next = map.nextKey(next + 1);
      assert map.containsKey(index);
      return new BasicEntry(index, map.array[index]);
    }
  }

  private static class EntrySetView extends AbstractInt2DoubleEntrySet<Nat2DoubleDenseArrayMap> {
    EntrySetView(Nat2DoubleDenseArrayMap map) {
      super(map);
    }

    @Override
    public EntrySetView clone() throws CloneNotSupportedException {
      return (EntrySetView) super.clone();
    }

    @Override
    public ObjectIterator<Int2DoubleMap.Entry> fastIterator() {
      return new FastEntryIterator(map);
    }

    @Override
    public ObjectIterator<Int2DoubleMap.Entry> iterator() {
      return new EntryIterator(map);
    }
  }

  private static class FastEntryIterator implements ObjectIterator<Entry> {
    private final FastMapEntry entry;
    private final Nat2DoubleDenseArrayMap map;
    private int next;

    FastEntryIterator(Nat2DoubleDenseArrayMap map) {
      entry = new FastMapEntry(map);
      this.map = map;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next >= 0;
    }

    @Override

    public Entry next() {
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

  private static class FastMapEntry extends AbstractInt2DoubleMap.BasicEntry {
    private final Nat2DoubleDenseArrayMap map;
    int index = -1;

    public FastMapEntry(Nat2DoubleDenseArrayMap map) {
      this.map = map;
    }

    @Override
    public double getDoubleValue() {
      return map.array[index];
    }

    @Override
    public int getIntKey() {
      return index;
    }

    @Override
    public double setValue(double v) {
      double oldValue = map.array[index];
      map.array[index] = v;
      return oldValue;
    }
  }

  private static class KeySetIterator implements IntIterator {
    private final Nat2DoubleDenseArrayMap map;
    private int current = -1;
    private int next;

    public KeySetIterator(Nat2DoubleDenseArrayMap map) {
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
    private final Nat2DoubleDenseArrayMap map;

    KeySetView(Nat2DoubleDenseArrayMap map) {
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

  private static class ValuesIterator implements DoubleIterator {
    private final Nat2DoubleDenseArrayMap map;
    private int current;
    private int next;

    ValuesIterator(Nat2DoubleDenseArrayMap map) {
      this.map = map;
      current = -1;
      next = map.nextKey(0);
    }

    @Override
    public boolean hasNext() {
      return next >= 0;
    }

    @Override
    public double nextDouble() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      double value = map.array[next];
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

  private static class ValuesView extends AbstractDoubleCollection {
    private final Nat2DoubleDenseArrayMap map;

    ValuesView(Nat2DoubleDenseArrayMap map) {
      this.map = map;
    }

    @Override
    public void clear() {
      map.clear();
    }

    @Override
    public boolean contains(double v) {
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
    public DoubleIterator iterator() {
      return new ValuesIterator(map);
    }

    @Override
    public int size() {
      return map.size();
    }
  }
}
