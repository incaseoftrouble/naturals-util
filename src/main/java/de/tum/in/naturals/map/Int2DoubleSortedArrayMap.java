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

import it.unimi.dsi.fastutil.doubles.AbstractDoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.AbstractInt2DoubleMap;
import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import javax.annotation.Nullable;

public class Int2DoubleSortedArrayMap extends AbstractInt2DoubleMap {
  private static final long serialVersionUID = 4177317104795467442L;

  @Nullable
  private transient EntrySetView entrySetView = null;
  @Nullable
  private transient KeySetView keySetView = null;
  private int[] keys;
  private int size;
  private double[] values;
  @Nullable
  private transient ValuesView valuesView = null;

  public Int2DoubleSortedArrayMap(Map<? extends Integer, ? extends Double> map) {
    if (map instanceof Int2DoubleSortedArrayMap) {
      Int2DoubleSortedArrayMap arrayMap = (Int2DoubleSortedArrayMap) map;
      size = arrayMap.size;
      keys = Arrays.copyOf(arrayMap.keys, size);
      values = Arrays.copyOf(arrayMap.values, size);
    } else {
      int size = map.size();
      keys = new int[size];
      values = new double[size];
      putAll(map);
    }
  }

  public Int2DoubleSortedArrayMap() {
    keys = IntArrays.EMPTY_ARRAY;
    values = DoubleArrays.EMPTY_ARRAY;
    size = 0;
  }

  public Int2DoubleSortedArrayMap(int capacity) {
    keys = new int[capacity];
    values = new double[capacity];
  }

  public Int2DoubleSortedArrayMap(Int2DoubleMap map) {
    this((Map<Integer, Double>) map);
  }

  public Int2DoubleSortedArrayMap(int key, double value) {
    keys = new int[] {key};
    values = new double[] {value};
  }

  @Override
  public void clear() {
    size = 0;
  }

  @Override
  public boolean containsKey(int key) {
    return keyIndex(key) >= 0;
  }

  @Override
  public boolean containsValue(double v) {
    long valueBits = Double.doubleToRawLongBits(v);
    for (int index = 0; index < keys.length; index++) {
      if (Double.doubleToRawLongBits(values[index]) == valueBits) {
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
    if (o instanceof Int2DoubleSortedArrayMap) {
      Int2DoubleSortedArrayMap other = (Int2DoubleSortedArrayMap) o;
      return size == other.size && Arrays.equals(keys, other.keys)
          && Arrays.equals(values, other.values);
    }
    return super.equals(o);
  }

  @Override
  public double get(int key) {
    int index = keyIndex(key);
    return 0 <= index ? values[index] : defRetValue;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(keys) ^ Arrays.hashCode(values);
  }

  @Override
  public ObjectSet<Entry> int2DoubleEntrySet() {
    if (entrySetView == null) {
      entrySetView = new EntrySetView(this);
    }
    return entrySetView;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  private int keyIndex(int key) {
    return Arrays.binarySearch(keys, 0, size, key);
  }

  @Override
  public IntSet keySet() {
    if (keySetView == null) {
      keySetView = new KeySetView(this);
    }
    return keySetView;
  }

  @Override
  public double put(int key, double value) {
    if (keys.length == 0) {
      keys = new int[] {key, 0};
      values = new double[] {value, 0.0d};
      size = 1;
      return defRetValue;
    }

    int index = keyIndex(key);
    assert -(size + 1) <= index && index < size : size + " " + index;

    if (index >= 0) {
      double oldValue = values[index];
      values[index] = value;
      return oldValue;
    }

    int insertionPoint = -(index + 1);
    int tailLength = size - insertionPoint;

    if (size == keys.length) {
      int newSize = size * 2;
      assert 0 < size && size < newSize;
      if (insertionPoint == size) {
        keys = Arrays.copyOf(keys, newSize);
        values = Arrays.copyOf(values, newSize);
        keys[insertionPoint] = key;
        values[insertionPoint] = value;
      } else {
        int[] newKeys = new int[newSize];
        double[] newValues = new double[newSize];
        System.arraycopy(keys, 0, newKeys, 0, insertionPoint);
        System.arraycopy(values, 0, newValues, 0, insertionPoint);
        System.arraycopy(keys, insertionPoint, newKeys, insertionPoint + 1, tailLength);
        System.arraycopy(values, insertionPoint, newValues, insertionPoint + 1, tailLength);

        keys = newKeys;
        values = newValues;
        newKeys[insertionPoint] = key;
        newValues[insertionPoint] = value;
      }
    } else if (insertionPoint == size) {
      keys[size] = key;
      values[size] = value;
    } else {
      assert insertionPoint < size && size < keys.length;
      assert 0 <= tailLength && insertionPoint + 1 + tailLength <= keys.length;

      System.arraycopy(keys, insertionPoint, keys, insertionPoint + 1, tailLength);
      System.arraycopy(values, insertionPoint, values, insertionPoint + 1, tailLength);
      keys[insertionPoint] = key;
      values[insertionPoint] = value;
    }
    size++;
    return defRetValue;
  }

  @Override
  public double remove(int key) {
    int index = keyIndex(key);
    if (index < 0) {
      return defRetValue;
    }
    double value = values[index];
    removeIndex(index);
    return value;
  }

  protected void removeIndex(int index) {
    assert index < size;
    int nextIndex = index + 1;
    if (nextIndex < size) {
      int tail = size - nextIndex;
      System.arraycopy(keys, nextIndex, keys, index, tail);
      System.arraycopy(values, nextIndex, values, index, tail);
    }
    size--;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public String toString() {
    if (isEmpty()) {
      return "{}";
    }

    StringBuilder builder = new StringBuilder();
    builder.append('{').append(keys[0]).append('=').append(values[0]);
    for (int keyIndex = 1; keyIndex < size; keyIndex++) {
      builder.append(", ").append(keys[keyIndex]).append("=>").append(values[keyIndex]);
    }
    builder.append('}');
    return builder.toString();
  }

  public void trim() {
    if (keys.length == size) {
      return;
    }
    keys = Arrays.copyOf(keys, size);
    values = Arrays.copyOf(values, size);
  }

  @Override
  public DoubleCollection values() {
    if (valuesView == null) {
      valuesView = new ValuesView(this);
    }
    return valuesView;
  }

  private static class EntryIterator implements ObjectIterator<Entry> {
    private final Int2DoubleSortedArrayMap map;
    private int index = 0;

    EntryIterator(Int2DoubleSortedArrayMap map) {
      this.map = map;
    }

    @Override
    public boolean hasNext() {
      return index < map.size();
    }

    @Override

    public MapEntry next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      MapEntry entry = new MapEntry(map, index);
      index += 1;
      return entry;
    }

    @Override
    public void remove() {
      if (index == 0) {
        throw new IllegalStateException();
      }
      index -= 1;
      map.removeIndex(index);
    }
  }

  private static class EntrySetView extends AbstractInt2DoubleEntrySet<Int2DoubleSortedArrayMap> {
    EntrySetView(Int2DoubleSortedArrayMap map) {
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
    private final Int2DoubleSortedArrayMap map;

    FastEntryIterator(Int2DoubleSortedArrayMap map) {
      entry = new FastMapEntry(map);
      this.map = map;
    }

    @Override
    public boolean hasNext() {
      return entry.index + 1 < map.size;
    }

    @Override

    public Entry next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      entry.index += 1;
      return entry;
    }

    @Override
    public void remove() {
      if (entry.index == -1) {
        throw new IllegalStateException();
      }
      map.removeIndex(entry.index);
    }
  }

  private static class FastMapEntry extends BasicInt2DoubleEntry {
    private final Int2DoubleSortedArrayMap map;
    int index = -1;

    public FastMapEntry(Int2DoubleSortedArrayMap map) {
      this.map = map;
    }

    @Override
    public double getDoubleValue() {
      return map.values[index];
    }

    @Override
    public int getIntKey() {
      return map.keys[index];
    }

    @Override
    public double setValue(double v) {
      double oldValue = map.values[index];
      map.values[index] = v;
      return oldValue;
    }
  }

  private static class KeySetView extends AbstractIntSet {
    private final Int2DoubleSortedArrayMap map;

    KeySetView(Int2DoubleSortedArrayMap map) {
      this.map = map;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public KeySetView clone() throws CloneNotSupportedException {
      return this;
    }

    @Override
    public void forEach(IntConsumer action) {
      int[] keys = map.keys;
      for (int index = 0; index < map.size; index++) {
        action.accept(keys[index]);
      }
    }

    @Override
    public IntIterator iterator() {
      return IntIterators.wrap(map.keys, 0, map.size);
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
      return map.size;
    }
  }

  private static class MapEntry extends BasicInt2DoubleEntry {
    private final int index;
    private final Int2DoubleSortedArrayMap map;

    MapEntry(Int2DoubleSortedArrayMap map, int index) {
      this.map = map;
      this.index = index;
    }

    @Override
    public double getDoubleValue() {
      return map.values[index];
    }

    @Override
    public int getIntKey() {
      return map.keys[index];
    }

    @Override
    public double setValue(double v) {
      double oldValue = map.values[index];
      map.values[index] = v;
      return oldValue;
    }
  }

  private static class ValuesIterator implements DoubleIterator {
    private final Int2DoubleSortedArrayMap map;
    private int nextIndex = 0;

    ValuesIterator(Int2DoubleSortedArrayMap map) {
      this.map = map;
    }

    @Override
    public boolean hasNext() {
      return nextIndex < map.size;
    }

    @Override
    public double nextDouble() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      double value = map.values[nextIndex];
      nextIndex += 1;
      return value;
    }

    @Override
    public void remove() {
      if (nextIndex == 0) {
        throw new IllegalStateException();
      }
      nextIndex -= 1;
      map.removeIndex(nextIndex);
    }
  }

  private  static class ValuesView extends AbstractDoubleCollection {
    private final Int2DoubleSortedArrayMap map;

    ValuesView(Int2DoubleSortedArrayMap map) {
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
      return map.hashCode() * 31;
    }

    @Override
    public DoubleIterator iterator() {
      return new ValuesIterator(map);
    }

    @Override
    public int size() {
      return map.size;
    }
  }
}
