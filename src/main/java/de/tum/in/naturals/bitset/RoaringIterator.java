// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;
import org.roaringbitmap.PeekableIntIterator;

final class RoaringIterator implements IntIterator {
    private final PeekableIntIterator iterator;

    public RoaringIterator(PeekableIntIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public int nextInt() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return iterator.next();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }
}
