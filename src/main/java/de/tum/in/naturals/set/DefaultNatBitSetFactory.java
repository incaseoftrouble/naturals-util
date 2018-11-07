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

import com.zaxxer.sparsebits.SparseBitSet;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;

public class DefaultNatBitSetFactory implements NatBitSetFactory {
  private static final int SPARSE_THRESHOLD = Long.SIZE * 128;

  private final BiPredicate<Integer, Integer> useSparse;

  DefaultNatBitSetFactory() {
    this(DefaultNatBitSetFactory::useSparse);
  }

  public DefaultNatBitSetFactory(BiPredicate<Integer, Integer> useSparse) {
    this.useSparse = useSparse;
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
        || (expectedSize != UNKNOWN_SIZE && expectedSize > SPARSE_THRESHOLD);
  }

  @Override
  public BoundedNatBitSet boundedSet(int domainSize, int expectedSize) {
    if (domainSize <= LongBoundedNatBitSet.maximalSize()) {
      return new LongBoundedNatBitSet(domainSize);
    }
    return (useSparse.test(expectedSize, domainSize))
        ? new SparseBoundedNatBitSet(new SparseBitSet(domainSize), domainSize)
        : new SimpleBoundedNatBitSet(new BitSet(), domainSize);
  }

  @Override
  public boolean isModifiable(BoundedNatBitSet set) {
    return set instanceof LongBoundedNatBitSet
        || set instanceof SimpleBoundedNatBitSet
        || set instanceof SparseBoundedNatBitSet;
  }

  @Override
  public NatBitSet compact(NatBitSet set, boolean forceCopy) {
    if (set instanceof MutableSingletonNatBitSet || set instanceof FixedSizeNatBitSet) {
      return set;
    }
    if (set.isEmpty()) {
      return NatBitSetProvider.emptySet();
    }
    if (set.size() == 1) {
      return NatBitSetProvider.singleton(set.firstInt());
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

  @Override
  public NatBitSet set(int expectedSize, int expectedLength) {
    if (useSparse.test(expectedSize, expectedLength)) {
      SparseBitSet backingSet = expectedLength == UNKNOWN_LENGTH
          ? new SparseBitSet() : new SparseBitSet(expectedLength);
      return new SparseNatBitSet(backingSet);
    }
    return new SimpleNatBitSet(new BitSet());
  }

  @Override
  public NatBitSet copyOf(Collection<Integer> indices) {
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

  @Override
  public boolean isModifiable(NatBitSet set, int length) {
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

  /**
   * Ensures that the given {@code set} is a {@link BoundedNatBitSet}, copying it if necessary.
   * Note that this also clones the set if, e.g., it is a bounded set with a larger domain.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code set} contains an index larger than {@code domainSize}.
   */
  @Override
  public BoundedNatBitSet ensureBounded(NatBitSet set, @Nonnegative int domainSize) {
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
}
