// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

class NatBitSetComplementReverseIterator implements IntIterator {
    private final NatBitSet set;
    private int current;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    NatBitSetComplementReverseIterator(NatBitSet set, int length) {
        this.set = set;
        // The domain is [0, length), so the search starts at length - 1, and index 0 has no predecessor
        current = length == 0 ? -1 : set.previousAbsentIndex(length - 1);
    }

    @Override
    public boolean hasNext() {
        return current != -1;
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        int result = current;
        current = result == 0 ? -1 : set.previousAbsentIndex(result - 1);
        return result;
    }

    // No remove(): see NatBitSetComplementIterator.
}
