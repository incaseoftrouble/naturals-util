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

import com.zaxxer.sparsebits.SparseBitSet;
import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.SparseBitSets;
import java.util.BitSet;
import java.util.Collections;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;

public final class NatBitSetProvider {
  private NatBitSetProvider() {}

  // --- Unbounded Sets ---

  /**
   * Return a view on the given {@code bitSet}.
   */
  public static NatBitSet asSet(BitSet bitSet) {
    return new SimpleNatBitSet(bitSet);
  }

  /**
   * Return a view on the given {@code bitSet}.
   */
  public static NatBitSet asSet(SparseBitSet bitSet) {
    return new SparseNatBitSet(bitSet);
  }


  public static NatBitSet longSet() {
    return new LongNatBitSet();
  }

  public static NatBitSet simpleSet() {
    return new SimpleNatBitSet(new BitSet());
  }

  public static NatBitSet simpleSet(@Nonnegative int expectedSize) {
    return new SimpleNatBitSet(new BitSet(expectedSize));
  }

  public static NatBitSet sparseSet() {
    return new SparseNatBitSet(new SparseBitSet());
  }

  public static NatBitSet sparseSet(@Nonnegative int expectedSize) {
    return new SparseNatBitSet(new SparseBitSet(expectedSize));
  }


  // --- Bounded Sets ---

  /**
   * Return a view on the given {@code bitSet}.
   */
  public static BoundedNatBitSet asBoundedSet(BitSet bitSet, int domainSize) {
    return new SimpleBoundedNatBitSet(bitSet, domainSize);
  }

  /**
   * Return a view on the given {@code bitSet}.
   */
  public static BoundedNatBitSet asBoundedSet(SparseBitSet bitSet, int domainSize) {
    return new SparseBoundedNatBitSet(bitSet, domainSize);
  }


  public static BoundedNatBitSet boundedLongSet(int domainSize) {
    return new LongBoundedNatBitSet(domainSize);
  }

  public static BoundedNatBitSet boundedSimpleSet(int domainSize) {
    return new SimpleBoundedNatBitSet(new BitSet(), domainSize);
  }

  public static BoundedNatBitSet boundedSparseSet(int domainSize) {
    return new SparseBoundedNatBitSet(new SparseBitSet(), domainSize);
  }


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

  // --- Special Cases ---

  // Empty

  /**
   * Returns an empty set.
   */
  public static NatBitSet emptySet() {
    return new MutableSingletonNatBitSet();
  }

  /**
   * Returns an empty set over the given domain.
   */
  public static BoundedNatBitSet boundedEmptySet(@Nonnegative int domainSize) {
    return new FixedSizeNatBitSet(domainSize).complement();
  }

  // Singleton

  public static NatBitSet singleton(@Nonnegative int element) {
    return new MutableSingletonNatBitSet(element);
  }

  public static BoundedNatBitSet boundedSingleton(int domainSize, int element) {
    return new BoundedMutableSingletonNatBitSet(element, domainSize);
  }

  // Full

  /**
   * Returns the set containing the full domain {@code {0, ..., length - 1}}.
   */
  public static BoundedNatBitSet boundedFullSet(@Nonnegative int length) {
    return new FixedSizeNatBitSet(length);
  }

  // Power

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
    return powerSet(boundedFullSet(domainSize));
  }


  // --- Extraction ---

  public static BitSet toBitSet(NatBitSet indices) {
    if (indices.isEmpty()) {
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
      return BitSets.of(((SparseNatBitSet) indices).getSparseBitSet());
    }
    if (indices instanceof SparseBoundedNatBitSet) {
      SparseBoundedNatBitSet boundedSet = (SparseBoundedNatBitSet) indices;
      if (boundedSet.isComplement()) {
        BitSet bitSet = new BitSet(boundedSet.domainSize());
        boundedSet.forEach((IntConsumer) bitSet::set);
        return bitSet;
      }
      return BitSets.of(boundedSet.getSparseBitSet());
    }

    BitSet bitSet = new BitSet(indices.lastInt() + 1);
    indices.forEach((IntConsumer) bitSet::set);
    return bitSet;
  }

  public static SparseBitSet toSparseBitSet(NatBitSet indices) {
    if (indices.isEmpty()) {
      return new SparseBitSet(1); // 0 is buggy here
    }
    if (indices instanceof SimpleNatBitSet) {
      return SparseBitSets.of(((SimpleNatBitSet) indices).getBitSet());
    }
    if (indices instanceof SimpleBoundedNatBitSet) {
      SimpleBoundedNatBitSet boundedSet = (SimpleBoundedNatBitSet) indices;
      if (boundedSet.isComplement()) {
        SparseBitSet bitSet = new SparseBitSet(boundedSet.domainSize());
        boundedSet.forEach((IntConsumer) bitSet::set);
        return bitSet;
      }
      return SparseBitSets.of(boundedSet.getBitSet());
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

    SparseBitSet bitSet = new SparseBitSet(indices.lastInt() + 1);
    indices.forEach((IntConsumer) bitSet::set);
    return bitSet;
  }
}
