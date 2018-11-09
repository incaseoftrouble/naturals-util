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

package de.tum.in.naturals.set;

import static de.tum.in.naturals.set.NatBitSets.UNKNOWN_LENGTH;
import static de.tum.in.naturals.set.NatBitSets.UNKNOWN_SIZE;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnegative;

public interface NatBitSetFactory {

  // --- Unbounded Sets ---

  // Constructors

  /**
   * Creates a modifiable set with the expected size and length.
   */
  NatBitSet set(int expectedSize, int expectedLength);

  /**
   * Creates a modifiable set.
   */
  default NatBitSet set() {
    return set(UNKNOWN_SIZE, UNKNOWN_LENGTH);
  }

  /**
   * Creates a modifiable set with expected length.
   */
  default NatBitSet setWithExpectedLength(@Nonnegative int expectedLength) {
    return set(UNKNOWN_SIZE, expectedLength);
  }

  /**
   * Creates a modifiable set with expected size.
   */
  default NatBitSet setWithExpectedSize(@Nonnegative int expectedSize) {
    return set(expectedSize, UNKNOWN_LENGTH);
  }

  /**
   * Creates a modifiable set which is modifiable up to {@code maximalLength}.
   */
  default NatBitSet setWithMaximalLength(int maximalLength) {
    if (maximalLength < LongNatBitSet.maximalSize()) {
      return new LongNatBitSet();
    }
    return set(UNKNOWN_SIZE, maximalLength);
  }

  // Copies

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
  default NatBitSet copyOf(Collection<Integer> indices) {
    NatBitSet copy;
    if (indices.isEmpty()) {
      copy = NatBitSetProvider.emptySet();
    } else if (indices instanceof NatBitSet) {
      copy = ((NatBitSet) indices).clone();
    } else if (indices instanceof IntSortedSet) {
      copy = set(indices.size(), ((IntSortedSet) indices).lastInt());
      copy.or((IntSortedSet) indices);
    } else {
      copy = set(indices.size(), UNKNOWN_LENGTH);
      copy.addAll(indices);
    }
    assert copy.equals(indices instanceof Set ? indices : new IntAVLTreeSet(indices));
    return copy;
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable.
   *
   * @see NatBitSets#isModifiable(NatBitSet)
   */
  default NatBitSet modifiableCopyOf(NatBitSet set) {
    return modifiableCopyOf(set, Integer.MAX_VALUE);
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable up to
   * {@code length - 1}.
   *
   * @see NatBitSets#isModifiable(NatBitSet, int)
   */
  default NatBitSet modifiableCopyOf(NatBitSet set, @Nonnegative int length) {
    if (NatBitSets.isModifiable(set, length)) {
      return set.clone();
    }
    if (set instanceof BoundedNatBitSet && length <= ((BoundedNatBitSet) set).domainSize()) {
      return modifiableCopyOf((BoundedNatBitSet) set);
    }

    NatBitSet copy = set(set.size(), length);
    copy.or(set);
    assert NatBitSets.isModifiable(copy, length) && copy.equals(set);
    return copy;
  }

  // Ensures

  /**
   * Determines whether the given {@code set} can handle arbitrary (positive) modifications.
   */
  default boolean isModifiable(NatBitSet set) {
    return isModifiable(set, Integer.MAX_VALUE);
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary modifications of values between 0
   * and {@code length - 1}.
   */
  boolean isModifiable(NatBitSet set, @Nonnegative int length);

  /**
   * Ensures that the given {@code set} can be modified with arbitrary values. If necessary, the set
   * is copied into a general purpose representation.
   *
   * @see #isModifiable(NatBitSet)
   */
  default NatBitSet ensureModifiable(NatBitSet set) {
    return NatBitSets.isModifiable(set) ? set : modifiableCopyOf(set);
  }

  /**
   * Ensures that the given {@code set} can be modified with arbitrary values from
   * {@code {0, ..., n}}. If necessary, the set is copied into a general purpose representation.
   *
   * @see #isModifiable(NatBitSet, int)
   */
  default NatBitSet ensureModifiable(NatBitSet set, @Nonnegative int length) {
    return NatBitSets.isModifiable(set, length) ? set : modifiableCopyOf(set, length);
  }


  // --- Bounded Sets ---

  // Constructors

  BoundedNatBitSet boundedSet(int domainSize, int expectedSize);

  default BoundedNatBitSet boundedSet(int domainSize) {
    return boundedSet(domainSize, UNKNOWN_SIZE);
  }

  // Special cases

  /**
   * Returns a modifiable set over the specified domain which contains the whole domain.
   */
  default BoundedNatBitSet boundedFilledSet(int domainSize) {
    return boundedFilledSet(domainSize, UNKNOWN_SIZE);
  }

  default BoundedNatBitSet boundedFilledSet(int domainSize, int expectedSize) {
    return boundedSet(domainSize, expectedSize).complement();
  }

  // Copies

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable (within the
   * domain).
   *
   * @see NatBitSets#isModifiable(BoundedNatBitSet)
   */
  default BoundedNatBitSet modifiableCopyOf(BoundedNatBitSet set) {
    if (NatBitSets.isModifiable(set)) {
      return set.clone();
    }

    BoundedNatBitSet copy = boundedSet(set.domainSize(), set.size());
    copy.or(set);
    assert NatBitSets.isModifiable(copy, set.domainSize()) && copy.equals(set);
    return copy;
  }

  // Ensures

  /**
   * Determines whether the given {@code set} can handle arbitrary modifications within its domain.
   */
  boolean isModifiable(BoundedNatBitSet set);

  BoundedNatBitSet ensureBounded(NatBitSet set, @Nonnegative int domainSize);

  /**
   * Ensures that the given {@code set} can be modified in its domain. If necessary, the set is
   * copied into a general purpose representation.
   */
  default NatBitSet ensureModifiable(BoundedNatBitSet set) {
    return NatBitSets.isModifiable(set) ? set : modifiableCopyOf(set);
  }


  // --- Compaction ---

  /**
   * Try to compact the given set by potentially representing it as, e.g., empty or singleton set.
   * The returned set might not be modifiable.
   *
   * @param set
   *     The set to be compacted.
   *
   * @return a potentially compacted representation of the given set.
   */
  default NatBitSet compact(NatBitSet set) {
    return compact(set, false);
  }

  NatBitSet compact(NatBitSet set, boolean forceCopy);
}
