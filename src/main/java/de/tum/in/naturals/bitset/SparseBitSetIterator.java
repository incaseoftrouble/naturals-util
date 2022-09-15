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

import com.zaxxer.sparsebits.SparseBitSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

final class SparseBitSetIterator implements IntIterator {
  private final SparseBitSet bitSet;
  private int current = -1;
  private int next;

  SparseBitSetIterator(SparseBitSet bitSet) {
    this.bitSet = bitSet;
    this.next = bitSet.nextSetBit(0);
  }

  private int getNext(int index) {
    return bitSet.nextSetBit(index);
  }

  @Override
  public boolean hasNext() {
    return next != -1;
  }

  @Override
  public int nextInt() {
    if (next == -1) {
      throw new NoSuchElementException();
    }
    current = next;
    next = getNext(next + 1);
    return current;
  }

  @Override
  public void remove() {
    if (current == -1) {
      throw new IllegalStateException();
    }
    assert bitSet.get(current);
    bitSet.clear(current);
    current = -1;
  }
}
