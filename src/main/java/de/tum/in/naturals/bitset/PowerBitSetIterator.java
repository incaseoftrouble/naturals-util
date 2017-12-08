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

package de.tum.in.naturals.bitset;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

class PowerBitSetIterator implements Iterator<BitSet> {
  private final BitSet baseSet;
  private final BitSet iteration;
  private final int baseCardinality;
  private int numSetBits = -1;

  PowerBitSetIterator(BitSet baseSet) {
    this.baseSet = baseSet;
    this.baseCardinality = baseSet.cardinality();
    this.iteration = new BitSet(baseSet.length());
  }

  @Override
  public boolean hasNext() {
    return numSetBits < baseCardinality;
  }

  @Override
  public BitSet next() {
    if (numSetBits == -1) {
      numSetBits = 0;
      return iteration;
    }

    if (numSetBits == baseCardinality) {
      throw new NoSuchElementException("No next element");
    }

    IntIterator iterator = BitSets.iterator(baseSet);
    while (iterator.hasNext()) {
      int index = iterator.nextInt();
      if (iteration.get(index)) {
        iteration.clear(index);
        numSetBits -= 1;
      } else {
        iteration.set(index);
        numSetBits += 1;
        break;
      }
    }

    return iteration;
  }
}
