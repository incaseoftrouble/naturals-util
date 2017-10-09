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
import java.util.Collection;
import java.util.function.Predicate;

public abstract class AbstractUnmodifiableNatBitSet extends AbstractIntSet implements NatBitSet {
  @Override
  public boolean add(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(IntCollection c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends Integer> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void and(IntCollection indices) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void andNot(IntCollection indices) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear(int from, int to) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearFrom(int from) {
    throw new UnsupportedOperationException();
  }

  @Override
  public AbstractUnmodifiableNatBitSet clone() {
    try {
      return (AbstractUnmodifiableNatBitSet) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError(e);
    }
  }

  @Override
  public boolean containsAll(IntCollection c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void flip(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void flip(int from, int to) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void or(IntCollection indices) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(IntCollection c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeIf(Predicate<? super Integer> filter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(IntCollection c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(int index, boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(int from, int to) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void set(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void xor(IntCollection indices) {
    throw new UnsupportedOperationException();
  }
}
