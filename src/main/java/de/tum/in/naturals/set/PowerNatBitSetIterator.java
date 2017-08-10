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
import java.util.Iterator;
import java.util.NoSuchElementException;

final class PowerNatBitSetIterator implements Iterator<NatBitSet> {
  private final NatBitSet baseSet;
  private boolean hasNext = true;
  private final NatBitSet current;

  PowerNatBitSetIterator(NatBitSet baseSet) {
    assert !baseSet.isEmpty();
    this.baseSet = baseSet;
    this.current = NatBitSets.setWithMaximalLength(baseSet.lastInt());
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public NatBitSet next() {
    if (!hasNext) {
      throw new NoSuchElementException("No next element");
    }

    hasNext = false;
    IntIterator iterator = baseSet.iterator();
    while (iterator.hasNext()) {
      int index = iterator.nextInt();
      if (current.contains(index)) {
        current.clear(index);
      } else {
        hasNext = true;
        current.set(index);
        break;
      }
    }

    return current;
  }
}
