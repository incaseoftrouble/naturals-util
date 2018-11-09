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

package de.tum.in.naturals.bitset;

import com.zaxxer.sparsebits.SparseBitSet;
import de.tum.in.naturals.set.NatBitSet;
import de.tum.in.naturals.set.NatBitSets;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.BitSet;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;

/**
 * Utility class to help interacting with {@link SparseBitSet}.
 */
public final class SparseBitSets {
  private SparseBitSets() {}

  public static SparseBitSet of() {
    return new SparseBitSet(0);
  }

  public static SparseBitSet of(int... indices) {
    SparseBitSet bitSet = new SparseBitSet();
    for (int index : indices) {
      bitSet.set(index);
    }
    return bitSet;
  }

  public static SparseBitSet of(IntIterable iterable) {
    if (iterable instanceof NatBitSet) {
      return NatBitSets.toSparseBitSet((NatBitSet) iterable);
    }

    SparseBitSet bitSet;
    if (iterable instanceof IntSortedSet) {
      IntSortedSet sortedSet = (IntSortedSet) iterable;
      if (sortedSet.comparator() == null) {
        bitSet = new SparseBitSet(sortedSet.lastInt() + 1);
      } else {
        bitSet = new SparseBitSet();
      }
    } else {
      bitSet = new SparseBitSet();
    }
    iterable.forEach((IntConsumer) bitSet::set);
    return bitSet;
  }

  public static SparseBitSet of(Iterable<Integer> iterable) {
    if (iterable instanceof IntIterable) {
      return of((IntIterable) iterable);
    }

    SparseBitSet bitSet = new SparseBitSet();
    iterable.forEach(bitSet::set);
    return bitSet;
  }

  @SuppressWarnings("TypeMayBeWeakened")
  public static SparseBitSet of(PrimitiveIterator.OfInt iterator) {
    SparseBitSet sparseBitSet = new SparseBitSet();
    iterator.forEachRemaining((IntConsumer) sparseBitSet::set);
    return sparseBitSet;
  }

  public static SparseBitSet of(BitSet bitSet) {
    SparseBitSet sparseBitSet = new SparseBitSet(bitSet.length());
    BitSets.forEach(bitSet, sparseBitSet::set);
    return sparseBitSet;
  }


  public static IntIterator complementIterator(SparseBitSet bitSet, int length) {
    return new SparseBitSetComplementIterator(bitSet, length);
  }


  public static void forEach(SparseBitSet bitSet, IntConsumer consumer) {
    int length = bitSet.length();
    int cardinality = bitSet.cardinality();
    if (length < cardinality * 2) {
      // the set is rather dense, meaning there probably are a lot of blocks of 1s
      int currentBlock = bitSet.nextSetBit(0);
      while (currentBlock > -1) {
        int blockEnd = bitSet.nextClearBit(currentBlock);
        assert blockEnd > currentBlock;
        for (int i = currentBlock; i < blockEnd; i++) {
          consumer.accept(i);
        }
        currentBlock = bitSet.nextSetBit(blockEnd);
      }
    } else {
      for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
        consumer.accept(i);
      }
    }
  }


  /**
   * Checks if {@code first} is a subset of {@code second}.
   */
  public static boolean isSubset(SparseBitSet first, SparseBitSet second) {
    return isSubsetConsuming(first.clone(), second);
  }

  /**
   * Checks if {@code first} is a subset of {@code second}, potentially modifying both sets in the
   * process.
   */
  public static boolean isSubsetConsuming(SparseBitSet first, SparseBitSet second) {
    first.andNot(second);
    return first.isEmpty();
  }


  public static boolean isDisjoint(SparseBitSet first, SparseBitSet second) {
    return !first.intersects(second);
  }


  public static IntIterator iterator(SparseBitSet bitSet) {
    return new SparseBitSetIterator(bitSet);
  }


  public static int previousAbsentIndex(SparseBitSet set, @Nonnegative int index) {
    // Binary search for the biggest clear bit with index <= length
    if (!set.get(index)) {
      return index;
    }

    int firstAbsentIndex = set.nextClearBit(0);
    if (firstAbsentIndex > index) {
      return -1;
    }

    int high = index - 1;
    int low = firstAbsentIndex;

    while (true) {
      assert low <= high;
      int mid = (high + low) >>> 1;
      int next = set.nextClearBit(mid);
      while (next > index) {
        assert low <= mid && mid <= high;
        high = mid;
        mid = (high + low) >>> 1;
        next = set.nextClearBit(mid);
      }
      assert !set.get(next);
      low = next;
      int nextClear = set.nextClearBit(low + 1);
      if (nextClear > index) {
        return low;
      }
      low = nextClear;
    }
  }

  public static int previousPresentIndex(SparseBitSet set, @Nonnegative int index) {
    // Binary search for the biggest set bit with index <= length
    if (set.get(index)) {
      return index;
    }

    int firstPresentIndex = set.nextSetBit(0);
    if (firstPresentIndex == -1 || firstPresentIndex > index) {
      return -1;
    }

    int high = index - 1;
    int low = firstPresentIndex;

    while (true) {
      assert low <= high;
      int mid = (high + low) >>> 1;
      int next = set.nextSetBit(mid);
      while (next == -1 || next > index) {
        assert low <= mid && mid <= high;
        high = mid;
        mid = (high + low) >>> 1;
        next = set.nextSetBit(mid);
      }
      assert set.get(next);
      low = next;
      int nextSet = set.nextSetBit(low + 1);
      if (nextSet == -1 || nextSet > index) {
        return low;
      }
      low = nextSet;
    }
  }
}
