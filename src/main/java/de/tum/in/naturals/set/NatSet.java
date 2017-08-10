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

import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnegative;

/**
 * A set of non-negative integers.
 */
public interface NatSet extends IntSet {
  /**
   * {@inheritDoc}
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   */
  @Override
  boolean add(@Nonnegative int index);

  /**
   * Returns an int stream compatible with the {@link #spliterator() spliterator}.
   */
  IntStream intStream();

  /**
   * {@inheritDoc}
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   */
  @Override
  boolean remove(@Nonnegative int index);

  /**
   * Returns a spliterator over this set. The spliterator is expected to be
   * {@link Spliterator#SIZED sized}, {@link Spliterator#DISTINCT distinct},
   * {@link Spliterator#ORDERED ordered}, and {@link Spliterator#SORTED sorted}.
   */
  @Override
  Spliterator.OfInt spliterator();

  @Override
  default Stream<Integer> stream() {
    return intStream().boxed();
  }
}
