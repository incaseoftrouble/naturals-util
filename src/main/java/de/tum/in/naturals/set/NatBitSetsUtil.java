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

import com.zaxxer.sparsebits.SparseBitSet;
import java.util.Spliterator;
import javax.annotation.Nonnegative;
import org.roaringbitmap.RoaringBitmap;

public final class NatBitSetsUtil {
  public static final int SPLITERATOR_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.SORTED
      | Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SIZED;

  private NatBitSetsUtil() {}

  public static void checkInDomain(int domainSize, int index) {
    checkNonNegative(index);
    if (domainSize <= index) {
      throw new IndexOutOfBoundsException(String.format("Index %d too large for domain [0, %d)",
          index, domainSize));
    }
  }

  public static void checkInDomain(int domainSize, int from, int to) {
    checkRange(from, to);
    if (domainSize < to) {
      throw new IndexOutOfBoundsException(String.format("To index %d too large for domain [0, %d)",
          to, domainSize));
    }
  }

  public static void checkNonNegative(int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException(String.format("Negative index %d ", index));
    }
  }

  public static void checkRange(int from, int to) {
    if (to < from) {
      throw new IndexOutOfBoundsException(String.format("From %d bigger than to %d", from, to));
    }
    if (from < 0) {
      throw new IndexOutOfBoundsException(String.format("Negative from index %d ", from));
    }
  }

  public static int previousPresentIndex(RoaringBitmap set, @Nonnegative int index) {
    // Binary search for the biggest set bit with index <= length
    if (set.contains(index)) {
      return index;
    }

    int firstPresentIndex = (int) set.nextValue(0);
    if (firstPresentIndex == -1 || firstPresentIndex > index) {
      return -1;
    }

    int high = index - 1;
    int low = firstPresentIndex;

    while (true) {
      assert low <= high;
      int mid = (high + low) >>> 1;
      int next = (int) set.nextValue(mid);
      while (next == -1 || next > index) {
        assert low <= mid && mid <= high;
        high = mid;
        mid = (high + low) >>> 1;
        next = (int) set.nextValue(mid);
      }
      assert set.contains(next);
      low = next;
      int nextSet = (int) set.nextValue(low + 1);
      if (nextSet == -1 || nextSet > index) {
        return low;
      }
      low = nextSet;
    }
  }
}
