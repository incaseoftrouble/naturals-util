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
import java.util.BitSet;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;
import org.roaringbitmap.RoaringBitmap;

public abstract class AbstractNatBitSetFactory implements NatBitSetFactory {
  @Override
  public BoundedNatBitSet boundedSet(int domainSize, int expectedSize) {
    if (domainSize <= LongBoundedNatBitSet.maximalSize()) {
      return new LongBoundedNatBitSet(domainSize);
    }
    return makeBoundedSet(domainSize, expectedSize);
  }

  protected abstract BoundedNatBitSet makeBoundedSet(int domainSize, int expectedSize);

  @Override
  public boolean isModifiable(BoundedNatBitSet set) {
    return set instanceof LongBoundedNatBitSet
        || set instanceof SimpleBoundedNatBitSet
        || set instanceof SparseBoundedNatBitSet
        || set instanceof RoaringBoundedNatBitSet;
  }

  @Override
  public NatBitSet compact(NatBitSet set, boolean forceCopy) {
    if (set instanceof MutableSingletonNatBitSet || set instanceof FixedSizeNatBitSet) {
      return set;
    }
    if (set.isEmpty()) {
      return NatBitSets.emptySet();
    }
    if (set.size() == 1) {
      return NatBitSets.singleton(set.firstInt());
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
    return set instanceof SimpleNatBitSet
        || set instanceof SparseNatBitSet
        || set instanceof RoaringNatBitSet;
  }

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
        SparseBitSet bitSetCopy = sparseBoundedSet.getBitSet().clone();
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
      } else if (set instanceof RoaringBoundedNatBitSet) {
        RoaringBoundedNatBitSet sparseBoundedSet = (RoaringBoundedNatBitSet) set;
        RoaringBitmap bitmapCopy = sparseBoundedSet.getBitmap().clone();
        if (sparseBoundedSet.isComplement()) {
          if (domainSize < oldDomainSize) {
            bitmapCopy.remove((long) domainSize, (long) oldDomainSize);
          } else {
            bitmapCopy.add((long) oldDomainSize, (long) domainSize);
          }
        }
        BoundedNatBitSet copy = new RoaringBoundedNatBitSet(bitmapCopy, domainSize);
        return sparseBoundedSet.isComplement() ? copy.complement() : copy;
      }
    }
    BoundedNatBitSet copy = boundedSet(domainSize, set.size());
    copy.or(set);
    return copy;
  }
}
