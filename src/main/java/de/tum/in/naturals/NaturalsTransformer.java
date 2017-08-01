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

package de.tum.in.naturals;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nonnegative;

/**
 * Transforms a given stream of natural numbers by a specified function, ignoring negative results.
 * Semantically, this transformation is equivalent to
 * {@code intStream.map(transformer).filter(i -> i >= 0)}.
 */
public final class NaturalsTransformer implements IntIterator {
  private final PrimitiveIterator.OfInt original;
  private final IntUnaryOperator transformer;
  // A negative value indicates that there are no more elements.
  private int next;

  public NaturalsTransformer(PrimitiveIterator.OfInt original, IntUnaryOperator transformer) {
    this.original = original;
    this.transformer = transformer;
    getNext();
  }

  private int getNext() {
    while (original.hasNext()) {
      int nextCandidate = transformer.applyAsInt(original.nextInt());

      // If the transformer returns a negative value, the element is skipped.
      if (nextCandidate >= 0) {
        return nextCandidate;
      }
    }

    return -1;
  }

  @Override
  public boolean hasNext() {
    return next >= 0;
  }

  @Override
  @Nonnegative
  public int nextInt() {
    if (next < 0) {
      throw new NoSuchElementException();
    }

    int value = next;
    next = getNext();
    return value;
  }
}
