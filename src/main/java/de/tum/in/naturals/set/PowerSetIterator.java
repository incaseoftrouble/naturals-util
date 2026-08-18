// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>This iterator yields all elements of the power set of the given {@code base}. More specifically
 * it yields all boolean arrays of length {@code base.length} which are a subset of {@code base}.
 * The iteration always returns the elements in the order and always starts with the empty array.</p>
 *
 * <strong>Warning</strong>: For performance, the returned array is modified in place.
 */
public class PowerSetIterator implements Iterator<boolean[]> {
    private final boolean[] base;
    private final boolean[] current;
    private final int domainSize;
    private int currentSize = 0;
    private boolean first = true;

    public PowerSetIterator(boolean[] base) {
        this.base = base.clone();
        this.current = new boolean[base.length];
        int domainSize = 0;
        for (boolean value : this.base) {
            if (value) {
                domainSize += 1;
            }
        }
        this.domainSize = domainSize;
    }

    public int currentIndex() {
        if (base.length >= Integer.SIZE) {
            throw new IllegalStateException();
        }

        int index = 0;
        for (int i = 0; i < base.length; i++) {
            if (current[i]) {
                index |= 1 << i;
            }
        }
        return index;
    }

    public long currentIndexLong() {
        if (base.length >= Long.SIZE) {
            throw new IllegalStateException();
        }

        long index = 0L;
        for (int i = 0; i < base.length; i++) {
            if (current[i]) {
                index |= 1L << i;
            }
        }
        return index;
    }

    /** The last subset yielded is the whole base, i.e. the counter has no room left to increment. */
    @Override
    public boolean hasNext() {
        return first || currentSize < domainSize;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public boolean[] next() {
        if (first) {
            first = false;
            return current;
        }

        for (int i = 0; i < base.length; i++) {
            if (!base[i]) {
                continue;
            }
            if (current[i]) {
                current[i] = false;
                currentSize -= 1;
            } else {
                current[i] = true;
                currentSize += 1;
                return current;
            }
        }

        throw new NoSuchElementException("No next element");
    }
}
