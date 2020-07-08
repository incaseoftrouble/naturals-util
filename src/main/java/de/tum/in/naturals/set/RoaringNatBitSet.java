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

import de.tum.in.naturals.bitset.RoaringBitmaps;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collection;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import org.roaringbitmap.RoaringBitmap;

class RoaringNatBitSet extends AbstractNatBitSet {
  private static final long INFINITY = Integer.MAX_VALUE + 1L;

  private final RoaringBitmap bitmap;

  RoaringNatBitSet(RoaringBitmap bitmap) {
    this.bitmap = bitmap;
  }


  @Override
  public boolean isEmpty() {
    return bitmap.isEmpty();
  }

  @Override
  public int size() {
    return bitmap.getCardinality();
  }

  @Override
  public boolean contains(int index) {
    return 0 <= index && bitmap.contains(index);
  }

  @Override
  public boolean containsAll(IntCollection indices) {
    if (isEmpty()) {
      return indices.isEmpty();
    }
    if (indices.isEmpty()) {
      return true;
    }

    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      return bitmap.contains(other.bitmap);
    }
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;

      if (other.isComplement()) {
        return bitmap.rank(other.domainSize() - 1) + other.bitmap().getCardinality()
            == other.domainSize() + RoaringBitmap.andCardinality(bitmap, other.bitmap());
      }
      return bitmap.contains(other.bitmap());
    }

    return super.containsAll(indices);
  }


  @Override
  public int firstInt() {
    return bitmap.first();
  }

  @Override
  public int lastInt() {
    return bitmap.last();
  }


  @Override
  public int nextPresentIndex(int index) {
    return Math.toIntExact(bitmap.nextValue(index));
  }

  @Override
  public int nextAbsentIndex(int index) {
    return Math.toIntExact(bitmap.nextAbsentValue(index));
  }

  @Override
  public int previousPresentIndex(int index) {
    return bitmap.isEmpty() ? -1 : Math.toIntExact(bitmap.previousValue(index));
  }

  @Override
  public int previousAbsentIndex(int index) {
    return Math.toIntExact(bitmap.previousAbsentValue(index));
  }


  @Override
  public IntIterator iterator() {
    return RoaringBitmaps.iterator(bitmap);
  }

  @Override
  public void forEach(IntConsumer consumer) {
    bitmap.forEach((org.roaringbitmap.IntConsumer) consumer::accept);
  }


  @Override
  public void set(int index) {
    bitmap.add(index);
  }

  @Override
  public void set(int index, boolean value) {
    if (value) {
      bitmap.add(index);
    } else {
      bitmap.remove(index);
    }
  }

  @Override
  public void set(int from, int to) {
    bitmap.add((long) from, (long) to);
  }

  @Override
  public void clear() {
    bitmap.clear();
  }

  @Override
  public void clear(int index) {
    bitmap.remove(index);
  }

  @Override
  public void clear(int from, int to) {
    bitmap.remove((long) from, (long) to);
  }

  @Override
  public void clearFrom(int from) {
    bitmap.remove((long) from, INFINITY);
  }

  @Override
  public void flip(int index) {
    bitmap.flip(index);
  }

  @Override
  public void flip(int from, int to) {
    bitmap.flip((long) from, (long) to);
  }


  @Override
  public boolean intersects(Collection<Integer> indices) {
    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      return RoaringBitmap.intersects(bitmap, other.bitmap);
    }
    if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      return other.isComplement()
          ? RoaringBitmap.intersects(bitmap, other.complementBits())
          : RoaringBitmap.intersects(bitmap, other.bitmap());
    }
    return super.intersects(indices);
  }

  @Override
  public void and(IntCollection indices) {
    if (indices.isEmpty()) {
      clear();
    } else if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      bitmap.and(other.bitmap);
    } else if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      bitmap.remove((long) other.domainSize(), INFINITY);
      if (other.isComplement()) {
        bitmap.andNot(other.bitmap());
      } else {
        bitmap.and(other.bitmap());
      }
    } else if (indices instanceof MutableSingletonNatBitSet) {
      MutableSingletonNatBitSet singleton = (MutableSingletonNatBitSet) indices;
      int index = singleton.firstInt();
      assert index == singleton.lastInt();
      boolean contains = contains(index);
      clear();
      if (contains) {
        set(index);
      }
    } else {
      bitmap.and(RoaringBitmaps.of(indices));
    }
  }

  @Override
  public void andNot(IntCollection indices) {
    if (isEmpty() || indices.isEmpty()) {
      return;
    }
    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      bitmap.andNot(other.bitmap);
    } else if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;

      int otherDomainSize = other.domainSize();
      if (other.isComplement()) {
        RoaringBitmap tail = RoaringBitmaps.subset(bitmap, otherDomainSize, INFINITY);
        bitmap.and(other.bitmap());
        bitmap.or(tail);
      } else {
        bitmap.andNot(other.bitmap());
      }
    } else {
      bitmap.andNot(RoaringBitmaps.of(indices));
    }
  }

  @Override
  public void or(IntCollection indices) {
    if (indices.isEmpty()) {
      return;
    }
    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      bitmap.or(other.bitmap);
    } else if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;
      if (other.isComplement()) {
        // TODO orNot bug
        RoaringBitmap tail = RoaringBitmaps.subset(bitmap, other.domainSize(), INFINITY);
        bitmap.orNot(other.bitmap(), other.domainSize());
        bitmap.or(tail);
      } else {
        bitmap.or(other.bitmap());
      }
    } else {
      bitmap.or(RoaringBitmaps.of(indices));
    }
  }

  @Override
  public void xor(IntCollection indices) {
    if (indices instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) indices;
      bitmap.xor(other.bitmap);
    } else if (indices instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) indices;

      bitmap.xor(other.bitmap());
      if (other.isComplement()) {
        bitmap.flip(0L, (long) other.domainSize());
      }
    } else {
      bitmap.xor(RoaringBitmaps.of(indices));
    }
  }


  @Override
  public boolean removeIf(IntPredicate filter) {
    RoaringBitmap remove = new RoaringBitmap();
    bitmap.forEach((int i) -> {
      if (filter.test(i)) {
        remove.add(i);
      }
    });
    bitmap.andNot(remove);
    return !remove.isEmpty();
  }


  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public RoaringNatBitSet clone() {
    return new RoaringNatBitSet(bitmap.clone());
  }

  @Override
  public boolean equals(Object o) {
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

    if (o instanceof RoaringNatBitSet) {
      RoaringNatBitSet other = (RoaringNatBitSet) o;
      return bitmap.equals(other.bitmap);
    }
    if (o instanceof RoaringBoundedNatBitSet) {
      RoaringBoundedNatBitSet other = (RoaringBoundedNatBitSet) o;

      if (lastInt() >= other.domainSize()) {
        return false;
      }
      if (other.isComplement()) {
        return size() == other.size() && !RoaringBitmap.intersects(bitmap, other.bitmap());
      }
      return bitmap.equals(other.bitmap());
    }
    return super.equals(o);
  }

  RoaringBitmap bitmap() {
    return bitmap;
  }
}
