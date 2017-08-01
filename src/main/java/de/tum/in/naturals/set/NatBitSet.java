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
 * A set of non-negative integers, allowing potentially more efficient collection methods.
 *
 * <p>Since the Java collection API requires to, e.g., return a boolean indicating whether the
 * collection was modified due to a particular modification, it disallows certain efficient
 * implementations based on, e.g., bit-wise operations. This interface tries to tackle this
 * problem by providing modification methods without return type, corresponding to the API of
 * {@link BitSet}.</p>
 */
public interface NatBitSet extends NatSet, Cloneable {
  /**
   * Computes the intersection with the given collection.
   *
   * @see Collection#retainAll(Collection)
   * @see BitSet#and(BitSet)
   */
  void and(IntCollection ints);

  /**
   * Removes all elements of the collection from this set.
   *
   * @see Collection#removeAll(Collection)
   * @see BitSet#andNot(BitSet)
   */
  void andNot(IntCollection ints);

  /**
   * Removes the given index.
   *
   * @see #clear(int, int)
   * @see Collection#remove(Object)
   * @see BitSet#clear(int)
   */
  void clear(@Nonnegative int index);

  /**
   * Removes the given range.
   *
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
  int firstInt();

  /**
   * Flips the given index.
   *
   * @see #flip(int)
   * @see BitSet#flip(int)
   */
  void flip(@Nonnegative int index);

  /**
   * Flips all values in the given range.
   *
   * @see #flip(int)
   * @see BitSet#flip(int, int)
   */
  void flip(@Nonnegative int from, @Nonnegative int to);

  /**
   * Determines whether this set has any element in common with the given collection.
   *
   * @see BitSet#intersects(BitSet)
   */
  boolean intersects(IntCollection ints);

  /**
   * Returns the last (highest) element currently in this set.
   *
   * @throws NoSuchElementException
   *     if this set is empty
   * @see SortedSet#last()
   */
  int lastInt();

  /**
   * Adds all elements of the given collection to this set.
   *
   * @see Collection#addAll(Collection)
   * @see BitSet#or(BitSet)
   */
  void or(IntCollection ints);

  /**
   * Adds the given index to this set.
   *
   * @see Collection#add(Object)
   * @see BitSet#set(int)
   */
  void set(@Nonnegative int index);

  /**
   * Adds or removes the given index, based on the given value.
   *
   * @see BitSet#set(int, boolean)
   */
  void set(@Nonnegative int index, boolean value);

  /**
   * Adds the given range to this set.
   *
   * @see BitSet#set(int, int)
   */
  void set(@Nonnegative int from, @Nonnegative int to);

  /**
   * Computes the exclusive or with the given collection. After the call to this method the set
   * contains all values which are contained either in the set before the call or in the given
   * collection, but not both.
   *
   * @see BitSet#xor(BitSet)
   */
  void xor(IntCollection ints);
}
