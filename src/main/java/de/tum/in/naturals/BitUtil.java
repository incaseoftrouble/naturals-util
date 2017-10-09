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

package de.tum.in.naturals;

public final class BitUtil {
  private BitUtil() {}

  public static long mask(int from, int to) {
    assert 0 <= from && from <= to && to <= Long.SIZE;
    if (from == to) {
      return 0L;
    }
    if (to == Long.SIZE) {
      return ~((1L << from) - 1L);
    }

    return (1L << to) - 1L & ~((1L << from) - 1L);
  }

  public static long maskTo(int to) {
    assert 0 <= to && to <= Long.SIZE;
    if (to == Long.SIZE) {
      return -1L;
    }
    return (1L << to) - 1L;
  }

  public static int nextBit(long store, int index) {
    long shifted = store >>> (index - 1);
    int num = Long.numberOfTrailingZeros(shifted);
    if (num == Long.SIZE) {
      return -1;
    }
    assert num < Long.SIZE - index;
    return num + index + 1;
  }
}
