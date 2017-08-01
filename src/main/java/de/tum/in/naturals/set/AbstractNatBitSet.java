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
import java.util.function.IntConsumer;

abstract class AbstractNatBitSet extends AbstractIntSet implements NatBitSet {
  @Override
  public boolean add(int key) {
    if (contains(key)) {
      return true;
    }
    set(key);
    return false;
  }

  @Override
  public void and(IntCollection ints) {
    IntIterator iterator = iterator();
    while (iterator.hasNext()) {
      int next = iterator.nextInt();
      if (!ints.contains(next)) {
        iterator.remove();
      }
    }
  }

  @Override
  public void andNot(IntCollection ints) {
    IntIterator iterator = iterator();
    while (iterator.hasNext()) {
      int next = iterator.nextInt();
      if (ints.contains(next)) {
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
  public boolean intersects(IntCollection ints) {
    return IntIterators.any(ints.iterator(), this::contains);
  }

  @Override
  public void or(IntCollection ints) {
    ints.forEach((IntConsumer) this::set);
  }

  @Override
  public boolean remove(int key) {
    if (!contains(key)) {
      return false;
    }
    clear(key);
    return false;
  }

  @Override
  public boolean removeAll(IntCollection ints) {
    if (!intersects(ints)) {
      return false;
    }
    andNot(ints);
    return true;
  }

  @Override
  public boolean retainAll(IntCollection ints) {
    if (IntIterators.all(iterator(), ints::contains)) {
      return false;
    }
    and(ints);
    return true;
  }

  @Override
  public void xor(IntCollection ints) {
    if (ints instanceof IntSet) {
      ints.forEach((IntConsumer) this::flip);
    } else {
      NatBitSets.copyOf(ints).forEach((IntConsumer) this::flip);
    }
  }
}
