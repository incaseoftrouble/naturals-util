// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import java.util.NoSuchElementException;

class NatBitSetBidiIterator implements IntBidirectionalIterator {
    private final NatBitSet set;
    private int previous;
    private int next;
    /** Element last returned by {@link #nextInt()} or {@link #previousInt()}, {@code -1} if none. */
    private int last = -1;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    NatBitSetBidiIterator(NatBitSet set, int start) {
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
        last = previous;
        return previous;
    }

    @Override
    public int previousInt() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        next = previous;
        previous = next == 0 ? -1 : set.previousPresentIndex(next - 1);
        last = next;
        return next;
    }

    @Override
    public void remove() {
        if (last == -1) {
            throw new IllegalStateException();
        }
        set.clear(last);
        // The removed element was whichever cursor pointed at it - re-anchor that one past the gap.
        if (previous == last) {
            previous = last == 0 ? -1 : set.previousPresentIndex(last - 1);
        }
        if (next == last) {
            next = set.nextPresentIndex(last + 1);
        }
        last = -1;
    }
}
