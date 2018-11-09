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

import it.unimi.dsi.fastutil.Size64;
import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.AbstractSet;
import java.util.Iterator;
import javax.annotation.Nullable;

class PowerNatBitSet extends AbstractSet<NatBitSet> implements Size64 {
  private final NatBitSet baseSet;
  private final int baseSize;

  PowerNatBitSet(NatBitSet baseSet) {
    assert !baseSet.isEmpty();
    this.baseSet = NatBitSets.compact(baseSet, true);
    baseSize = this.baseSet.size();
  }

  @Override
  public boolean isEmpty() {
    return false;
  }


  @Override
  public boolean contains(@Nullable Object obj) {
    return obj instanceof IntCollection && baseSet.containsAll((IntCollection) obj);
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof PowerNatBitSet) {
      PowerNatBitSet other = (PowerNatBitSet) obj;
      return baseSet.equals(other.baseSet);
    }
    return super.equals(obj);
  }

  /**
   * Returns an iterator over the power set.
   * <strong>Warning</strong>: To avoid repeated allocation, the returned set is modified in-place!
   */
  @Override
  public Iterator<NatBitSet> iterator() {
    return new PowerNatBitSetIterator(baseSet);
  }

  @SuppressWarnings("deprecation")
  @Override
  public int size() {
    return baseSize >= Integer.SIZE ? Integer.MAX_VALUE : 1 << baseSize;
  }

  @Override
  public long size64() {
    return 1L << baseSize;
  }

  @Override
  public String toString() {
    return String.format("powerSet(%s)", baseSet);
  }
}
