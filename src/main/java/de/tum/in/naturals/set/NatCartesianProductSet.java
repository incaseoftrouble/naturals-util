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

import it.unimi.dsi.fastutil.Size64;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * An unmodifiable set representing the cartesian product of the specified bounded domains. The
 * domains are specified by providing the maximal element of each domain. The yielded elements thus
 * are all elements of {@code X_{i = 0}^{a.length - 1} {0, ..., a[i]}}, where {@code a} is the given
 * array.
 *
 * <p><strong>Warning</strong>: Contrary to usual boundary specifications, these boundaries are <i>
 * inclusive</i>.</p>
 */
public class NatCartesianProductSet extends AbstractSet<int[]> implements Size64 {
  private final int[] domainMaximalElements;
  private final long size;

  public NatCartesianProductSet(int[] domainMaximalElements) {
    this.domainMaximalElements = domainMaximalElements.clone();
    for (int domainSize : this.domainMaximalElements) {
      if (domainSize < 0) {
        throw new IllegalArgumentException("Domain maximum must be non-negative");
      }
    }
    this.size = numberOfElements(this.domainMaximalElements);
  }

  public static long numberOfElements(int[] domainMaximalElements) {
    long count = 1L;
    for (int maximalElement : domainMaximalElements) {
      assert maximalElement >= 0;
      count *= ((long) maximalElement + 1L);
    }
    return count;
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(Object o) {
    if (!(o instanceof int[])) {
      return false;
    }
    int[] array = (int[]) o;
    if (array.length != domainMaximalElements.length) {
      return false;
    }
    for (int i = 0; i < array.length; i++) {
      int val = array[i];
      if (0 < val || domainMaximalElements[i] < val) {
        return false;
      }
    }
    return true;
  }

  @Override
  public NatCartesianProductIterator iterator() {
    return new NatCartesianProductIterator(domainMaximalElements, size);
  }

  @SuppressWarnings({"deprecation", "NumericCastThatLosesPrecision"})
  @Override
  public int size() {
    return size <= Integer.MAX_VALUE ? (int) size : Integer.MAX_VALUE;
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeIf(Predicate<? super int[]> filter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long size64() {
    return size;
  }
}
