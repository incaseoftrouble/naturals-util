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
import de.tum.in.naturals.set.NatBitSetProvider;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.BitSet;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Utility class to help interacting with {@link BitSet}.
 */
public final class BitSets {
  private BitSets() {}

  public static BitSet of() {
    return new BitSet(0);
  }

  public static BitSet of(int index) {
    BitSet bitSet = new BitSet(index + 1);
    bitSet.set(index);
    return bitSet;
  }

  public static BitSet of(int... indices) {
    BitSet bitSet = new BitSet();
    for (int index : indices) {
      bitSet.set(index);
    }
    return bitSet;
  }

  public static BitSet of(IntIterable iterable) {
    if (iterable instanceof NatBitSet) {
      return NatBitSetProvider.toBitSet((NatBitSet) iterable);
    }

    BitSet bitSet;
    if (iterable instanceof IntSortedSet) {
      IntSortedSet sortedSet = (IntSortedSet) iterable;
      if (sortedSet.comparator() == null) {
        bitSet = new BitSet(sortedSet.lastInt() + 1);
      } else {
        bitSet = new BitSet();
      }
    } else {
      bitSet = new BitSet();
    }
    iterable.forEach((IntConsumer) bitSet::set);
    return bitSet;
  }

  public static BitSet of(Iterable<Integer> iterable) {
    if (iterable instanceof IntIterable) {
      return of((IntIterable) iterable);
    }

    BitSet bitSet = new BitSet();
    iterable.forEach(bitSet::set);
    return bitSet;
  }

  @SuppressWarnings("TypeMayBeWeakened")
  public static BitSet of(PrimitiveIterator.OfInt iterator) {
    BitSet bitSet = new BitSet();
    iterator.forEachRemaining((IntConsumer) bitSet::set);
    return bitSet;
  }

  public static BitSet of(boolean... indices) {
    BitSet bitSet = new BitSet(indices.length);
    for (int i = 0; i < indices.length; i++) {
      if (indices[i]) {
        bitSet.set(i);
      }
    }
    return bitSet;
  }

  public static BitSet of(SparseBitSet sparseBitSet) {
    BitSet bitSet = new BitSet(sparseBitSet.length());
    SparseBitSets.forEach(sparseBitSet, bitSet::set);
    return bitSet;
  }

  public static BitSet copyOf(BitSet bitset) {
    return (BitSet) bitset.clone();
  }


  public static IntIterator complementIterator(BitSet bitSet, int length) {
    return new BitSetComplementIterator(bitSet, length);
  }


  public static void forEach(BitSet bitSet, IntConsumer consumer) {
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
  public static boolean isSubset(BitSet first, BitSet second) {
    return isSubsetConsuming(copyOf(first), second);
  }

  /**
   * Checks if {@code first} is a subset of {@code second}, potentially modifying both sets in the
   * process.
   */
  public static boolean isSubsetConsuming(BitSet first, BitSet second) {
    first.andNot(second);
    return first.isEmpty();
  }


  public static boolean isDisjoint(BitSet first, BitSet second) {
    return !first.intersects(second);
  }


  public static IntIterator iterator(BitSet bitSet) {
    return new BitSetIterator(bitSet);
  }

  public static IntIterator iterator(BitSet bitSet, int length) {
    return new BitSetIterator(bitSet, length);
  }


  /**
   * Returns the set containing all subsets of the given basis.
   * <strong>Warning</strong>: For performance reasons, the iterator of this set may modify the
   * returned elements in place.
   */
  public static Set<BitSet> powerSet(BitSet basis) {
    int length = basis.length();
    if (length == basis.cardinality()) {
      return powerSet(length);
    }
    return new PowerBitSet(basis);
  }

  public static Set<BitSet> powerSet(int i) {
    return new PowerBitSetSimple(i);
  }
}
