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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;
import javax.annotation.Nonnegative;

class BoundedWrapper extends AbstractBoundedNatBitSet {
  private final boolean complement;
  private final BoundedWrapper complementView;
  private final NatBitSet delegate;

  private BoundedWrapper(BoundedWrapper other) {
    super(other.domainSize());
    // Complement constructor
    this.delegate = other.delegate;
    this.complement = !other.complement;
    this.complementView = other;
    assert checkConsistency();
  }

  private BoundedWrapper(NatBitSet delegate, @Nonnegative int domainSize, boolean complement) {
    super(domainSize);
    assert !(delegate instanceof BoundedNatBitSet);
    this.delegate = delegate;
    this.complement = complement;
    this.complementView = new BoundedWrapper(this);
    assert checkConsistency();
  }

  BoundedWrapper(NatBitSet delegate, @Nonnegative int domainSize) {
    this(delegate, domainSize, false);
  }

  @Override
  public boolean add(int index) {
    checkInDomain(index);
    return complement ? delegate.remove(index) : delegate.add(index);
  }

  @Override
  public void and(IntCollection indices) {
    if (complement) {
      super.and(indices);
    } else {
      delegate.and(indices);
    }
  }

  @Override
  public void andNot(IntCollection indices) {
    if (complement) {
      super.andNot(indices);
    } else {
      delegate.andNot(indices);
    }
  }

  private boolean checkConsistency() {
    return delegate.isEmpty() || delegate.lastInt() <= domainSize();
  }

  @Override
  public void clear(int index) {
    assert checkConsistency();
    checkInDomain(index);
    if (complement) {
      delegate.set(index);
    } else {
      delegate.clear(index);
    }
    assert checkConsistency();
  }

  @Override
  public void clear(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    if (complement) {
      delegate.set(from, to);
    } else {
      delegate.clear(from, to);
    }
    assert checkConsistency();
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public BoundedWrapper clone() {
    return new BoundedWrapper(delegate, domainSize());
  }

  @Override
  public BoundedNatBitSet complement() {
    return complementView;
  }

  @Override
  public boolean contains(int index) {
    return inDomain(index) && (complement ^ delegate.contains(index));
  }

  @Override
  public boolean containsAll(IntCollection indices) {
    if (complement) {
      return super.containsAll(indices);
    }
    return delegate.containsAll(indices);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof BoundedWrapper) {
      BoundedWrapper other = (BoundedWrapper) o;
      return delegate.equals(other.delegate);
    }
    return delegate.equals(o);
  }

  @Override
  @Nonnegative
  @SuppressFBWarnings(value = "TQ_NEVER_VALUE_USED_WHERE_ALWAYS_REQUIRED",
                      justification = "Findbugs doesn't infer @Nonnull from control flow")
  public int firstInt() {
    if (complement) {
      int nextPresent = nextPresentIndex(0);
      if (nextPresent < 0) {
        throw new NoSuchElementException();
      }
      return nextPresent;
    }
    return delegate.firstInt();
  }

  @Override
  public void flip(int index) {
    assert checkConsistency();
    checkInDomain(index);
    delegate.flip(index);
    assert checkConsistency();
  }

  @Override
  public void flip(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    delegate.flip(from, to);
    assert checkConsistency();
  }

  @Override
  public int hashCode() {
    return HashCommon.mix(delegate.hashCode());
  }

  @Override
  public boolean intersects(IntCollection indices) {
    if (complement) {
      return super.intersects(indices);
    }
    return delegate.intersects(indices);
  }

  @Override
  boolean isComplement() {
    return complement;
  }

  @Override
  public boolean isEmpty() {
    assert checkConsistency();
    return delegate.size() == (complement ? domainSize() : 0);
  }

  @Override
  public IntIterator iterator() {
    assert checkConsistency();
    return complement
        ? NatBitSets.complementIterator(delegate, domainSize())
        : delegate.iterator();
  }

  @Override
  public int lastInt() {
    assert checkConsistency();
    if (complement) {
      if (isEmpty()) {
        throw new NoSuchElementException();
      }
      return NatBitSets.previousPresentIndex(delegate, domainSize() - 1);
    }
    return delegate.lastInt();
  }

  @Override
  public int nextAbsentIndex(int index) {
    assert checkConsistency();
    if (complement) {
      int nextSet = delegate.nextPresentIndex(index);
      return nextSet == -1 ? index : nextSet;
    }
    return delegate.nextAbsentIndex(index);
  }

  @Override
  public int nextPresentIndex(int index) {
    assert checkConsistency();
    if (complement) {
      int nextClear = delegate.nextAbsentIndex(index);
      return nextClear == domainSize() ? -1 : nextClear;
    }
    return delegate.nextPresentIndex(index);
  }

  @Override
  public void or(IntCollection indices) {
    if (complement) {
      super.or(indices);
    } else {
      delegate.or(indices);
    }
  }

  @Override
  public void orNot(IntCollection indices) {
    if (indices instanceof BoundedNatBitSet) {
      delegate.or(((BoundedNatBitSet) indices).complement());
    } else {
      super.orNot(indices);
    }
  }

  @Override
  public boolean remove(int index) {
    checkInDomain(index);
    return complement ? delegate.add(index) : delegate.remove(index);
  }

  @Override
  public boolean removeAll(IntCollection indices) {
    return complement ? delegate.addAll(indices) : delegate.removeAll(indices);
  }

  @Override
  public boolean retainAll(IntCollection indices) {
    if (complement) {
      return super.retainAll(indices);
    }
    return delegate.retainAll(indices);
  }

  @Override
  public void set(int index) {
    assert checkConsistency();
    checkInDomain(index);
    if (complement) {
      delegate.clear(index);
    } else {
      delegate.set(index);
    }
    assert checkConsistency();
  }

  @Override
  public void set(int index, boolean value) {
    assert checkConsistency();
    checkInDomain(index);
    delegate.set(index, value ^ complement);
    assert checkConsistency();
  }

  @Override
  public void set(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    if (complement) {
      delegate.clear(from, to);
    } else {
      delegate.set(from, to);
    }
    assert checkConsistency();
  }

  @Override
  public int size() {
    assert checkConsistency();
    return complement ? domainSize() - delegate.size() : delegate.size();
  }
}
