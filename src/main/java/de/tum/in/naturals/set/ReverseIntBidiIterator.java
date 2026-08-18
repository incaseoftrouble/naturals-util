// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;

public class ReverseIntBidiIterator implements IntBidirectionalIterator {
    private final IntBidirectionalIterator iterator;

    public ReverseIntBidiIterator(IntBidirectionalIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public int back(int n) {
        return iterator.skip(n);
    }

    @Override
    public boolean hasNext() {
        return iterator.hasPrevious();
    }

    @Override
    public boolean hasPrevious() {
        return iterator.hasNext();
    }

    @Override
    public int nextInt() {
        return iterator.previousInt();
    }

    @Override
    public int previousInt() {
        return iterator.nextInt();
    }

    @Override
    public int skip(int n) {
        return iterator.back(n);
    }

    @Override
    public void remove() {
        iterator.remove();
    }
}
