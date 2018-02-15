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
 * This iterator yields all elements of the power set of the given {@code base}. More specifically
 * it yields all boolean arrays of length {@code base.length} which are a subset of {@code base}.
 * The iteration always returns the elements in the order and always starts with the empty array.
 *
 * <strong>Warning</strong>: For performance, the returned array is modified in place.
 */
public class PowerSetIterator implements Iterator<boolean[]> {
  private final boolean[] base;
  private final boolean[] current;
  private final int domainSize;
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
    if (domainSize > Integer.SIZE) {
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
    if (domainSize > Long.SIZE) {
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

  @Override
  public boolean hasNext() {
    if (first) {
      return true;
    }
    for (int i = 0; i < base.length; i++) {
      if (base[i] && !current[i]) {
        return true;
      }
    }
    return false;
  }

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
      } else {
        current[i] = true;
        return current;
      }
    }

    throw new NoSuchElementException("No next element");
  }
}