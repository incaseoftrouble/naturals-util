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

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

/**
 * An iterator which yields all integers in {@code [from, to)} in descending order.
 */
public class ReverseRangeIterator implements IntIterator {
  private final int from;
  private int current;

  public ReverseRangeIterator(int from, int to) {
    if (to < from) {
      throw new IllegalArgumentException("To must be larger or equal than from");
    }
    this.from = from;
    current = to - 1;
  }

  @Override
  public boolean hasNext() {
    return current >= from;
  }

  @Override
  public int skip(int n) {
    int pos = current - n;
    if (pos >= from) {
      current = pos;
      return n;
    }
    int result = current - from + 1;
    current = from - 1;
    return result;
  }

  @Override
  public int nextInt() {
    if (current < from) {
      throw new NoSuchElementException();
    }
    int result = current;
    current -= 1;
    return result;
  }
}
