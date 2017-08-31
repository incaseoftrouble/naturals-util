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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This iterator yields all elements of the cartesian product. The domains are specified exactly as
 * in {@link NatCartesianProductSet}.
 *
 * <p><strong>Warning</strong>: For performance, the returned array is edited in-place.
 */
public final class NatCartesianProductIterator implements Iterator<int[]> {
  private final int[] domainMaximalElements;
  private final int[] element;
  private final long size;
  private long nextIndex = 0L;

  @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
  public NatCartesianProductIterator(int[] domainMaximalElements) {
    this.domainMaximalElements = domainMaximalElements;
    this.size = NatCartesianProductSet.numberOfElements(domainMaximalElements);
    this.element = new int[domainMaximalElements.length];
  }

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
