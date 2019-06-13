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
import javax.annotation.Nonnegative;
import org.roaringbitmap.RoaringBitmap;

class RoaringBoundedNatBitSet extends AbstractBoundedNatBitSet {
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
    if (size() < indices.size()) {
      return false;
    }

    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;

      if (complement) {
        if (other.complement) {
          if (other.domainSize() >= domainSize()) {
            return other.bitmap.contains(bitmap);
          }
          return RoaringBitmap.andCardinality(bitmap, other.bitmap)
              == bitmap.rank(other.domainSize() - 1);
        }
        return other.lastInt() <= domainSize()
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
        return other.lastInt() <= domainSize()
            && !RoaringBitmap.intersects(bitmap, other.getBitmap());
      }
      return bitmap.contains(other.getBitmap());
    }
    return super.containsAll(indices);
  }


  @Override
  public int lastInt() {
    int lastInt;
    if (complement) {
      lastInt = Math.toIntExact(bitmap.previousAbsentValue(domainSize() - 1));
    } else {
      lastInt = bitmap.isEmpty() ? -1 : bitmap.last();
    }
    if (lastInt == -1) {
      throw new NoSuchElementException();
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

      return RoaringBitmap.intersects(
          complement ? complementBits() : bitmap,
          other.isComplement() ? other.complementBits() : other.getBitmap());
    }
    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;

      if (complement) {
        return other.getBitmap().getCardinality()
            > RoaringBitmap.andCardinality(other.getBitmap(), bitmap);
      }
      return RoaringBitmap.intersects(bitmap, other.getBitmap());
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
      if (complement) {
        bitmap.or(other.complement ? other.bitmap : other.complementBits());

        int domainSize = domainSize();
        int otherDomainSize = other.domainSize();
        if (domainSize < otherDomainSize) {
          bitmap.remove((long) domainSize, (long) otherDomainSize);
        } else {
          bitmap.add((long) otherDomainSize, (long) domainSize);
        }
      } else {
        if (other.complement) {
          bitmap.andNot(other.bitmap);

          int domainSize = domainSize();
          int otherDomainSize = other.domainSize();
          if (otherDomainSize < domainSize) {
            bitmap.remove((long) otherDomainSize, (long) domainSize);
          }
        } else {
          bitmap.and(other.bitmap);
        }
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;

      if (complement) {
        // TODO More efficient?
        bitmap.flip(0L, (long) domainSize());
        bitmap.and(other.getBitmap());
        bitmap.flip(0L, (long) domainSize());
      } else {
        bitmap.and(other.getBitmap());
      }
    } else {
      if (complement) {
        bitmap.flip(0L, (long) domainSize());
      }
      RoaringBitmap toRemove = new RoaringBitmap();

      bitmap.forEach((int index) -> {
        if (!indices.contains(index)) {
          toRemove.add(index);
        }
      });

      bitmap.andNot(toRemove);
      if (complement) {
        bitmap.flip(0L, (long) domainSize());
      }
    }
    assert checkConsistency();
  }

  @Override
  public void andNot(IntCollection indices) {
    assert checkConsistency();
    if (isEmpty() || indices.isEmpty()) {
      return;
    }
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;

      if (complement) {
        bitmap.or(other.complement ? other.complementBits() : other.bitmap);

        int domainSize = domainSize();
        int otherDomainSize = other.domainSize();
        if (domainSize < otherDomainSize) {
          bitmap.remove((long) domainSize, (long) otherDomainSize);
        }
      } else {
        if (other.complement) {
          int domainSize = domainSize();
          int otherDomainSize = other.domainSize();
          if (otherDomainSize < domainSize) {
            RoaringBitmap clone = other.bitmap.clone();
            clone.add((long) otherDomainSize, (long) domainSize);
            bitmap.and(clone);
          } else {
            bitmap.and(other.bitmap);
          }
        } else {
          bitmap.andNot(other.bitmap);
        }
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;

      if (complement) {
        bitmap.or(other.getBitmap());
        bitmap.remove((long) domainSize(), (long) Integer.MAX_VALUE);
      } else {
        bitmap.andNot(other.getBitmap());
      }
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

  @Override
  public void or(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      checkInDomain(other.lastInt());

      if (complement) {
        if (other.complement) {
          int otherDomainSize = other.domainSize();
          int domainSize = domainSize();

          RoaringBitmap otherBitmap;
          if (otherDomainSize <= domainSize) {
            otherBitmap = other.bitmap.clone();
            otherBitmap.add((long) otherDomainSize, (long) domainSize);
          } else {
            otherBitmap = other.bitmap;
          }

          bitmap.and(otherBitmap);
        } else {
          bitmap.andNot(other.bitmap);
        }
      } else {
        if (other.complement) {
          this.bitmap.or(other.complementBits());
        } else {
          this.bitmap.or(other.bitmap);
        }
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      checkInDomain(other.lastInt());

      if (complement) {
        bitmap.andNot(other.getBitmap());
      } else {
        bitmap.or(other.getBitmap());
      }
    } else {
      super.or(indices);
    }
    assert checkConsistency();
  }

  @Override
  public void orNot(IntCollection indices) {
    assert checkConsistency();
    if (indices.isEmpty()) {
      if (complement) {
        bitmap.clear();
      } else {
        bitmap.add(0L, (long) domainSize());
      }
    } else if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      int domainSize = domainSize();
      int otherDomainSize = other.domainSize();

      if (complement) {
        if (other.complement) {
          bitmap.andNot(other.bitmap);
          if (otherDomainSize < domainSize) {
            bitmap.remove((long) otherDomainSize, (long) domainSize);
          }
        } else {
          bitmap.and(other.bitmap);
        }
      } else {
        if (other.complement) {
          bitmap.or(other.bitmap);
          if (otherDomainSize < domainSize) {
            bitmap.add((long) otherDomainSize, (long) domainSize);
          } else {
            bitmap.remove((long) domainSize, (long) otherDomainSize);
          }
        } else {
          int minDomainSize = Math.min(domainSize, otherDomainSize);
          other.bitmap.flip(0L, (long) minDomainSize);
          bitmap.or(other.bitmap);
          other.bitmap.flip(0L, (long) minDomainSize);

          if (otherDomainSize < domainSize) {
            bitmap.add((long) otherDomainSize, (long) domainSize);
          } else {
            bitmap.remove((long) domainSize, (long) otherDomainSize);
          }
        }
      }
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;

      if (complement) {
        bitmap.and(other.getBitmap());
      } else {
        RoaringBitmap bitmap = other.getBitmap().clone();
        bitmap.remove((long) domainSize(), Long.MAX_VALUE);
        bitmap.flip(0L, (long) domainSize());
        this.bitmap.or(bitmap);
      }
    } else {
      super.orNot(indices);
    }
    assert checkConsistency();
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

      bitmap.xor(other.getBitmap());
    } else {
      super.xor(indices);
    }
    assert checkConsistency();
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
          RoaringBitmap largerBitSet = larger.bitmap.clone();
          largerBitSet.remove((long) smallerSize, (long) largerSize);
          return smaller.bitmap.equals(largerBitSet);
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
        return size() == other.size() && !RoaringBitmap.intersects(bitmap, other.getBitmap());
      }
      return bitmap.equals(other.getBitmap());
    }
    return super.equals(o);
  }


  RoaringBitmap getBitmap() {
    return bitmap;
  }

  RoaringBitmap complementBits() {
    return RoaringBitmap.flip(bitmap, 0L, (long) domainSize());
  }


  private boolean checkConsistency() {
    return bitmap.isEmpty() || bitmap.last() < domainSize();
  }
}
