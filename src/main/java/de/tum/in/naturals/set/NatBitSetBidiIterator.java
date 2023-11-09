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

import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import java.util.NoSuchElementException;

class NatBitSetBidiIterator implements IntBidirectionalIterator {
    private final NatBitSet set;
    private int previous;
    private int next;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public NatBitSetBidiIterator(NatBitSet set) {
        this.set = set;
        previous = -1;
        next = set.nextPresentIndex(0);
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public NatBitSetBidiIterator(NatBitSet set, int start) {
        this.set = set;
        if (start == 0) {
            previous = -1;
            next = set.nextPresentIndex(0);
        } else {
            previous = set.previousPresentIndex(start - 1);
            next = set.nextPresentIndex(start);
        }
    }

    @Override
    public boolean hasNext() {
        return next != -1;
    }

    @Override
    public boolean hasPrevious() {
        return previous != -1;
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        previous = next;
        next = set.nextPresentIndex(next + 1);
        return previous;
    }

    @Override
    public int previousInt() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        next = previous;
        if (next == 0) {
            previous = -1;
        } else {
            previous = set.previousPresentIndex(next - 1);
        }
        return next;
    }

    @Override
    public void remove() {
        if (previous == -1) {
            throw new IllegalStateException();
        }
        set.clear(previous);
        previous = set.previousPresentIndex(previous - 1);
    }
}
