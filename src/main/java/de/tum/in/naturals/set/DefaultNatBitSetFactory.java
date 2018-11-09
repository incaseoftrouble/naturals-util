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
import java.util.BitSet;
import java.util.function.BiPredicate;

public class DefaultNatBitSetFactory extends AbstractNatBitSetFactory {
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
  protected BoundedNatBitSet makeBoundedSet(int domainSize, int expectedSize) {
    return (useSparse.test(expectedSize, domainSize))
        ? new SparseBoundedNatBitSet(new SparseBitSet(domainSize), domainSize)
        : new SimpleBoundedNatBitSet(new BitSet(), domainSize);
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
}
