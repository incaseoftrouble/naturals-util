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
import it.unimi.dsi.fastutil.ints.IntIterators;
import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnegative;

public final class NatBitSets {
  public static final int UNKNOWN_LENGTH = -1;
  public static final int UNKNOWN_SIZE = -1;
  private static NatBitSetFactory factory = new DefaultNatBitSetFactory();

  private NatBitSets() {}

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
    if (set.isEmpty() || set.firstInt() >= length) {
      return IntIterators.fromTo(0, length);
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
    if (set.isEmpty() || set.firstInt() >= length) {
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
   * Sets the default factory.
   *
   * <strong>Warning:</strong> This method is only intended for local performance testing, e.g.,
   * whether using only sparse or simple bit sets is faster. This should not remain in production
   * code, since it could lead to conflicts.
   */
  public static void setFactory(NatBitSetFactory factory) {
    Objects.requireNonNull(factory);
    NatBitSets.factory = factory;
  }


  // --- Delegated Methods ---

  /**
   * Determines whether the given {@code set} can handle arbitrary (positive) modifications.
   */
  public static boolean isModifiable(NatBitSet set) {
    return factory.isModifiable(set);
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary modifications within its domain.
   */
  public static boolean isModifiable(BoundedNatBitSet set) {
    return factory.isModifiable(set);
  }

  /**
   * Determines whether the given {@code set} can handle arbitrary modifications of values between 0
   * and {@code length - 1}.
   */
  public static boolean isModifiable(NatBitSet set, @Nonnegative int length) {
    return factory.isModifiable(set, length);
  }


  /**
   * Returns a modifiable set over the specified domain which contains the whole domain.
   */
  public static BoundedNatBitSet boundedFilledSet(int domainSize) {
    return factory.boundedFilledSet(domainSize);
  }

  public static BoundedNatBitSet boundedFilledSet(int domainSize, int expectedSize) {
    return factory.boundedFilledSet(domainSize, expectedSize);
  }

  public static BoundedNatBitSet boundedSet(int domainSize) {
    return factory.boundedSet(domainSize, UNKNOWN_SIZE);
  }

  public static BoundedNatBitSet boundedSet(int domainSize, int expectedSize) {
    return factory.boundedSet(domainSize, expectedSize);
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
    return factory.compact(set, false);
  }

  public static NatBitSet compact(NatBitSet set, boolean forceCopy) {
    return factory.compact(set, forceCopy);
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
  public static NatBitSet copyOf(Collection<Integer> indices) {
    return factory.copyOf(indices);
  }

  /**
   * Ensures that the given {@code set} is a {@link BoundedNatBitSet}, copying it if necessary.
   * Note that this also clones the set if, e.g., it is a bounded set with a larger domain.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code set} contains an index larger than {@code domainSize}.
   */
  public static BoundedNatBitSet ensureBounded(NatBitSet set, @Nonnegative int domainSize) {
    return factory.ensureBounded(set, domainSize);
  }

  /**
   * Ensures that the given {@code set} can be modified with arbitrary values. If necessary, the set
   * is copied into a general purpose representation.
   *
   * @see #isModifiable(NatBitSet)
   */
  public static NatBitSet ensureModifiable(NatBitSet set) {
    return factory.ensureModifiable(set);
  }

  /**
   * Ensures that the given {@code set} can be modified with arbitrary values from
   * {@code {0, ..., n}}. If necessary, the set is copied into a general purpose representation.
   *
   * @see #isModifiable(NatBitSet, int)
   */
  public static NatBitSet ensureModifiable(NatBitSet set, @Nonnegative int length) {
    return factory.ensureModifiable(set, length);
  }

  /**
   * Ensures that the given {@code set} can be modified in its domain. If necessary, the set is
   * copied into a general purpose representation.
   */
  public static NatBitSet ensureModifiable(BoundedNatBitSet set) {
    return factory.ensureModifiable(set);
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable.
   *
   * @see #isModifiable(NatBitSet)
   */
  public static NatBitSet modifiableCopyOf(NatBitSet set) {
    return factory.modifiableCopyOf(set);
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable up to
   * {@code length - 1}.
   *
   * @see #isModifiable(NatBitSet, int)
   */
  public static NatBitSet modifiableCopyOf(NatBitSet set, @Nonnegative int length) {
    return factory.modifiableCopyOf(set, length);
  }

  /**
   * Returns a copy of the given {@code set} which is guaranteed to be modifiable (within the
   * domain).
   *
   * @see #isModifiable(BoundedNatBitSet)
   */
  public static BoundedNatBitSet modifiableCopyOf(BoundedNatBitSet set) {
    return factory.modifiableCopyOf(set);
  }

  /**
   * Creates a modifiable set with the expected size and length.
   */
  public static NatBitSet set(int expectedSize, int expectedLength) {
    return factory.set(expectedSize, expectedLength);
  }

  /**
   * Creates a modifiable set.
   */
  public static NatBitSet set() {
    return factory.set();
  }

  /**
   * Creates a modifiable set with expected length.
   */
  public static NatBitSet setWithExpectedLength(@Nonnegative int expectedLength) {
    return factory.setWithExpectedLength(expectedLength);
  }

  /**
   * Creates a modifiable set with expected size.
   */
  public static NatBitSet setWithExpectedSize(@Nonnegative int expectedSize) {
    return factory.setWithExpectedSize(expectedSize);
  }

  /**
   * Creates a modifiable set which is modifiable up to {@code maximalLength}.
   */
  public static NatBitSet setWithMaximalLength(int maximalLength) {
    return factory.setWithExpectedSize(maximalLength);
  }
}
