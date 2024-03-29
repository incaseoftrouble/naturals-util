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
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

class MutableSingletonNatBitSet extends AbstractNatBitSet {
    private static final int[] EMPTY_INTS = new int[0];
    private static final int EMPTY = Integer.MIN_VALUE;

    private int element;

    MutableSingletonNatBitSet() {
        setEmpty();
    }

    MutableSingletonNatBitSet(int element) {
        this.element = element;
    }

    private static void throwOperationUnsupported() {
        throw new UnsupportedOperationException("Singleton can hold at most one value");
    }

    @Override
    public boolean isEmpty() {
        return element == EMPTY;
    }

    @Override
    public int size() {
        return isEmpty() ? 0 : 1;
    }

    @Override
    public boolean contains(int index) {
        return !isEmpty() && index == element;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c.isEmpty()) {
            return true;
        }
        if (isEmpty()) {
            return false;
        }
        if (c.size() > 1) {
            return false;
        }
        return c.contains(element);
    }

    @Override
    public int firstInt() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return element;
    }

    @Override
    public int lastInt() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return element;
    }

    @Override
    public int nextPresentIndex(int index) {
        NatBitSetsUtil.checkNonNegative(index);
        if (isEmpty()) {
            return -1;
        }
        return index <= element ? element : -1;
    }

    @Override
    public int nextAbsentIndex(int index) {
        NatBitSetsUtil.checkNonNegative(index);
        return (!isEmpty() && index == element) ? element + 1 : index;
    }

    @Override
    public int previousPresentIndex(int index) {
        return (!isEmpty() && index >= element) ? element : -1;
    }

    @Override
    public int previousAbsentIndex(int index) {
        return (!isEmpty() && index == element) ? element - 1 : index;
    }

    @Override
    public IntStream intStream() {
        return isEmpty() ? IntStream.empty() : IntStream.of(element);
    }

    @Override
    public IntIterator iterator() {
        return new SingletonSetIterator(this);
    }

    @Override
    public void forEach(IntConsumer action) {
        if (!isEmpty()) {
            action.accept(element);
        }
    }

    @Override
    public void set(int index) {
        NatBitSetsUtil.checkNonNegative(index);
        if (isEmpty()) {
            element = index;
        } else if (index != element) {
            throwOperationUnsupported();
        }
    }

    @Override
    public void set(int index, boolean value) {
        NatBitSetsUtil.checkNonNegative(index);
        if (isEmpty()) {
            if (value) {
                setValue(index);
            }
        } else if ((index == element) != value) {
            throwOperationUnsupported();
        }
    }

    @Override
    public void set(int from, int to) {
        NatBitSetsUtil.checkRange(from, to);
        if (from == to) {
            return;
        }
        if (from + 1 != to) {
            throwOperationUnsupported();
        }
        if (isEmpty()) {
            setValue(from);
        } else if (from != element) {
            throwOperationUnsupported();
        }
    }

    @Override
    public boolean add(int index) {
        NatBitSetsUtil.checkNonNegative(index);
        if (index == element) {
            return false;
        }
        if (!isEmpty()) {
            throwOperationUnsupported();
        }
        setValue(index);
        return true;
    }

    @Override
    public void clear() {
        setEmpty();
    }

    @Override
    public void clear(int index) {
        if (index == element) {
            setEmpty();
        }
    }

    @Override
    public void clear(int from, int to) {
        NatBitSetsUtil.checkRange(from, to);
        if (isEmpty()) {
            return;
        }
        if (from <= element && element < to) {
            setEmpty();
        }
    }

    @Override
    public boolean remove(int index) {
        NatBitSetsUtil.checkNonNegative(index);
        if (isEmpty()) {
            return false;
        }
        if (index != element) {
            return false;
        }
        setEmpty();
        return true;
    }

    @Override
    public void flip(int index) {
        NatBitSetsUtil.checkNonNegative(index);
        if (isEmpty()) {
            element = index;
        } else if (index == element) {
            element = EMPTY;
        } else {
            throwOperationUnsupported();
        }
    }

    @Override
    public void flip(int from, int to) {
        NatBitSetsUtil.checkRange(from, to);
        if (from == to) {
            return;
        }
        if (from + 1 != to) {
            throwOperationUnsupported();
        }
        flip(from);
    }

    @Override
    public boolean intersects(Collection<Integer> indices) {
        return !isEmpty() && indices.contains(element);
    }

    @Override
    public MutableSingletonNatBitSet clone() {
        return (MutableSingletonNatBitSet) super.clone();
    }

    @Override
    public int[] toIntArray() {
        return isEmpty() ? EMPTY_INTS : new int[] {element};
    }

    private void setEmpty() {
        element = EMPTY;
    }

    private void setValue(int value) {
        assert value >= 0 && isEmpty();
        element = value;
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
