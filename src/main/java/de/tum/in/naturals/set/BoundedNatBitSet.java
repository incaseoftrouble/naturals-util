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
import javax.annotation.Nonnegative;

/**
 * An extension to {@link NatBitSet} specialized for bounded, non-negative integer domains.
 */
public interface BoundedNatBitSet extends NatBitSet {
  @Override
  BoundedNatBitSet clone();

  /**
   * Returns a complement view of this set. The returned set contains exactly those values in
   * {@code {0, ..., domainSize() - 1}} which are not contained in this. The returned set also is
   * backed by this set, i.e. changes to one are visible in the other.
   */
  BoundedNatBitSet complement();

  /**
   * The size of the domain of this set. The set only contains values between zero (inclusive) and
   * the returned value (exclusive).
   */
  @Nonnegative
  int domainSize();

  /**
   * Adds all elements of the domain which are not contained in the given collection to this set.
   */
  void orNot(IntCollection ints);
}
