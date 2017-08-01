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

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.function.IntConsumer;

/**
 * Utility class to help interacting with {@link BitSet} and {@link SparseBitSet}.
 */
public final class BitSets {
  private BitSets() {}

  public static IntIterator complementIterator(BitSet bitSet, int length) {
    return new BitSetComplementIterator(bitSet, length);
  }

  public static IntIterator complementIterator(SparseBitSet bitSet, int length) {
    return new SparseBitSetComplementIterator(bitSet, length);
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

  public static IntIterator iterator(BitSet bitSet) {
    return new BitSetIterator(bitSet);
  }

  public static IntIterator iterator(BitSet bitSet, int length) {
    return new BitSetIterator(bitSet, length);
  }

  public static IntIterator iterator(SparseBitSet bitSet) {
    return new SparseBitSetIterator(bitSet);
  }

  public static IntIterator iterator(SparseBitSet bitSet, int length) {
    return new SparseBitSetIterator(bitSet, length);
  }

  public static BitSet toBitSet(SparseBitSet sparseBitSet) {
    BitSet bitSet = new BitSet(sparseBitSet.length());
    forEach(sparseBitSet, bitSet::set);
    return bitSet;
  }

  public static SparseBitSet toSparse(BitSet bitSet) {
    SparseBitSet sparseBitSet = new SparseBitSet(bitSet.length());
    forEach(bitSet, sparseBitSet::set);
    return sparseBitSet;
  }
}
