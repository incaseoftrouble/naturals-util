// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

class NatBitSetIterator implements IntIterator {
    private final NatBitSet set;
    private int current;
    private int next;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public NatBitSetIterator(NatBitSet set) {
        this.set = set;
        current = -1;
        next = set.nextPresentIndex(0);
    }

    @Override
    public boolean hasNext() {
        return next != -1;
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        current = next;
        next = set.nextPresentIndex(next + 1);
        return current;
    }

    @Override
    public void remove() {
        if (current == -1) {
            throw new IllegalStateException();
        }
        set.clear(current);
        current = -1;
    }
}
