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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntToDoubleFunction;
import javax.annotation.Nullable;

/**
 * An efficient representation of a total mapping from {0, ..., n} to R.
 *
 * <p><strong>Warning</strong>: This class uses {@link Double#NaN} to represent missing keys. Thus,
 * this value cannot be mapped.</p>
 */
@SuppressWarnings("PMD.AssignmentInOperand")
public class Nat2DoubleDenseArrayMap extends AbstractInt2DoubleMap {
    private static final long serialVersionUID = 943823872741225228L;

    private double[] array;
    private int size = 0;

    @Nullable
    private transient EntrySetView entriesView = null;

    @Nullable
    private transient KeySetView keySetView = null;

    @Nullable
    private transient ValuesView valuesView = null;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public Nat2DoubleDenseArrayMap(double[] array) {
        this.array = array;
        for (double value : array) {
            if (!isAbsent(value)) {
                size++;
            }
        }
    }

    public Nat2DoubleDenseArrayMap(int initialSize) {
        this.array = new double[initialSize];
        Arrays.fill(this.array, Double.NaN);
    }

    public Nat2DoubleDenseArrayMap(int initialSize, double initialValue) {
        checkNotAbsent(initialValue);
        this.array = new double[initialSize];
        if (initialValue != 0.0d) {
            Arrays.fill(array, initialValue);
        }
        this.size = initialSize;
    }

    public Nat2DoubleDenseArrayMap(int initialSize, IntToDoubleFunction initialValues) {
        this.array = new double[initialSize];
        for (int i = 0; i < array.length; i++) {
            double value = initialValues.applyAsDouble(i);
            checkNotAbsent(value);
            array[i] = value;
        }
        this.size = initialSize;
    }

    @SuppressWarnings("MethodMayBeStatic")
    private boolean isAbsent(double value) {
        return Double.isNaN(value);
    }

    private void checkNotAbsent(double value) {
        if (isAbsent(value)) {
            throw new IllegalArgumentException(String.format("Value %s not allowed", value));
        }
    }

    private int nextKey(int index) {
        double[] array = this.array;
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
            Arrays.fill(this.array, length, newLength, Double.NaN);
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
    public boolean containsValue(double v) {
        if (isAbsent(v)) {
            return false;
        }
        double[] array = this.array;
        for (double value : array) {
            if (value == v) {
                return true;
            }
        }
        return false;
    }

    @Override
    public double get(int key) {
        if (array.length <= key) {
            return defaultReturnValue();
        }
        double value = array[key];
        return isAbsent(value) ? defaultReturnValue() : value;
    }

    @Override
    public double getOrDefault(int key, double defaultValue) {
        if (array.length <= key) {
            return defaultValue;
        }
        double value = array[key];
        return isAbsent(value) ? defaultValue : value;
    }

    @Override
    public double put(int key, double value) {
        checkNotAbsent(value);
        double previous;
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
    public double putIfAbsent(int key, double value) {
        checkNotAbsent(value);
        double previous;
        //noinspection NestedAssignment
        if (ensureSize(key) || isAbsent(previous = array[key])) {
            array[key] = value;
            size++;
            return defaultReturnValue();
        }
        return previous;
    }

    @Override
    public double computeIfAbsent(int key, IntToDoubleFunction mappingFunction) {
        double previous;
        //noinspection NestedAssignment
        if (ensureSize(key) || isAbsent(previous = array[key])) {
            double value = mappingFunction.applyAsDouble(key);
            checkNotAbsent(value);
            array[key] = value;
            size++;
            return value;
        }
        return previous;
    }

    @Override
    public double merge(
            int key, double value, BiFunction<? super Double, ? super Double, ? extends Double> remappingFunction) {
        checkNotAbsent(value);
        double previous;
        //noinspection NestedAssignment
        if (ensureSize(key) || isAbsent(previous = array[key])) {
            assert isAbsent(array[key]);
            array[key] = value;
            size++;
            return value;
        }
        Double merge = remappingFunction.apply(previous, value);
        if (merge == null) {
            array[key] = Double.NaN;
            size--;
            return defaultReturnValue();
        }
        double mergeDouble = merge;
        checkNotAbsent(mergeDouble);
        array[key] = mergeDouble;
        return mergeDouble;
    }

    @Override
    public double mergeDouble(int key, double value, DoubleBinaryOperator remappingFunction) {
        checkNotAbsent(value);
        double previous;
        //noinspection NestedAssignment
        if (ensureSize(key) || isAbsent(previous = array[key])) {
            assert isAbsent(array[key]);
            array[key] = value;
            size++;
            return value;
        }
        double merge = remappingFunction.applyAsDouble(previous, value);
        checkNotAbsent(merge);
        array[key] = merge;
        return merge;
    }

    @Override
    public double remove(int key) {
        double previous;
        //noinspection NestedAssignment
        if (array.length <= key || isAbsent(previous = array[key])) {
            return defaultReturnValue();
        }
        array[key] = Double.NaN;
        size--;
        return previous;
    }

    public void fill(int from, int to, double value) {
        checkNotAbsent(value);
        ensureSize(to);
        Arrays.fill(array, from, to, value);
    }

    public void fill(PrimitiveIterator.OfInt iterator, double value) {
        checkNotAbsent(value);
        while (iterator.hasNext()) {
            int index = iterator.nextInt();
            ensureSize(index);
            array[index] = value;
        }
    }

    public void setAll(int from, int to, IntToDoubleFunction generator) {
        int length = array.length;
        if (length <= to) {
            int newLength = Math.max(length * 2, to + 1);
            this.array = Arrays.copyOf(this.array, newLength);
            Arrays.fill(this.array, to, newLength, Double.NaN);
        }
        double[] array = this.array;
        for (int i = from; i < to; i++) {
            double value = generator.applyAsDouble(i);
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
        Arrays.fill(array, Double.NaN);
        size = 0;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Nat2DoubleDenseArrayMap) {
            Nat2DoubleDenseArrayMap other = (Nat2DoubleDenseArrayMap) o;
            if (size != other.size) {
                return false;
            }
            // Note: Number of elements in the two arrays is the same here
            int mismatch = Arrays.mismatch(this.array, other.array);
            return mismatch == -1 || mismatch == this.array.length || mismatch == other.array.length;
        }
        return super.equals(o);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        int hash = HashCommon.mix(size);
        int elements = 0;
        int index = 0;
        while (elements < size) {
            double element = array[index];
            if (!isAbsent(element)) {
                hash ^= Double.hashCode(element) ^ HashCommon.mix(index);
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
    public DoubleCollection values() {
        if (valuesView == null) {
            valuesView = new ValuesView(this);
        }
        return valuesView;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public ObjectSet<Int2DoubleMap.Entry> int2DoubleEntrySet() {
        if (entriesView == null) {
            entriesView = new EntrySetView(this);
        }
        return entriesView;
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    private static class EntryIterator implements ObjectIterator<Int2DoubleMap.Entry> {
        private final Nat2DoubleDenseArrayMap map;
        private int current = -1;
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
        public Int2DoubleMap.Entry next() {
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

    private static final class Entry implements Int2DoubleMap.Entry {
        private final Nat2DoubleDenseArrayMap map;
        private final int index;

        Entry(Nat2DoubleDenseArrayMap map, int index) {
            this.map = map;
            this.index = index;
        }

        @Override
        public int getIntKey() {
            return index;
        }

        @Override
        public double getDoubleValue() {
            return map.array[index];
        }

        @Override
        public double setValue(double value) {
            return map.put(index, value);
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            if (o instanceof Int2DoubleMap.Entry) {
                Int2DoubleMap.Entry e = (Int2DoubleMap.Entry) o;
                return getIntKey() == e.getIntKey() && getDoubleValue() == e.getDoubleValue();
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            if (!(key instanceof Integer)) {
                return false;
            }
            Object value = e.getValue();
            if (!(value instanceof Double)) {
                return false;
            }
            return getIntKey() == (Integer) key && getDoubleValue() == (Double) value;
        }

        @Override
        public int hashCode() {
            return HashCommon.mix(index) ^ Double.hashCode(getDoubleValue());
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

    private static class FastEntryIterator implements ObjectIterator<Int2DoubleMap.Entry> {
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
        public Int2DoubleMap.Entry next() {
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
            assert index >= 0;
            return index;
        }

        @Override
        public double setValue(double v) {
            return map.put(index, v);
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

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
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
