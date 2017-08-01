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

package de.tum.in.naturals.unionfind;

import java.util.function.IntUnaryOperator;

public interface IntUnionFind extends IntUnaryOperator {
  /**
   * Extends the size of the domain by 1 element.
   *
   * @throws UnsupportedOperationException
   *     if the {@code add} operation is not supported by this implementation.
   * @see #add(int)
   */
  default void add() {
    add(1);
  }

  /**
   * Extends the size of the domain by {@code num} elements (optional operation).
   *
   * @throws UnsupportedOperationException
   *     if the {@code add} operation is not supported by this implementation.
   */
  void add(int num);

  @Override
  default int applyAsInt(int operand) {
    return find(operand);
  }

  /**
   * Returns the number of components.
   */
  int componentCount();

  /**
   * Returns true if the the two sites are in the same component.
   */
  default boolean connected(int p, int q) {
    return find(p) == find(q);
  }

  /**
   * Returns the component identifier for the component containing site {@code p}.
   */
  int find(int p);

  /**
   * Returns the number of elements in this union-find.
   */
  int size();

  /**
   * Merges the component containing site {@code p} with the
   * the component containing site {@code q}.
   */
  void union(int p, int q);
}
