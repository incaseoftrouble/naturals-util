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

/**
 * This package provides several wrapper and utility classes useful to deal with domains of the form
 * {@code {0, ..., n}}. Essentially, there are two different paradigms present:
 * <ul>
 * <li>{@link de.tum.in.naturals.set.NatBitSet} - This interface is intended for unbounded sets,
 * spanning the whole domain of non-negative integers. The complement of such sets is only of
 * limited use. While {@link java.lang.Integer#MAX_VALUE} imposes a practical upper bound on the
 * elements of the complement, in theory these sets are unbounded. This discrepancy can quickly lead
 * to implementation errors, thus defining the concept of complement for these sets has been
 * deliberately avoided. The central utility class {@link de.tum.in.naturals.set.NatBitSets} offers
 * some remedy, providing, e.g., iterators which yield the complement up to a certain length.</li>
 * <li>{@link de.tum.in.naturals.set.BoundedNatBitSet} - This interface extends the general sets
 * by a fixed domain size. Instead of dealing with all non-negative integers, sets of this kind only
 * contain numbers up to a given size. In order to have a consistent and predictable behaviour, some
 * potentially unintuitive decisions have been made when implementing the interface:
 * <ul>
 * <li>Any call to modifying methods on these sets with indices outside of the specified range
 * immediately fails with an {@link java.lang.UnsupportedOperationException}, even if the set would
 * not be modified as a consequence of this particular call (e.g. {@code set.clear(n + 1)}.</li>
 * <li>Read-only operations (like {@link java.util.Collection#contains(java.lang.Object)} are
 * allowed to go beyond this limit.</li>
 * </ul>
 * While not recommended, implementations are allowed to deviate from this requirement.
 * <p>Since a bound on the domain is known, a complement can be constructed in constant time.
 * Complements returned by the {@link de.tum.in.naturals.set.BoundedNatBitSet#complement()} method
 * are required to be <strong>views</strong> of the original set, i.e. changes to one are visible
 * in the other.
 * </p>
 * </li>
 * </ul>
 */
@EverythingIsNonnullByDefault
package de.tum.in.naturals.set;

import de.tum.in.naturals.EverythingIsNonnullByDefault;