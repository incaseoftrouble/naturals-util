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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;

public final class NatBitSets {
  public static final int UNKNOWN_LENGTH = -1;
  public static final int UNKNOWN_SIZE = -1;
  private static final int SPARSE_THRESHOLD = Long.SIZE * 128;

  private NatBitSets() {}

  /**
   * Ensures that the given {@code set} is a {@link BoundedNatBitSet}. When possible, the backing
   * data structure is shallow copied. For example, when passing a {@link SimpleNatBitSet}, a
   * {@link SimpleBoundedNatBitSet} with the same backing bit set will be returned. Note that after
   * this operation, only the returned set should be used to ensure integrity.
   *
   * <p><strong>Warning</strong>: If {@code set} already is a {@link BoundedNatBitSet} with
   * different domain size, an exception will be thrown, to avoid potentially unexpected behavior
   * </p>
   *
   * @throws IndexOutOfBoundsException
   *     if {@code set} contains an index larger than {@code domainSize}.
   * @throws IllegalArgumentException
   *     if {@code set} already is a {@link BoundedNatBitSet} and has a differing domain size.
   */
  public static BoundedNatBitSet asBounded(NatBitSet set, @Nonnegative int domainSize) {
    assert domainSize >= 0;
    if (!set.isEmpty() && set.lastInt() >= domainSize) {
      throw new IndexOutOfBoundsException();
    }
    if (set instanceof BoundedNatBitSet) {
      BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;
      int oldDomainSize = boundedSet.domainSize();
      if (oldDomainSize != domainSize) {
        throw new IllegalArgumentException(String.format(
            "Given set has domain size %d, expected %d", boundedSet.domainSize(), domainSize));
      }
      return boundedSet;
    }
    if (set instanceof SimpleNatBitSet) {
      SimpleNatBitSet simpleSet = (SimpleNatBitSet) set;
      return new SimpleBoundedNatBitSet(simpleSet.getBitSet(), domainSize);
    }
    if (set instanceof SparseNatBitSet) {
      SparseNatBitSet sparseSet = (SparseNatBitSet) set;
      return new SparseBoundedNatBitSet(sparseSet.getSparseBitSet(), domainSize);
    }
    if (set instanceof LongNatBitSet) {
      LongNatBitSet longSet = (LongNatBitSet) set;
      return new LongBoundedNatBitSet(longSet.getStore(), domainSize);
    }
    if (set instanceof MutableSingletonNatBitSet) {
      MutableSingletonNatBitSet singletonSet = (MutableSingletonNatBitSet) set;
      return singletonSet.isEmpty()
          ? new BoundedMutableSingletonNatBitSet(domainSize)
          : new BoundedMutableSingletonNatBitSet(singletonSet.firstInt(), domainSize);
    }

    return new BoundedWrapper(set, domainSize);
  }

  /**
   * Return a view on the given {@code bitSet}.
   */
  public static NatBitSet asSet(BitSet bitSet) {
    return new SimpleNatBitSet(bitSet);
  }

  /**
   * Returns a modifiable set over the specified domain which contains the whole domain.
   */
  public static BoundedNatBitSet boundedFilledSet(int domainSize) {
    return boundedFilledSet(domainSize, UNKNOWN_SIZE);
  }

  public static BoundedNatBitSet boundedFilledSet(int domainSize, int expectedSize) {
    return boundedSet(domainSize, expectedSize).complement();
  }

  public static BoundedNatBitSet boundedLongSet(int domainSize) {
    return new LongBoundedNatBitSet(domainSize);
  }

  public static BoundedNatBitSet boundedSet(int domainSize) {
    return boundedSet(domainSize, UNKNOWN_SIZE);
  }

  public static BoundedNatBitSet boundedSet(int domainSize, int expectedSize) {
    if (domainSize <= LongBoundedNatBitSet.maximalSize()) {
      return new LongBoundedNatBitSet(domainSize);
    }
    return (useSparse(expectedSize, domainSize))
        ? new SparseBoundedNatBitSet(new SparseBitSet(domainSize), domainSize)
        : new SimpleBoundedNatBitSet(new BitSet(), domainSize);
  }

  public static BoundedNatBitSet boundedSimpleSet(int domainSize) {
    return new SimpleBoundedNatBitSet(new BitSet(), domainSize);
  }

  public static BoundedNatBitSet boundedSingleton(int domainSize, int element) {
    return new BoundedMutableSingletonNatBitSet(element, domainSize);
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
    return compact(set, false);
  }

  public static NatBitSet compact(NatBitSet set, boolean forceCopy) {
    if (set instanceof MutableSingletonNatBitSet || set instanceof FixedSizeNatBitSet) {
      return set;
    }
    if (set.isEmpty()) {
      return emptySet();
    }
    if (set.size() == 1) {
      return singleton(set.firstInt());
    }
    if (set.firstInt() == 0 && set.lastInt() == set.size() - 1) {
      return new FixedSizeNatBitSet(set.lastInt() + 1);
    }
    if (!(set instanceof LongNatBitSet) && set.lastInt() < LongNatBitSet.maximalSize()) {
      LongNatBitSet copy = new LongNatBitSet();
      set.forEach((IntConsumer) copy::set);
      return copy;
    }
    // TODO Determine optimal representation (Sparse vs non-sparse, direct vs. complement)
    return forceCopy ? set.clone() : set;
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
  public static IntIterator complementIterator(NatBitSet set, @Nonnegative int length) {
    if (set.isEmpty()) {
      return IntIterators.fromTo(0, length);
    }
    if (set.firstInt() >= length) {
      throw new IllegalArgumentException();
    }
    if (set instanceof FixedSizeNatBitSet) {
      int size = set.size();
      if (size >= length) {
        return IntIterators.EMPTY_ITERATOR;
      }
      return IntIterators.fromTo(size, length);
    }
    if (set instanceof MutableSingletonNatBitSet) {
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
    return IntIterators.unmodifiable(new NatBitSetComplementIterator(set, length));
  }

  public static IntIterator complementReverseIterator(NatBitSet set, @Nonnegative int length) {
    if (set.firstInt() >= length) {
      throw new IllegalArgumentException();
    }
    if (set.isEmpty()) {
      return new ReverseRangeIterator(0, length);
    }
    if (set instanceof FixedSizeNatBitSet) {
      int size = set.size();
      if (size >= length) {
        return IntIterators.EMPTY_ITERATOR;
      }
      return new ReverseRangeIterator(size, length);
    }
    if (set instanceof MutableSingletonNatBitSet) {
      int element = set.firstInt();
      if (element == 0) {
        return new ReverseRangeIterator(1, length);
      }
      if (length <= element + 1) {
        return IntIterators.fromTo(0, length);
      }
      IntIterator firstIterator = new ReverseRangeIterator(element + 1, length);
      IntIterator secondIterator = new ReverseRangeIterator(0, element);
      return IntIterators.concat(new IntIterator[] {firstIterator, secondIterator});
    }
    return IntIterators.unmodifiable(new NatBitSetComplementReverseIterator(set, length));
  }

  /**
   * Copies the given indices. The returned set might not be modifiable.
   *
   * @param indices
   *     The indices to be copied.
   *
   * @return a copy of the given indices.
   *
   * @see #modifiableCopyOf(NatBitSet)
   */
  public static NatBitSet copyOf(IntCollection indices) {
    NatBitSet copy;
    if (indices.isEmpty()) {
      copy = emptySet();
    } else if (indices instanceof NatBitSet) {
      copy = ((NatBitSet) indices).clone();
    } else if (indices instanceof IntSortedSet) {
      copy = set(indices.size(), ((IntSortedSet) indices).lastInt());
      copy.or(indices);
    } else {
      copy = set(indices.size(), UNKNOWN_LENGTH);
      copy.or(indices);
    }
    assert copy.equals(indices instanceof Set ? indices : new IntAVLTreeSet(indices));
    return copy;
  }

  /**
   * Returns an empty set.
   */
  public static NatBitSet emptySet() {
    return new MutableSingletonNatBitSet();
  }

  /**
   * Returns an empty set over the given domain.
   */
  public static BoundedNatBitSet emptySet(@Nonnegative int domainSize) {
    return new FixedSizeNatBitSet(domainSize).complement();
  }

  /**
   * Ensures that the given {@code set} is a {@link BoundedNatBitSet}, copying it if necessary.
   * Note that this also clones the set if, e.g., it is a bounded set with a larger domain.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code set} contains an index larger than {@code domainSize}.
   */
  public static BoundedNatBitSet ensureBounded(NatBitSet set, @Nonnegative int domainSize) {
    if (!set.isEmpty() && set.lastInt() >= domainSize) {
      throw new IndexOutOfBoundsException();
    }
    if (set instanceof BoundedNatBitSet) {
      BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;
      int oldDomainSize = boundedSet.domainSize();
      if (oldDomainSize == domainSize) {
        return boundedSet;
      }
      if (set instanceof SimpleBoundedNatBitSet) {
        SimpleBoundedNatBitSet simpleBoundedSet = (SimpleBoundedNatBitSet) set;
        BitSet bitSetCopy = (BitSet) simpleBoundedSet.getBitSet().clone();
        if (simpleBoundedSet.isComplement()) {
          if (domainSize < oldDomainSize) {
            bitSetCopy.clear(domainSize, oldDomainSize);
          } else {
            bitSetCopy.set(oldDomainSize, domainSize);
          }
        }
        BoundedNatBitSet copy = new SimpleBoundedNatBitSet(bitSetCopy, domainSize);
        return simpleBoundedSet.isComplement() ? copy.complement() : copy;
      } else if (set instanceof SparseBoundedNatBitSet) {
        SparseBoundedNatBitSet sparseBoundedSet = (SparseBoundedNatBitSet) set;
        SparseBitSet bitSetCopy = sparseBoundedSet.getSparseBitSet().clone();
        if (sparseBoundedSet.isComplement()) {
          if (domainSize < oldDomainSize) {
            bitSetCopy.clear(domainSize, oldDomainSize);
          } else {
            bitSetCopy.set(oldDomainSize, domainSize);
          }
        }
        BoundedNatBitSet copy = new SparseBoundedNatBitSet(bitSetCopy, domainSize);
        return sparseBoundedSet.isComplement() ? copy.complement() : copy;
      } else if (set instanceof MutableSingletonNatBitSet) {
        MutableSingletonNatBitSet singletonSet = (MutableSingletonNatBitSet) set;
        return singletonSet.isEmpty()
            ? new BoundedMutableSingletonNatBitSet(domainSize)
            : new BoundedMutableSingletonNatBitSet(singletonSet.firstInt(), domainSize);
      }
    }
    BoundedNatBitSet copy = boundedSet(domainSize, set.size());
    copy.or(set);
    return copy;
  }

  /**
   * Ensures that the given {@code set} can be modified with arbitrary values. If necessary, the set
   * is copied into a general purpose representation.
   *
   * @see #isModifiable(NatBitSet)
   */
  public static NatBitSet ensureModifiable(NatBitSet set) {
    return isModifiable(set) ? set : modifiableCopyOf(set);
  }

  /**
   * Ensures that the given {@code set} can be modified with arbitrary values from
   * {@code {0, ..., n}}. If necessary, the set is copied into a general purpose representation.
   *
   * @see #isModifiable(NatBitSet, int)
   */
  public static NatBitSet ensureModifiable(NatBitSet set, @Nonnegative int length) {
    return isModifiable(set, length) ? set : modifiableCopyOf(set, length);
  }

  /**
   * Ensures that the given {@code set} can be modified in its domain. If necessary, the set is
   * copied into a general purpose representation.
   */
  public static NatBitSet ensureModifiable(BoundedNatBitSet set) {
    return isModifiable(set) ? set : modifiableCopyOf(set);
  }

  /**
   * Returns the set containing the full domain {@code {0, ..., length - 1}}.
   */
  public static BoundedNatBitSet fullSet(@Nonnegative int length) {
    return new FixedSizeNatBitSet(length);
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary (positive) modifications.
   */
  public static boolean isModifiable(NatBitSet set) {
    return isModifiable(set, Integer.MAX_VALUE);
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary modifications within its domain.
   */
  public static boolean isModifiable(BoundedNatBitSet set) {
    return set instanceof SimpleBoundedNatBitSet
        || set instanceof SparseBoundedNatBitSet
        || set instanceof LongBoundedNatBitSet;
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary modifications of values between 0
   * and {@code length - 1}.
   */
  public static boolean isModifiable(NatBitSet set, @Nonnegative int length) {
    if (length == 0) {
      return true;
    }
    if (set instanceof BoundedNatBitSet) {
      BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;
      return length <= boundedSet.domainSize() && isModifiable(boundedSet);
    }
    if (set instanceof LongNatBitSet) {
      return length <= LongNatBitSet.maximalSize();
    }
    return set instanceof SimpleNatBitSet || set instanceof SparseNatBitSet;
  }

  public static NatBitSet longSet() {
    return new LongNatBitSet();
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
  public static NatBitSet modifiableCopyOf(NatBitSet set, @Nonnegative int length) {
    if (isModifiable(set, length)) {
      return set.clone();
    }
    if (set instanceof BoundedNatBitSet && length <= ((BoundedNatBitSet) set).domainSize()) {
      return modifiableCopyOf((BoundedNatBitSet) set);
    }

    NatBitSet copy = set(set.size(), length);
    copy.or(set);
    assert isModifiable(copy, length) && copy.equals(set);
    return copy;
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable (within the
   * domain).
   *
   * @see #isModifiable(BoundedNatBitSet)
   */
  public static BoundedNatBitSet modifiableCopyOf(BoundedNatBitSet set) {
    if (isModifiable(set)) {
      return set.clone();
    }

    BoundedNatBitSet copy = boundedSet(set.domainSize(), set.size());
    copy.or(set);
    assert isModifiable(copy, set.domainSize()) && copy.equals(set);
    return copy;
  }

  /**
   * Returns the set containing all subsets of the given basis.
   * <strong>Warning</strong>: For performance reasons, the iterator of this set may modify the
   * returned elements in place.
   */
  public static Set<NatBitSet> powerSet(NatBitSet basis) {
    if (basis.isEmpty()) {
      return Collections.singleton(emptySet());
    }
    return new PowerNatBitSet(basis);
  }

  /**
   * Returns the set containing subsets of {0, ..., i-1}.
   * <strong>Warning</strong>: For performance reasons, the iterator of this set may modify the
   * returned elements in place.
   */
  public static Set<NatBitSet> powerSet(@Nonnegative int domainSize) {
    return powerSet(fullSet(domainSize));
  }

  static int previousAbsentIndex(SparseBitSet set, @Nonnegative int index) {
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
      int mid = (high + low) / 2;
      int next = set.nextClearBit(mid);
      while (next > index) {
        assert low <= mid && mid <= high;
        high = mid;
        mid = (high + low) / 2;
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

  static int previousPresentIndex(SparseBitSet set, @Nonnegative int index) {
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
      int mid = (high + low) / 2;
      int next = set.nextSetBit(mid);
      while (next == -1 || next > index) {
        assert low <= mid && mid <= high;
        high = mid;
        mid = (high + low) / 2;
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
  public static NatBitSet setWithExpectedLength(@Nonnegative int expectedLength) {
    return set(UNKNOWN_SIZE, expectedLength);
  }

  /**
   * Creates a modifiable set with expected size.
   */
  public static NatBitSet setWithExpectedSize(@Nonnegative int expectedSize) {
    return set(expectedSize, UNKNOWN_LENGTH);
  }

  /**
   * Creates a modifiable set which is modifiable up to {@code maximalLength}.
   */
  public static NatBitSet setWithMaximalLength(int maximalLength) {
    if (maximalLength < LongNatBitSet.maximalSize()) {
      return new LongNatBitSet();
    }
    return set(UNKNOWN_SIZE, maximalLength);
  }

  public static NatBitSet simpleSet() {
    return new SimpleNatBitSet(new BitSet());
  }

  public static NatBitSet simpleSet(@Nonnegative int expectedSize) {
    return new SimpleNatBitSet(new BitSet(expectedSize));
  }

  public static NatBitSet singleton(@Nonnegative int element) {
    return new MutableSingletonNatBitSet(element);
  }

  public static NatBitSet sparseSet() {
    return new SparseNatBitSet(new SparseBitSet());
  }

  public static NatBitSet sparseSet(@Nonnegative int expectedSize) {
    return new SparseNatBitSet(new SparseBitSet(expectedSize));
  }

  public static BitSet toBitSet(IntIterable indices) {
    if (!(indices instanceof IntCollection)) {
      BitSet bitSet = new BitSet();
      indices.forEach((IntConsumer) bitSet::set);
      return bitSet;
    }
    if (((Collection<?>) indices).isEmpty()) {
      return new BitSet(0);
    }

    if (indices instanceof SimpleNatBitSet) {
      return (BitSet) ((SimpleNatBitSet) indices).getBitSet().clone();
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet boundedSet = (SimpleBoundedNatBitSet) indices;
      BitSet bitSet = (BitSet) boundedSet.getBitSet().clone();
      if (boundedSet.isComplement()) {
        bitSet.flip(0, boundedSet.domainSize());
      }
      return bitSet;
    }
    if (indices instanceof SparseNatBitSet) {
      return BitSets.toBitSet(((SparseNatBitSet) indices).getSparseBitSet());
    }
    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet boundedSet = (SparseBoundedNatBitSet) indices;
      if (boundedSet.isComplement()) {
        BitSet bitSet = new BitSet(boundedSet.domainSize());
        boundedSet.forEach((IntConsumer) bitSet::set);
        return bitSet;
      }
      return BitSets.toBitSet(boundedSet.getSparseBitSet());
    }

    BitSet bitSet;
    if (indices instanceof NatBitSet) {
      bitSet = new BitSet(((NatBitSet) indices).lastInt());
    } else if (indices instanceof IntSortedSet) {
      bitSet = new BitSet(((IntSortedSet) indices).lastInt());
    } else {
      bitSet = new BitSet();
    }
    indices.forEach((IntConsumer) bitSet::set);
    return bitSet;
  }

  public static SparseBitSet toSparseBitSet(IntIterable indices) {
    if (!(indices instanceof IntCollection)) {
      SparseBitSet bitSet = new SparseBitSet();
      indices.forEach((IntConsumer) bitSet::set);
      return bitSet;
    }
    if (((Collection<?>) indices).isEmpty()) {
      return new SparseBitSet(0);
    }

    if (indices instanceof SimpleNatBitSet) {
      return BitSets.toSparse(((SimpleNatBitSet) indices).getBitSet());
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet boundedSet = (SimpleBoundedNatBitSet) indices;
      if (boundedSet.isComplement()) {
        SparseBitSet bitSet = new SparseBitSet(boundedSet.domainSize());
        boundedSet.forEach((IntConsumer) bitSet::set);
        return bitSet;
      }
      return BitSets.toSparse(boundedSet.getBitSet());
    }
    if (indices instanceof SparseNatBitSet) {
      return ((SparseNatBitSet) indices).getSparseBitSet().clone();
    }
    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet boundedSet = (SparseBoundedNatBitSet) indices;
      SparseBitSet bitSet = boundedSet.getSparseBitSet().clone();
      if (boundedSet.isComplement()) {
        bitSet.flip(0, boundedSet.domainSize());
      }
      return bitSet;
    }

    SparseBitSet bitSet;
    if (indices instanceof NatBitSet) {
      bitSet = new SparseBitSet(((NatBitSet) indices).lastInt());
    } else if (indices instanceof IntSortedSet) {
      bitSet = new SparseBitSet(((IntSortedSet) indices).lastInt());
    } else {
      bitSet = new SparseBitSet();
    }
    indices.forEach((IntConsumer) bitSet::set);
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
