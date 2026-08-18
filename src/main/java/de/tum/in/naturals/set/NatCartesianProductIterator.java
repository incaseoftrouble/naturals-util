// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This iterator yields all elements of the cartesian product. The domains are specified exactly as
 * in {@link NatCartesianProductSet}. It is guaranteed that the iteration always follows the same
 * order.
 *
 * <p><strong>Warning</strong>: For performance, the returned array is edited in-place.
 */
public class NatCartesianProductIterator implements Iterator<int[]> {
    private final int[] domainMaximalElements;
    private final int[] element;
    private final long size;
    private long nextIndex = 0L;

    public NatCartesianProductIterator(int[] domainMaximalElements) {
        this.domainMaximalElements = domainMaximalElements.clone();
        this.size = NatCartesianProductSet.numberOfElements(this.domainMaximalElements);
        this.element = new int[domainMaximalElements.length];
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    NatCartesianProductIterator(int[] domainMaximalElements, long size) {
        assert NatCartesianProductSet.numberOfElements(domainMaximalElements) == size;
        this.domainMaximalElements = domainMaximalElements;
        this.size = size;
        this.element = new int[domainMaximalElements.length];
    }

    @Override
    public boolean hasNext() {
        return nextIndex < size;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public int[] next() {
        nextIndex += 1L;
        if (nextIndex == 1L) {
            return element;
        }

        for (int i = 0; i < element.length; i++) {
            if (element[i] == domainMaximalElements[i]) {
                element[i] = 0;
            } else {
                assert element[i] < domainMaximalElements[i];
                element[i] += 1;
                return element;
            }
        }

        throw new NoSuchElementException("No next element");
    }

    /**
     * Returns the iteration index of the next element. It is guaranteed that two different iterators
     * over the same domain yield the same element if and only if the element has the same index.
     */
    public long nextIndex() {
        return nextIndex;
    }
}
