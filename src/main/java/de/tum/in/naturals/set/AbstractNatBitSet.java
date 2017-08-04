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

import it.unimi.dsi.fastutil.ints.AbstractIntSet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

abstract class AbstractNatBitSet extends AbstractIntSet implements NatBitSet {
  protected static void checkNonNegative(int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException(String.format("Negative index %d ", index));
    }
  }

  protected static void checkRange(int from, int to) {
    if (to < from) {
      throw new IndexOutOfBoundsException(String.format("From %d bigger than to %d", from, to));
    }
    if (from < 0) {
      throw new IndexOutOfBoundsException(String.format("Negative from index %d ", from));
    }
  }

  @Override
  public boolean add(int index) {
    if (contains(index)) {
      return false;
    }
    set(index);
    return true;
  }

  @Override
  public void and(IntCollection indices) {
    if (indices.isEmpty()) {
      clear();
    } else {
      IntIterator iterator = iterator();
      while (iterator.hasNext()) {
        int next = iterator.nextInt();
        if (!indices.contains(next)) {
          iterator.remove();
        }
      }
    }
  }

  @Override
  public void andNot(IntCollection indices) {
    if (indices.isEmpty()) {
      return;
    }
    IntIterator iterator = iterator();
    while (iterator.hasNext()) {
      int next = iterator.nextInt();
      if (indices.contains(next)) {
        iterator.remove();
      }
    }
  }

  @Override
  public AbstractNatBitSet clone() {
    try {
      return (AbstractNatBitSet) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError(e);
    }
  }

  @Override
  public int firstInt() {
    int firstPresent = nextPresentIndex(0);
    if (firstPresent == -1) {
      throw new NoSuchElementException();
    }
    return firstPresent;
  }

  @Override
  public boolean intersects(IntCollection indices) {
    return IntIterators.any(indices.iterator(), this::contains);
  }

  @Override
  public void or(IntCollection indices) {
    if (indices.isEmpty()) {
      return;
    }
    indices.forEach((IntConsumer) this::set);
  }

  @Override
  public boolean remove(int index) {
    if (!contains(index)) {
      return false;
    }
    clear(index);
    return true;
  }

  @Override
  public boolean removeAll(IntCollection indices) {
    if (!intersects(indices)) {
      return false;
    }
    andNot(indices);
    return true;
  }

  @Override
  public boolean retainAll(IntCollection indices) {
    if (IntIterators.all(iterator(), indices::contains)) {
      return false;
    }
    and(indices);
    return true;
  }

  @Override
  public void xor(IntCollection indices) {
    if (indices.isEmpty()) {
      return;
    }
    IntSet set = indices instanceof IntSet ? (IntSet) indices : NatBitSets.copyOf(indices);
    set.forEach((IntConsumer) this::flip);
  }
}
