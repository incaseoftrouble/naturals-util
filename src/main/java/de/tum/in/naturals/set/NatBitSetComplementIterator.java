// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

class NatBitSetComplementIterator implements IntIterator {
    private final int length;
    private final NatBitSet set;
    private int current;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public NatBitSetComplementIterator(NatBitSet set, int length) {
        this.set = set;
        this.length = length;
        current = set.nextAbsentIndex(0);
    }

    @Override
    public boolean hasNext() {
        return current < length;
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        int result = current;
        current = set.nextAbsentIndex(current + 1);
        return result;
    }

    // No remove(): removing from a complement means adding to the underlying set, which is too
    // surprising to offer. Mutate the set directly instead.
}
