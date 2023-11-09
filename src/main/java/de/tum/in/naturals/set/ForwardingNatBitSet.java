/*
 * Copyright (C) 2019 Tobias Meggendorfer
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

import de.tum.in.naturals.bitset.RoaringBitmaps;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Collection;
import java.util.function.IntConsumer;
import org.roaringbitmap.RoaringBitmap;

class ForwardingNatBitSet extends AbstractNatBitSet {
    private final IntSortedSet delegate;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public ForwardingNatBitSet(IntSortedSet delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean add(int key) {
        return delegate.add(key);
    }

    @Override
    public boolean addAll(IntCollection o) {
        return delegate.addAll(o);
    }

    @Override
    public void and(IntCollection o) {
        retainAll(o);
    }

    @Override
    public void andNot(IntCollection o) {
        removeAll(o);
    }

    @Override
    public void clear(int index) {
        remove(index);
    }

    @Override
    public void clear(int from, int to) {
        IntIterators.fromTo(from, to).forEachRemaining((IntConsumer) delegate::remove);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void clearFrom(int from) {
        delegate.tailSet(from).clear();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ForwardingNatBitSet clone() {
        return new ForwardingNatBitSet(new IntAVLTreeSet(delegate));
    }

    @Override
    public boolean contains(int key) {
        return delegate.contains(key);
    }

    @Override
    public boolean containsAll(IntCollection o) {
        return delegate.containsAll(o);
    }

    @SuppressWarnings("com.haulmont.jpb.EqualsDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int firstInt() {
        return delegate.firstInt();
    }

    @Override
    public void flip(int index) {
        if (!delegate.remove(index)) {
            delegate.add(index);
        }
    }

    @Override
    public void flip(int from, int to) {
        IntIterators.fromTo(from, to).forEachRemaining((IntConsumer) this::flip);
    }

    @Override
    public void forEach(IntConsumer action) {
        delegate.forEach(action);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean intersects(Collection<Integer> o) {
        return IntIterators.any(delegate.iterator(), o::contains);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public IntIterator iterator() {
        return delegate.iterator();
    }

    @Override
    public int lastInt() {
        return delegate.lastInt();
    }

    @Override
    public int nextAbsentIndex(int index) {
        // TODO Binary search
        int i = index;
        while (delegate.contains(i)) {
            i++;
        }
        return i;
    }

    @Override
    public int nextPresentIndex(int index) {
        IntSortedSet tail = delegate.tailSet(index);
        return tail.isEmpty() ? -1 : tail.firstInt();
    }

    @Override
    public void or(IntCollection o) {
        addAll(o);
    }

    @Override
    public int previousAbsentIndex(int index) {
        // TODO Binary search
        int i = index;
        while (i > -1 && delegate.contains(i)) {
            i--;
        }
        return i;
    }

    @Override
    public int previousPresentIndex(int index) {
        IntSortedSet tail = delegate.headSet(index);
        return tail.isEmpty() ? -1 : tail.lastInt();
    }

    @Override
    public boolean remove(int k) {
        return delegate.remove(k);
    }

    @Override
    public boolean removeAll(IntCollection o) {
        return delegate.removeAll(o);
    }

    @Override
    public boolean retainAll(IntCollection o) {
        return delegate.retainAll(o);
    }

    @Override
    public void set(int index) {
        add(index);
    }

    @Override
    public void set(int index, boolean value) {
        if (value) {
            add(index);
        } else {
            remove(index);
        }
    }

    @Override
    public void set(int from, int to) {
        IntIterators.fromTo(from, to).forEachRemaining((IntConsumer) delegate::add);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public int[] toArray(int[] a) {
        return delegate.toArray(a);
    }

    @Override
    public int[] toIntArray() {
        return delegate.toIntArray();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void xor(IntCollection o) {
        // TODO This is expensive!
        RoaringBitmap our = RoaringBitmaps.of(this);
        RoaringBitmap other = RoaringBitmaps.of(o);

        clear();
        our.xor(other);
        our.forEach((org.roaringbitmap.IntConsumer) this::set);
    }
}
