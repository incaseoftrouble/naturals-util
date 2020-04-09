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

import static de.tum.in.naturals.set.NatBitSetsUtil.checkNonNegative;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkRange;

import de.tum.in.naturals.bitset.RoaringBitmaps;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntPredicate;
import javax.annotation.Nonnegative;
import org.roaringbitmap.RoaringBitmap;

class RoaringBoundedNatBitSet extends AbstractBoundedNatBitSet {
  private static final long INFINITY = Integer.MAX_VALUE + 1L;

  private final RoaringBitmap bitmap;
  private final boolean complement;
  private final RoaringBoundedNatBitSet complementView;

  private RoaringBoundedNatBitSet(RoaringBoundedNatBitSet other) {
    super(other.domainSize());
    // Complement constructor
    this.bitmap = other.bitmap;
    this.complement = !other.complement;
    this.complementView = other;
    assert checkConsistency();
  }

  private RoaringBoundedNatBitSet(RoaringBitmap bitmap, @Nonnegative int domainSize,
      boolean complement) {
    super(domainSize);
    this.bitmap = bitmap;
    this.complement = complement;
    this.complementView = new RoaringBoundedNatBitSet(this);
    assert checkConsistency();
  }

  RoaringBoundedNatBitSet(RoaringBitmap bitmap, @Nonnegative int domainSize) {
    this(bitmap, domainSize, false);
  }


  @Override
  boolean isComplement() {
    return complement;
  }

  @Override
  public boolean isEmpty() {
    assert checkConsistency();
    if (complement) {
      return bitmap.getCardinality() == domainSize();
    }
    return bitmap.isEmpty();
  }

  @Override
  public int size() {
    assert checkConsistency();
    int bitSetCardinality = bitmap.getCardinality();
    return complement ? domainSize() - bitSetCardinality : bitSetCardinality;
  }

  @Override
  public boolean contains(int k) {
    return 0 <= k && (complement ? k < domainSize() && !bitmap.contains(k) : bitmap.contains(k));
  }

  @Override
  public boolean containsAll(IntCollection indices) {
    assert checkConsistency();
    if (isEmpty()) {
      return indices.isEmpty();
    }
    if (indices.isEmpty()) {
      return true;
    }

    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;

      if (complement) {
        if (other.complement) {
          if (other.domainSize() >= domainSize()) {
            return other.bitmap.nextAbsentValue(domainSize()) == other.domainSize()
                && other.bitmap.contains(bitmap);
          }
          return RoaringBitmap.andCardinality(bitmap, other.bitmap)
              == bitmap.rank(other.domainSize() - 1);
        }
        return other.lastInt() < domainSize()
            && !RoaringBitmap.intersects(bitmap, other.bitmap);
      }

      if (other.complement) {
        if (other.domainSize() >= domainSize()) {
          return bitmap.getCardinality() + other.bitmap.getCardinality()
              == other.domainSize() + RoaringBitmap.andCardinality(bitmap, other.bitmap);
        }
        return bitmap.rank(other.domainSize() - 1) + other.bitmap.getCardinality()
            == other.domainSize() + RoaringBitmap.andCardinality(bitmap, other.bitmap);
      }
      return bitmap.contains(other.bitmap);
    }
    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;

      if (complement) {
        return other.lastInt() < domainSize()
            && !RoaringBitmap.intersects(bitmap, other.bitmap());
      }
      return bitmap.contains(other.bitmap());
    }

    return super.containsAll(indices);
  }


  @Override
  public int firstInt() {
    assert checkConsistency();
    if (complement) {
      int firstInt = Math.toIntExact(bitmap.nextAbsentValue(0));
      if (firstInt >= domainSize()) {
        throw new NoSuchElementException();
      }
      return firstInt;
    }
    return bitmap.first();
  }

  @Override
  public int lastInt() {
    assert checkConsistency();
    int lastInt;
    if (complement) {
      lastInt = Math.toIntExact(bitmap.previousAbsentValue(domainSize() - 1));
      if (lastInt == -1) {
        throw new NoSuchElementException();
      }
    } else {
      lastInt = bitmap.last();
    }
    assert 0 <= lastInt && lastInt < domainSize();
    return lastInt;
  }


  @Override
  public int nextPresentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
    if (index >= domainSize()) {
      return -1;
    }
    if (complement) {
      int nextClear = Math.toIntExact(bitmap.nextAbsentValue(index));
      return nextClear >= domainSize() ? -1 : nextClear;
    }
    return Math.toIntExact(bitmap.nextValue(index));
  }

  @Override
  public int nextAbsentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
    if (index >= domainSize()) {
      return index;
    }
    if (complement) {
      int nextSet = Math.toIntExact(bitmap.nextValue(index));
      return nextSet == -1 ? domainSize() : nextSet;
    }
    return Math.toIntExact(bitmap.nextAbsentValue(index));
  }

  @Override
  public int previousPresentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
    int clampedIndex = Math.min(index, domainSize() - 1);
    return Math.toIntExact(complement
        ? bitmap.previousAbsentValue(clampedIndex)
        : bitmap.previousValue(clampedIndex));
  }

  @Override
  public int previousAbsentIndex(int index) {
    assert checkConsistency();
    checkNonNegative(index);
    if (index >= domainSize()) {
      return index;
    }
    return Math.toIntExact(complement
        ? bitmap.previousValue(index)
        : bitmap.previousAbsentValue(index));
  }


  @Override
  public IntIterator iterator() {
    assert checkConsistency();
    return RoaringBitmaps.iterator(complement ? complementBits() : this.bitmap);
  }


  @Override
  public void set(int index) {
    assert checkConsistency();
    checkInDomain(index);
    if (complement) {
      bitmap.remove(index);
    } else {
      bitmap.add(index);
    }
    assert checkConsistency();
  }

  @Override
  public void set(int index, boolean value) {
    assert checkConsistency();
    checkInDomain(index);
    if (value == complement) {
      bitmap.remove(index);
    } else {
      bitmap.add(index);
    }
    assert checkConsistency();
  }

  @Override
  public void set(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    if (complement) {
      bitmap.remove((long) from, (long) to);
    } else {
      bitmap.add((long) from, (long) to);
    }
    assert checkConsistency();
  }

  @Override
  public void clear() {
    assert checkConsistency();
    if (complement) {
      bitmap.add(0L, (long) domainSize());
    } else {
      bitmap.clear();
    }
    assert checkConsistency();
  }

  @Override
  public void clear(int index) {
    assert checkConsistency();
    if (index >= domainSize()) {
      return;
    }
    if (complement) {
      bitmap.add(index);
    } else {
      bitmap.remove(index);
    }
    assert checkConsistency();
  }

  @Override
  public void clear(int from, int to) {
    assert checkConsistency();
    checkRange(from, to);
    int domainSize = domainSize();
    if (from >= domainSize) {
      return;
    }

    if (complement) {
      bitmap.add((long) from, (long) Math.min(to, domainSize));
    } else {
      bitmap.remove((long) from, (long) Math.min(to, domainSize));
    }
    assert checkConsistency();
  }

  @Override
  public void flip(int index) {
    assert checkConsistency();
    checkInDomain(index);
    bitmap.flip(index);
    assert checkConsistency();
  }

  @Override
  public void flip(int from, int to) {
    assert checkConsistency();
    checkInDomain(from, to);
    bitmap.flip((long) from, (long) to);
    assert checkConsistency();
  }


  @Override
  public boolean intersects(Collection<Integer> indices) {
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;

      // TODO This is crappy slow
      return RoaringBitmap.intersects(
          complement ? complementBits() : bitmap,
          other.isComplement() ? other.complementBits() : other.bitmap());
    }
    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;

      if (complement) {
        return other.bitmap().getCardinality()
            > RoaringBitmap.andCardinality(other.bitmap(), bitmap);
      }
      return RoaringBitmap.intersects(bitmap, other.bitmap());
    }
    return super.intersects(indices);
  }

  @Override
  public void and(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      clear();
    } else if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      if (other.complement) {
        doAndComplement(other.bitmap, other.domainSize());
      } else {
        doAnd(other.bitmap);
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      doAnd(other.bitmap());
    } else {
      doAnd(RoaringBitmaps.of(indices));
    }
    assert checkConsistency();
  }

  private void doAnd(RoaringBitmap other) {
    int domainSize = domainSize();
    if (complement) {
      bitmap.orNot(other, domainSize);
      bitmap.remove(domainSize, INFINITY);
    } else {
      bitmap.and(other);
    }
  }

  private void doAndComplement(RoaringBitmap other, int otherDomainSize) {
    int domainSize = domainSize();

    if (complement) {
      bitmap.or(other);
      bitmap.add((long) otherDomainSize, (long) domainSize);
      bitmap.remove((long) domainSize, (long) otherDomainSize);
    } else {
      bitmap.andNot(other);
      bitmap.remove((long) otherDomainSize, (long) domainSize);
    }
  }


  @Override
  public void andNot(IntCollection indices) {
    assert checkConsistency();
    if (isEmpty() || indices.isEmpty()) {
      return;
    }
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      if (other.complement) {
        doAndNotComplement(other.bitmap, other.domainSize());
      } else {
        doAndNot(other.bitmap);
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      doAndNot(other.bitmap());
    } else {
      if (complement) {
        indices.forEach((int index) -> {
          if (index < domainSize()) {
            bitmap.add(index);
          }
        });
      } else {
        RoaringBitmap toRemove = new RoaringBitmap();

        bitmap.forEach((int index) -> {
          if (indices.contains(index)) {
            toRemove.add(index);
          }
        });

        bitmap.andNot(toRemove);
      }
    }
    assert checkConsistency();
  }

  private void doAndNot(RoaringBitmap other) {
    if (complement) {
      bitmap.or(other);
      bitmap.remove((long) domainSize(), INFINITY);
    } else {
      bitmap.andNot(other);
    }
  }

  private void doAndNotComplement(RoaringBitmap other, int otherDomainSize) {
    int domainSize = domainSize();
    if (complement) {
      if (otherDomainSize < domainSize) { // TODO orNot bug
        RoaringBitmap tail = RoaringBitmaps.subset(bitmap, otherDomainSize, domainSize);
        bitmap.orNot(other, otherDomainSize);
        bitmap.or(tail);
      } else {
        bitmap.orNot(other, domainSize);
        bitmap.remove((long) domainSize, INFINITY); // TODO orNot bug
      }
    } else {
      if (otherDomainSize < domainSize) {
        RoaringBitmap tail = RoaringBitmaps.subset(bitmap, otherDomainSize, domainSize);
        bitmap.and(other);
        bitmap.or(tail);
      } else {
        bitmap.and(other);
      }
    }
  }


  @Override
  public void or(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      checkInDomain(other.lastInt());
      if (other.complement) {
        doOrComplement(other.bitmap, other.domainSize());
      } else {
        doOr(other.bitmap);
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      checkInDomain(other.lastInt());
      doOr(other.bitmap());
    } else {
      doOr(RoaringBitmaps.of(indices));
    }
    assert checkConsistency();
  }

  private void doOr(RoaringBitmap other) {
    if (complement) {
      bitmap.andNot(other);
    } else {
      bitmap.or(other);
    }
  }

  private void doOrComplement(RoaringBitmap other, int otherDomainSize) {
    if (complement) {
      int domainSize = domainSize();

      if (otherDomainSize < domainSize) {
        RoaringBitmap tail = RoaringBitmaps.subset(bitmap, otherDomainSize, domainSize);
        bitmap.and(other);
        bitmap.or(tail);
      } else {
        bitmap.and(other);
      }
    } else {
      // TODO orNot bugs
      RoaringBitmap tail = RoaringBitmaps.subset(bitmap, otherDomainSize, domainSize());
      bitmap.orNot(other, otherDomainSize);
      bitmap.or(tail);
    }
  }


  @Override
  public void orNot(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      set(0, domainSize());
    } else if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      if (other.complement) {
        doOrNotComplement(other.bitmap, other.domainSize());
      } else {
        doOrNot(other.bitmap);
      }
    } else if (indices instanceof RoaringNatBitSet) {
      doOrNot(((RoaringNatBitSet) indices).bitmap());
    } else {
      doOrNot(RoaringBitmaps.of(indices));
    }
    assert checkConsistency();
  }

  private void doOrNot(RoaringBitmap other) {
    if (complement) {
      bitmap.and(other);
    } else {
      int domainSize = domainSize();
      bitmap.orNot(other, domainSize);
      bitmap.remove(domainSize, INFINITY);
    }
  }

  private void doOrNotComplement(RoaringBitmap other, int otherDomainSize) {
    int domainSize = domainSize();
    if (complement) {
      bitmap.andNot(other);
      if (otherDomainSize < domainSize) {
        bitmap.remove((long) otherDomainSize, (long) domainSize);
      }
    } else {
      bitmap.or(other);
      if (otherDomainSize < domainSize) {
        bitmap.add((long) otherDomainSize, (long) domainSize);
      } else {
        bitmap.remove((long) domainSize, (long) otherDomainSize);
      }
    }
  }

  @Override
  public void xor(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      checkInDomain(other.lastInt());

      bitmap.xor(other.bitmap);
      if (other.complement) {
        bitmap.flip(0L, (long) other.domainSize());
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      checkInDomain(other.lastInt());

      bitmap.xor(other.bitmap());
    } else {
      bitmap.xor(RoaringBitmaps.of(indices));
    }
    assert checkConsistency();
  }


  @Override
  public boolean removeIf(IntPredicate filter) {
    RoaringBitmap remove = new RoaringBitmap();
    (complement ? complementBits() : bitmap).forEach((int i) -> {
      if (filter.test(i)) {
        remove.add(i);
      }
    });
    if (complement) {
      bitmap.or(remove);
    } else {
      bitmap.andNot(remove);
    }
    return !remove.isEmpty();
  }


  @Override
  public RoaringBoundedNatBitSet clone() {
    assert checkConsistency();
    return new RoaringBoundedNatBitSet(bitmap.clone(), domainSize(), complement);
  }

  @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
  @Override
  public RoaringBoundedNatBitSet complement() {
    return complementView;
  }


  @Override
  public boolean equals(Object o) {
    assert checkConsistency();
    if (this == o) {
      return true;
    }
    if (!(o instanceof Set)) {
      return false;
    }
    if (isEmpty()) {
      return ((Collection<?>) o).isEmpty();
    }
    if (((Collection<?>) o).isEmpty()) {
      return false;
    }

    if (o instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) o;

      int domainSize = domainSize();
      int otherDomainSize = other.domainSize();

      if (complement) {
        if (other.complement) {
          if (domainSize == otherDomainSize) {
            return bitmap.equals(other.bitmap);
          }
          RoaringBoundedNatBitSet smaller;
          RoaringBoundedNatBitSet larger;
          int smallerSize;
          int largerSize;
          if (domainSize < otherDomainSize) {
            smaller = this;
            larger = other;
            smallerSize = domainSize;
            largerSize = otherDomainSize;
          } else {
            smaller = other;
            larger = this;
            smallerSize = otherDomainSize;
            largerSize = domainSize;
          }

          if (larger.bitmap.nextAbsentValue(smallerSize) < (long) largerSize) {
            return false;
          }

          return smaller.bitmap.equals(RoaringBitmaps.subset(larger.bitmap, 0, smallerSize));
        }
      } else if (!other.complement) {
        return bitmap.equals(other.bitmap);
      }

      // complement != otherComplement
      int complementDomainSize = complement ? domainSize : otherDomainSize;
      RoaringBoundedNatBitSet nonComplementSet = complement ? other : this;
      assert !nonComplementSet.complement;

      return !RoaringBitmap.intersects(bitmap, other.bitmap)
          && bitmap.getCardinality() + other.bitmap.getCardinality() == complementDomainSize
          && (domainSize == otherDomainSize || nonComplementSet.lastInt() < complementDomainSize);
    }
    if (o instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) o;

      if (other.lastInt() >= domainSize()) {
        return false;
      }
      if (isComplement()) {
        return size() == other.size() && !RoaringBitmap.intersects(bitmap, other.bitmap());
      }
      return bitmap.equals(other.bitmap());
    }
    return super.equals(o);
  }


  RoaringBitmap bitmap() {
    return bitmap;
  }

  RoaringBitmap complementBits() {
    return RoaringBitmap.flip(bitmap, 0L, (long) domainSize());
  }


  private boolean checkConsistency() {
    assert bitmap.isEmpty() || bitmap.last() < domainSize() : bitmap.last() + " " + domainSize();
    return true;
  }
}
