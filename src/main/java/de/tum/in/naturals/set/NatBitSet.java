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
import java.util.BitSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import javax.annotation.Nonnegative;

/**
 * A set of non-negative integers, allowing potentially more efficient indices methods.
 *
 * <p>Since the Java indices API requires to, e.g., return a boolean indicating whether the
 * indices was modified due to a particular modification, it disallows certain efficient
 * implementations based on, e.g., bit-wise operations. This interface tries to tackle this
 * problem by providing modification methods without return type, corresponding to the API of
 * {@link BitSet}.</p>
 */
public interface NatBitSet extends NatSet, Cloneable {
  /**
   * Computes the intersection with the given indices.
   *
   * @see Collection#retainAll(Collection)
   * @see BitSet#and(BitSet)
   */
  void and(IntCollection indices);

  /**
   * Removes all elements of the indices from this set.
   *
   * @see Collection#removeAll(Collection)
   * @see BitSet#andNot(BitSet)
   */
  void andNot(IntCollection indices);

  /**
   * Removes the given index.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   * @see #clear(int, int)
   * @see Collection#remove(Object)
   * @see BitSet#clear(int)
   */
  void clear(@Nonnegative int index);

  /**
   * Removes the given range.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code from} or {@code to} is negative or {@code to} is less than {@code from}.
   * @see #clear(int)
   * @see BitSet#clear(int, int)
   */
  void clear(@Nonnegative int from, @Nonnegative int to);

  NatBitSet clone();

  /**
   * Returns the first (smallest) element currently in this set.
   *
   * @throws NoSuchElementException
   *     if this set is empty
   * @see SortedSet#first()
   */
  @Nonnegative
  int firstInt();

  /**
   * Flips the given index.
   *
   * <p>This is equivalent to<pre>
   * if (contains(index)) remove(index);
   * else add(index);
   * </pre></p>
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   * @see #flip(int, int)
   * @see BitSet#flip(int)
   */
  void flip(@Nonnegative int index);

  /**
   * Flips all values in the given range.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code from} or {@code to} is negative or {@code to} is less than {@code from}.
   * @see #flip(int)
   * @see BitSet#flip(int)
   */
  void flip(@Nonnegative int from, @Nonnegative int to);

  /**
   * Determines whether this set has any element in common with the given indices.
   *
   * @see BitSet#intersects(BitSet)
   */
  boolean intersects(IntCollection indices);

  /**
   * Returns the last (highest) element currently in this set.
   *
   * @throws NoSuchElementException
   *     if this set is empty
   * @see SortedSet#last()
   */
  @Nonnegative
  int lastInt();

  /**
   * Returns the smallest index larger or equal to {@code index} which is not contained in this set.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   * @see BitSet#nextClearBit(int)
   */
  int nextAbsentIndex(@Nonnegative int index);

  /**
   * Returns the smallest index larger or equal to {@code index} which is contained in this set or
   * -1 if none.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   * @see BitSet#nextSetBit(int)
   */
  int nextPresentIndex(@Nonnegative int index);

  /**
   * Adds all elements of the given indices to this set.
   *
   * @see Collection#addAll(Collection)
   * @see BitSet#or(BitSet)
   */
  void or(IntCollection indices);

  /**
   * Adds the given index to this set.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   * @see Collection#add(Object)
   * @see BitSet#set(int)
   */
  void set(@Nonnegative int index);

  /**
   * Adds or removes the given index, based on the given value.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code index} is negative.
   * @see BitSet#set(int, boolean)
   */
  void set(@Nonnegative int index, boolean value);

  /**
   * Adds the given range to this set.
   *
   * @throws IndexOutOfBoundsException
   *     if {@code from} or {@code to} is negative or {@code to} is less than {@code from}.
   * @see BitSet#set(int, int)
   */
  void set(@Nonnegative int from, @Nonnegative int to);

  /**
   * Computes the exclusive or with the given indices. After the call to this method the set
   * contains all values which are contained either in the set before the call or in the given
   * indices, but not both.
   *
   * @see BitSet#xor(BitSet)
   */
  void xor(IntCollection indices);
}
