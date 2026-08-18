// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

public abstract class AbstractNatBitSet extends AbstractIntSet implements NatBitSet {
    @Override
    public int firstInt() {
        int firstPresent = nextPresentIndex(0);
        if (firstPresent == -1) {
            throw new NoSuchElementException();
        }
        return firstPresent;
    }

    @Override
    public int lastInt() {
        int lastPresent = previousPresentIndex(Integer.MAX_VALUE);
        if (lastPresent == -1) {
            throw new NoSuchElementException();
        }
        return lastPresent;
    }

    @Override
    public boolean add(int index) {
        if (contains(index)) {
            return false;
        }
        set(index);
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        if (c.isEmpty()) {
            return false;
        }
        int size = size();
        if (c instanceof IntCollection) {
            or((IntCollection) c);
        } else {
            c.forEach(this::set);
        }
        return size() > size;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return super.retainAll(c);
    }

    @Override
    public void clearFrom(int from) {
        clear(Math.max(0, from), Integer.MAX_VALUE);
    }

    @Override
    public boolean remove(int index) {
        if (!contains(index)) {
            return false;
        }
        clear(index);
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return super.removeAll(c);
    }

    @Override
    public boolean intersects(Collection<Integer> indices) {
        if (isEmpty() || indices.isEmpty()) {
            return false;
        }
        if (indices instanceof IntCollection) {
            return IntIterators.any(((IntCollection) indices).iterator(), this::contains);
        }
        return NatBitSet.super.intersects(indices);
    }

    @Override
    public void and(IntCollection indices) {
        if (indices.isEmpty()) {
            clear();
        } else {
            IntSet reference = indices instanceof IntSet ? (IntSet) indices : new IntOpenHashSet(indices);
            IntIterator iterator = iterator();
            while (iterator.hasNext()) {
                int next = iterator.nextInt();
                if (!reference.contains(next)) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public boolean retainAll(IntCollection indices) {
        if (isEmpty()) {
            return false;
        }
        if (indices.isEmpty()) {
            clear();
            return true;
        }
        int size = size();
        and(indices);
        return size() < size;
    }

    @Override
    public void andNot(IntCollection indices) {
        indices.forEach(this::clear);
    }

    @Override
    public boolean removeAll(IntCollection indices) {
        if (isEmpty() || indices.isEmpty()) {
            return false;
        }
        int size = size();
        andNot(indices);
        return size() < size;
    }

    @Override
    public void or(IntCollection indices) {
        if (indices.isEmpty()) {
            return;
        }
        indices.forEach((IntConsumer) this::set);
    }

    @Override
    public boolean addAll(IntCollection indices) {
        if (indices.isEmpty()) {
            return false;
        }
        int size = size();
        or(indices);
        return size < size();
    }

    @Override
    public void xor(IntCollection indices) {
        if (indices.isEmpty()) {
            return;
        }
        IntSet set = indices instanceof IntSet ? (IntSet) indices : new IntOpenHashSet(indices);
        set.forEach((IntConsumer) this::flip);
    }

    @Override
    public AbstractNatBitSet clone() {
        try {
            return (AbstractNatBitSet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public abstract IntIterator iterator();
}
