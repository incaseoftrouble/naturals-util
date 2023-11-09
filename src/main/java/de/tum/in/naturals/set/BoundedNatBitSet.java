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
import java.util.Collection;
import javax.annotation.Nonnegative;

/**
 * An extension to {@link NatBitSet} specialized for bounded, non-negative integer domains.
 */
public interface BoundedNatBitSet extends NatBitSet {

    // Accessors

    /**
     * The size of the domain of this set. The set only contains values between zero (inclusive) and
     * the returned value (exclusive).
     */
    @Nonnegative
    int domainSize();

    // Mutators

    /**
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    void set(int index);

    /**
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    void set(int index, boolean value);

    /**
     * @throws IndexOutOfBoundsException
     *     if {@code from} or {@code to} is negative, {@code to} is less than {@code from},
     *     or {@code to} is greater than {@link #domainSize()}.
     */
    @Override
    void set(int from, int to);

    /**
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    boolean add(@Nonnegative int index);

    /**
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    void flip(int index);

    /**
     * @throws IndexOutOfBoundsException
     *     if {@code from} or {@code to} is negative or greater than the {@link #domainSize()},
     *     or {@code to} is less than {@code from}.
     */
    @Override
    void flip(int from, int to);

    // Bulk operations

    /**
     * @throws IndexOutOfBoundsException
     *     if any index is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    void or(IntCollection indices);

    /**
     * @throws IndexOutOfBoundsException
     *     if any index is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    boolean addAll(Collection<? extends Integer> indices);

    /**
     * @throws IndexOutOfBoundsException
     *     if any index is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    boolean addAll(IntCollection indices);

    /**
     * Adds all elements of the domain which are not contained in the given indices to this set. This
     * is equivalent to<pre>
     *   for(int i = 0; i &lt; domainSize(); i++) {
     *     if (!indices.contains(i)) add(i);
     *   }
     * </pre>
     */
    void orNot(IntCollection indices);

    /**
     * @throws IndexOutOfBoundsException
     *     if any index is negative or greater or equal to the {@link #domainSize()}.
     */
    @Override
    void xor(IntCollection indices);

    // Clone

    @Override
    BoundedNatBitSet clone();

    /**
     * Returns a complement view of this set. The returned set contains exactly those values in
     * {@code {0, ..., domainSize() - 1}} which are not contained in this. The returned set also is
     * backed by this set, i.e. changes to one are visible in the other.
     */
    BoundedNatBitSet complement();
}
