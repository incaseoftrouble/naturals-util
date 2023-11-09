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

import static de.tum.in.naturals.set.NatBitSetsUtil.checkNonNegative;

import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
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
    public void clearFrom(int from) {
        checkNonNegative(from);
        clear(from, Integer.MAX_VALUE);
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
    public boolean intersects(Collection<Integer> indices) {
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
            IntIterator iterator = iterator();
            while (iterator.hasNext()) {
                int next = iterator.nextInt();
                if (!indices.contains(next)) {
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
        if (indices.isEmpty()) {
            return;
        }
        IntIterator iterator = iterator();
        while (iterator.hasNext()) {
            int next = iterator.nextInt();
            if (indices.contains(next)) {
                iterator.remove();
            }
        }
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
        @SuppressWarnings("TooBroadScope")
        int size = size();
        or(indices);
        return size < size();
    }

    @Override
    public void xor(IntCollection indices) {
        if (indices.isEmpty()) {
            return;
        }
        IntSet set = indices instanceof IntSet ? (IntSet) indices : NatBitSets.copyOf(indices);
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
}
