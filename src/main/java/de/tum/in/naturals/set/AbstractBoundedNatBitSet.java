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

import it.unimi.dsi.fastutil.ints.IntCollection;
import javax.annotation.Nonnegative;

abstract class AbstractBoundedNatBitSet extends AbstractNatBitSet implements BoundedNatBitSet {
  @Nonnegative
  private final int domainSize;

  protected AbstractBoundedNatBitSet(@Nonnegative int domainSize) {
    if (domainSize < 0) {
      throw new IllegalArgumentException(String.format("Invalid domain size %d", domainSize));
    }
    this.domainSize = domainSize;
  }

  @Override
  public boolean add(int index) {
    checkInDomain(index);
    return super.add(index);
  }

  protected void checkInDomain(int from, int to) {
    checkRange(from, to);
    if (domainSize < to) {
      throw new IndexOutOfBoundsException(String.format("To index %d too large for domain [0, %d)",
          to, domainSize));
    }
  }

  protected void checkInDomain(int index) {
    checkNonNegative(index);
    if (domainSize <= index) {
      throw new IndexOutOfBoundsException(String.format("Index %d too large for domain [0, %d)",
          index, domainSize));
    }
  }

  @Override
  public void clearFrom(int from) {
    checkInDomain(from);
    clear(from, domainSize());
  }

  @Override
  public AbstractBoundedNatBitSet clone() {
    return (AbstractBoundedNatBitSet) super.clone();
  }

  @Override
  @Nonnegative
  public int domainSize() {
    return domainSize;
  }

  protected boolean inDomain(int index) {
    return 0 <= index && index < domainSize;
  }

  abstract boolean isComplement();

  @Override
  public void orNot(IntCollection indices) {
    if (indices.isEmpty()) {
      set(0, domainSize);
    } else {
      for (int i = 0; i < domainSize(); i++) {
        if (!indices.contains(i)) {
          set(i);
        }
      }
    }
  }

  @Override
  public boolean remove(int index) {
    checkInDomain(index);
    return super.remove(index);
  }
}
