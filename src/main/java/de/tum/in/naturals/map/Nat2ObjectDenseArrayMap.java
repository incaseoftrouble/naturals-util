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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;

/**
 * An efficient representation of a total mapping from {0, ..., n} to objects.
 *
 * <p>This implementation does not allow {@code null} keys.</p>
 */
@SuppressWarnings("PMD.AssignmentInOperand")
public class Nat2ObjectDenseArrayMap<V> extends AbstractInt2ObjectMap<V> {
    public static final int DEFAULT_SIZE = 16;
    private static final long serialVersionUID = 630710213786009957L;

    private V[] array;
    private int size = 0;

    @Nullable
    private transient EntrySetView<V> entriesView = null;

    @Nullable
    private transient KeySetView keySetView = null;

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

    private boolean isAbsent(@Nullable Object value) {
        return value == null;
    }

    private void checkNotAbsent(@Nullable V value) {
        if (isAbsent(value)) {
            // noinspection ProhibitedExceptionThrown
            throw new NullPointerException("Null value not allowed"); // NOPMD
        }
    }

    private int nextKey(int index) {
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
            this.array = Arrays.copyOf(this.array, Math.max(length * 2, index + 1));
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
    public boolean containsValue(@Nullable Object v) {
        if (isAbsent(v)) {
            return false;
        }
        V[] array = this.array;
        for (V value : array) {
            if (!isAbsent(value) && value.equals(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(int key) {
        if (array.length <= key) {
            return defaultReturnValue();
        }
        V value = array[key];
        return isAbsent(value) ? defaultReturnValue() : value;
    }

    @Override
    public V getOrDefault(int key, V defaultValue) {
        if (array.length <= key) {
            return defaultValue;
        }
        V value = array[key];
        return isAbsent(value) ? defaultValue : value;
    }

    @Override
    public V put(int key, V value) {
        checkNotAbsent(value);
        V previous;
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
    public V putIfAbsent(int key, V value) {
        checkNotAbsent(value);
        V previous;
        //noinspection NestedAssignment
        if (ensureSize(key) || isAbsent(previous = array[key])) {
            array[key] = value;
            size++;
            return defaultReturnValue();
        }
        return previous;
    }

    @Override
    public V computeIfAbsent(int key, IntFunction<? extends V> mappingFunction) {
        V previous;
        //noinspection NestedAssignment
        if (ensureSize(key) || isAbsent(previous = array[key])) {
            V value = mappingFunction.apply(key);
            checkNotAbsent(value);
            array[key] = value;
            size++;
            return value;
        }
        return previous;
    }

    @Override
    public V merge(int key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        checkNotAbsent(value);
        V previous;
        //noinspection NestedAssignment
        if (ensureSize(key) || isAbsent(previous = array[key])) {
            assert isAbsent(array[key]);
            array[key] = value;
            size++;
            return value;
        }
        V merge = remappingFunction.apply(previous, value);
        if (merge == null) {
            //noinspection AssignmentToNull
            array[key] = null;
            size--;
            return defaultReturnValue();
        }
        checkNotAbsent(merge);
        array[key] = merge;
        return merge;
    }

    @SuppressWarnings("AssignmentToNull")
    @Override
    public V remove(int key) {
        V previous;
        //noinspection NestedAssignment
        if (array.length <= key || isAbsent(previous = array[key])) {
            return defaultReturnValue();
        }
        array[key] = null;
        size--;
        return previous;
    }

    public void fill(int from, int to, V value) {
        checkNotAbsent(value);
        ensureSize(to);
        Arrays.fill(array, from, to, value);
    }

    public void fill(PrimitiveIterator.OfInt iterator, V value) {
        checkNotAbsent(value);
        V[] array = this.array;
        while (iterator.hasNext()) {
            int index = iterator.nextInt();
            ensureSize(index);
            array[index] = value;
        }
    }

    public void setAll(int from, int to, IntFunction<V> generator) {
        ensureSize(to);
        V[] array = this.array;
        for (int i = from; i < to; i++) {
            V value = generator.apply(i);
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
        Arrays.fill(array, null);
        size = 0;
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
        V[] array = this.array;
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
    public ObjectCollection<V> values() {
        if (valuesView == null) {
            valuesView = new ValuesView<>(this);
        }
        return valuesView;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public ObjectSet<Int2ObjectMap.Entry<V>> int2ObjectEntrySet() {
        if (entriesView == null) {
            entriesView = new EntrySetView<>(this);
        }
        return entriesView;
    }

    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    private static class EntryIterator<V> implements ObjectIterator<Int2ObjectMap.Entry<V>> {
        private final Nat2ObjectDenseArrayMap<V> map;
        private int current = -1;
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
            current = next;
            next = map.nextKey(next + 1);
            assert map.containsKey(current);
            return new Entry<>(map, current);
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

    private static final class Entry<V> implements Int2ObjectMap.Entry<V> {
        private final Nat2ObjectDenseArrayMap<V> map;
        private final int index;

        Entry(Nat2ObjectDenseArrayMap<V> map, int index) {
            this.map = map;
            this.index = index;
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
        public V setValue(V value) {
            return map.put(index, value);
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            if (o instanceof Int2ObjectMap.Entry) {
                Int2ObjectMap.Entry<?> e = (Int2ObjectMap.Entry<?>) o;
                return getIntKey() == e.getIntKey() && Objects.equals(getValue(), e.getValue());
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            return key instanceof Integer && getIntKey() == (Integer) key && Objects.equals(getValue(), e.getValue());
        }

        @Override
        public int hashCode() {
            return HashCommon.mix(index) ^ getValue().hashCode();
        }
    }

    private static class EntrySetView<V> extends AbstractInt2ObjectEntrySet<V, Nat2ObjectDenseArrayMap<V>> {
        EntrySetView(Nat2ObjectDenseArrayMap<V> map) {
            super(map);
        }

        @Override
        public EntrySetView<V> clone() throws CloneNotSupportedException {
            return (EntrySetView<V>) super.clone();
        }

        @Override
        public ObjectIterator<Int2ObjectMap.Entry<V>> fastIterator() {
            return new FastEntryIterator<>(map);
        }

        @Override
        public ObjectIterator<Int2ObjectMap.Entry<V>> iterator() {
            return new EntryIterator<>(map);
        }
    }

    private static class FastEntryIterator<V> implements ObjectIterator<Int2ObjectMap.Entry<V>> {
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
        public Int2ObjectMap.Entry<V> next() {
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
