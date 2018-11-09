/*
 * Copyright (C) 2018 Tobias Meggendorfer
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

package de.tum.in.naturals.bitset;

import de.tum.in.naturals.set.NatBitSet;
import de.tum.in.naturals.set.NatBitSets;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

/**
 * Utility class to help interacting with {@link org.roaringbitmap.RoaringBitmap}.
 */
public final class RoaringBitmaps {
  private RoaringBitmaps() {}

  public static RoaringBitmap of(IntIterable iterable) {
    if (iterable instanceof NatBitSet) {
      return NatBitSets.toRoaringBitmap((NatBitSet) iterable);
    }

    RoaringBitmap bitmap = new RoaringBitmap();
    iterable.forEach((IntConsumer) bitmap::add);
    return bitmap;
  }

  public static RoaringBitmap of(Iterable<Integer> iterable) {
    if (iterable instanceof IntIterable) {
      return of((IntIterable) iterable);
    }

    RoaringBitmap bitmap = new RoaringBitmap();
    iterable.forEach(bitmap::add);
    return bitmap;
  }

  @SuppressWarnings("TypeMayBeWeakened")
  public static RoaringBitmap of(PrimitiveIterator.OfInt iterator) {
    RoaringBitmap bitmap = new RoaringBitmap();
    iterator.forEachRemaining((IntConsumer) bitmap::add);
    return bitmap;
  }

  public static RoaringBitmap of(BitSet bitSet) {
    RoaringBitmap bitmap = new RoaringBitmap();
    BitSets.forEach(bitSet, bitmap::add);
    return bitmap;
  }


  public static IntIterator iterator(RoaringBitmap bitmap) {
    return new RoaringIterator(bitmap.getIntIterator());
  }

  public static int nextAbsentIndex(RoaringBitmap bitmap, @Nonnegative int index) {
    // For now a simple linear search ...
    if (!bitmap.contains(index)) {
      return index;
    }
    PeekableIntIterator intIterator = bitmap.getIntIterator();
    intIterator.advanceIfNeeded(index);
    int current = index;
    while (intIterator.hasNext()) {
      int next = intIterator.next();
      if (next > current + 1) {
        return current + 1;
      }
      current = next;
    }
    return current + 1;
  }

  public static int previousPresentIndex(RoaringBitmap bitmap, @Nonnegative int index) {
    if (bitmap.contains(index)) {
      return index;
    }
    int rank = bitmap.rank(index);
    if (rank == 0) {
      return -1;
    }

    return bitmap.select(rank - 1);
  }

  public static int previousAbsentIndex(RoaringBitmap bitmap, @Nonnegative int index) {
    if (!bitmap.contains(index)) {
      return index;
    }
    int rank = bitmap.rank(index) - 1;
    if (index == rank) { // Rank of index == index -> All elements below are set
      return -1;
    }
    assert bitmap.select(rank) == index;

    int current = index;
    for (int i = rank - 1; i >= 0; i--) {
      assert bitmap.contains(current);

      int next = bitmap.select(i);
      assert next < current;
      if (next + 1 < current) {
        assert !bitmap.contains(current - 1);
        return current - 1;
      }
      current = next;
    }

    return current - 1;
  }
}
