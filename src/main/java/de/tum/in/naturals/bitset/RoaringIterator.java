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

package de.tum.in.naturals.bitset;

import it.unimi.dsi.fastutil.ints.IntIterator;
import org.roaringbitmap.PeekableIntIterator;

final class RoaringIterator implements IntIterator {
  private final PeekableIntIterator iterator;

  public RoaringIterator(PeekableIntIterator iterator) {
    this.iterator = iterator;
  }

  @Override
  public int nextInt() {
    return iterator.next();
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }
}
