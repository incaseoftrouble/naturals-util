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

import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.SparseBitSet;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.BitSet;
import java.util.Set;
import java.util.function.IntConsumer;

public final class NatBitSets {
  public static final int UNKNOWN_LENGTH = -1;
  public static final int UNKNOWN_SIZE = -1;
  private static final int SPARSE_THRESHOLD = 128;

  private NatBitSets() {}

  /**
   * Return a view on the given {@code bitSet}.
   */
  public static NatBitSet asSet(BitSet bitSet) {
    return new SimpleNatBitSet(bitSet);
  }

  public static BoundedNatBitSet boundedSet(int domainSize, int expectedSize) {
    return (useSparse(expectedSize, domainSize))
        ? new SparseBoundedNatBitSet(new SparseBitSet(domainSize), domainSize)
        : new SimpleBoundedNatBitSet(new BitSet(), domainSize);
  }

  public static BoundedNatBitSet boundedSimpleSet(int domainSize) {
    return new SimpleBoundedNatBitSet(new BitSet(), domainSize);
  }

  public static BoundedNatBitSet boundedSparseSet(int domainSize) {
    return new SparseBoundedNatBitSet(new SparseBitSet(), domainSize);
  }

  /**
   * Try to compact the given set by potentially representing it as, e.g., empty or singleton set.
   * The returned set might not be modifiable.
   *
   * @param set
   *     The set to be compacted.
   *
   * @return a potentially compacted representation of the given set.
   */
  public static NatBitSet compact(NatBitSet set) {
    if (set instanceof MutableSingletonNatBitSet || set instanceof FixedSizeNatBitSet) {
      return set;
    }
    if (set.isEmpty()) {
      return new MutableSingletonNatBitSet();
    }
    if (set.size() == 1) {
      return new MutableSingletonNatBitSet(set.lastInt());
    }
    if (set.firstInt() == 0 && set.lastInt() == set.size() - 1) {
      return new FixedSizeNatBitSet(set.lastInt());
    }
    // TODO Determine optimal representation (Sparse vs non-sparse, direct vs. complement)
    return set;
  }

  /**
   * Returns an unmodifiable iterator yielding all elements in {@code {0, ..., length - 1} \ set} in
   * ascending order.
   *
   * @param set
   *     The set to be complemented.
   * @param length
   *     The size of the domain.
   *
   * @return an unmodifiable iterator over the complement.
   */
  public static IntIterator complementIterator(NatBitSet set, int length) {
    if (set.isEmpty() || set.firstInt() >= length) {
      return IntIterators.fromTo(0, length);
    }
    if (set instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet boundedSet = (SimpleBoundedNatBitSet) set;
      BitSet bitSet = boundedSet.getBitSet();
      if (!boundedSet.isComplement()) {
        return IntIterators.unmodifiable(BitSets.iterator(bitSet, length));
      }
      int domainSize = boundedSet.domainSize();
      if (length <= domainSize) {
        return IntIterators.unmodifiable(BitSets.complementIterator(bitSet, length));
      }
      return IntIterators.concat(new IntIterator[] {
          IntIterators.unmodifiable(BitSets.complementIterator(bitSet, domainSize)),
          IntIterators.fromTo(domainSize, length)
      });
    }
    if (set instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet boundedSet = (SparseBoundedNatBitSet) set;
      SparseBitSet bitSet = boundedSet.getSparseBitSet();
      if (!boundedSet.isComplement()) {
        return IntIterators.unmodifiable(BitSets.iterator(bitSet, length));
      }
      int domainSize = boundedSet.domainSize();
      if (length <= domainSize) {
        return IntIterators.unmodifiable(BitSets.complementIterator(bitSet, length));
      }
      return IntIterators.concat(new IntIterator[] {
          IntIterators.unmodifiable(BitSets.complementIterator(bitSet, domainSize)),
          IntIterators.fromTo(domainSize, length)
      });
    }
    if (set instanceof SimpleNatBitSet) {
      SimpleNatBitSet bitSet = (SimpleNatBitSet) set;
      return IntIterators.unmodifiable(BitSets.complementIterator(bitSet.getBitSet(), length));
    }
    if (set instanceof SparseNatBitSet) {
      SparseNatBitSet bitSet = (SparseNatBitSet) set;
      return IntIterators.unmodifiable(
          BitSets.complementIterator(bitSet.getSparseBitSet(), length));
    }
    if (set instanceof FixedSizeNatBitSet) {
      int size = set.size();
      if (size >= length) {
        return IntIterators.EMPTY_ITERATOR;
      }
      return IntIterators.fromTo(size, length);
    }
    if (set instanceof MutableSingletonNatBitSet) {
      assert !set.isEmpty();
      int element = set.firstInt();
      if (element == 0) {
        return IntIterators.fromTo(1, length);
      }
      if (length <= element + 1) {
        return IntIterators.fromTo(0, length);
      }
      return IntIterators.concat(new IntIterator[] {IntIterators.fromTo(0, element),
          IntIterators.fromTo(element + 1, length)});
    }
    throw new IllegalArgumentException();
  }

  /**
   * Copies the given collection. The returned set might not be modifiable.
   *
   * @param ints
   *     The collection to be copied.
   *
   * @return a copy of the given collection.
   *
   * @see #modifiableCopyOf(NatBitSet)
   */
  public static NatBitSet copyOf(IntCollection ints) {
    NatBitSet copy;
    if (ints instanceof NatBitSet) {
      copy = ((NatBitSet) ints).clone();
    } else if (ints instanceof IntSortedSet) {
      copy = set(ints.size(), ((IntSortedSet) ints).lastInt());
      copy.or(ints);
    } else {
      copy = set(ints.size(), UNKNOWN_LENGTH);
      copy.or(ints);
    }
    assert copy.equals(ints instanceof Set ? ints : new IntAVLTreeSet(ints));
    return copy;
  }

  /**
   * Returns an empty set.
   */
  public static NatBitSet emptySet() {
    return new MutableSingletonNatBitSet();
  }

  /**
   * Ensures that the given set can be modified with arbitrary values. If necessary, the set is
   * copied into a general purpose representation if necessary.
   *
   * @see #isModifiable(NatBitSet)
   */
  public static NatBitSet ensureGeneral(NatBitSet set) {
    if (isModifiable(set)) {
      return set;
    }
    return modifiableCopyOf(set);
  }

  /**
   * Ensures that the given set can be modified with arbitrary values from {@code {0, ..., n}}. If
   * necessary, the set is copied into a general purpose representation if necessary.
   *
   * @see #isModifiable(NatBitSet, int)
   */
  public static NatBitSet ensureGeneral(NatBitSet set, int length) {
    if (isModifiable(set, length)) {
      return set;
    }
    return modifiableCopyOf(set, length);
  }

  /**
   * Returns the set containing the full domain {@code {0, ..., length - 1}}.
   */
  public static BoundedNatBitSet fullSet(int length) {
    return new FixedSizeNatBitSet(length);
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary (positive) modifications.
   */
  public static boolean isModifiable(NatBitSet set) {
    return isModifiable(set, Integer.MAX_VALUE);
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary modifications of values between 0
   * and {@code length - 1}.
   */
  public static boolean isModifiable(NatBitSet set, int length) {
    if (length == 0) {
      return true;
    }
    if (set instanceof BoundedNatBitSet) {
      return length <= ((BoundedNatBitSet) set).domainSize();
    }
    return set instanceof SimpleNatBitSet || set instanceof SparseNatBitSet;
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable.
   *
   * @see #isModifiable(NatBitSet)
   */
  public static NatBitSet modifiableCopyOf(NatBitSet set) {
    return modifiableCopyOf(set, Integer.MAX_VALUE);
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable up to
   * {@code length - 1}.
   *
   * @see #isModifiable(NatBitSet, int)
   */
  public static NatBitSet modifiableCopyOf(NatBitSet set, int length) {
    if (isModifiable(set, length)) {
      return set.clone();
    }
    NatBitSet copy;
    if (set instanceof BoundedNatBitSet && length <= ((BoundedNatBitSet) set).domainSize()) {
      copy = set.clone();
    } else if (set instanceof SimpleNatBitSet || set instanceof SparseBoundedNatBitSet) {
      copy = set.clone();
    } else {
      copy = set(set.size(), length);
      copy.or(set);
    }
    assert isModifiable(copy, length) && copy.equals(set);
    return copy;
  }

  /**
   * Creates a modifiable set with the expected size and length.
   */
  public static NatBitSet set(int expectedSize, int expectedLength) {
    if (useSparse(expectedSize, expectedLength)) {
      SparseBitSet backingSet = expectedLength == UNKNOWN_LENGTH
          ? new SparseBitSet() : new SparseBitSet(expectedLength);
      return new SparseNatBitSet(backingSet);
    }
    return new SimpleNatBitSet(new BitSet());
  }

  /**
   * Creates a modifiable set.
   */
  public static NatBitSet set() {
    return set(UNKNOWN_SIZE, UNKNOWN_LENGTH);
  }

  /**
   * Creates a modifiable set with expected length.
   */
  public static NatBitSet setWithExpectedLength(int expectedLength) {
    return set(UNKNOWN_SIZE, expectedLength);
  }

  /**
   * Creates a modifiable set with expected size.
   */
  public static NatBitSet setWithExpectedSize(int expectedSize) {
    return set(expectedSize, UNKNOWN_LENGTH);
  }

  public static NatBitSet simpleSet() {
    return new SimpleNatBitSet(new BitSet());
  }

  public static NatBitSet simpleSet(int expectedSize) {
    return new SimpleNatBitSet(new BitSet(expectedSize));
  }

  public static NatBitSet singleton(int element) {
    return new MutableSingletonNatBitSet(element);
  }

  public static NatBitSet sparseSet() {
    return new SparseNatBitSet(new SparseBitSet());
  }

  public static NatBitSet sparseSet(int expectedSize) {
    return new SparseNatBitSet(new SparseBitSet(expectedSize));
  }

  public static BitSet toBitSet(IntIterable ints) {
    if (ints instanceof SimpleNatBitSet) {
      return (BitSet) ((SimpleNatBitSet) ints).getBitSet().clone();
    }
    if (ints instanceof SimpleBoundedNatBitSet) {
      return (BitSet) ((SimpleBoundedNatBitSet) ints).getBitSet().clone();
    }
    if (ints instanceof SparseNatBitSet) {
      return BitSets.toBitSet(((SparseNatBitSet) ints).getSparseBitSet());
    }
    if (ints instanceof SparseBoundedNatBitSet) {
      return BitSets.toBitSet(((SparseBoundedNatBitSet) ints).getSparseBitSet());
    }

    BitSet bitSet;
    if (ints instanceof NatBitSet) {
      bitSet = new BitSet(((NatBitSet) ints).lastInt());
    } else if (ints instanceof IntSortedSet) {
      bitSet = new BitSet(((IntSortedSet) ints).lastInt());
    } else {
      bitSet = new BitSet();
    }
    ints.forEach((IntConsumer) bitSet::set);
    return bitSet;
  }

  private static boolean useSparse(int expectedSize, int expectedLength) {
    if (expectedLength == UNKNOWN_LENGTH) {
      if (expectedSize == UNKNOWN_SIZE) {
        return true;
      }
      if (expectedSize > SPARSE_THRESHOLD) {
        return true;
      }
    }
    return expectedLength > SPARSE_THRESHOLD
        || expectedSize != UNKNOWN_SIZE && expectedSize > SPARSE_THRESHOLD;
  }
}
