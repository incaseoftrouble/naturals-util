// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.Collection;
import javax.annotation.Nonnegative;

/**
 * An extension to {@link NatBitSet} specialized for bounded, non-negative integer domains.
 *
 * <p>An operation fails with an {@link IndexOutOfBoundsException} exactly when it would have to
 * <em>write</em> outside {@code {0, ..., domainSize() - 1}} - negative indices and indices at or beyond
 * {@link #domainSize()} count the same way.</p>
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
     *     if {@code value} is {@code true} and {@code index} is outside the domain. Clearing outside the
     *     domain is a no-op.
     */
    @Override
    void set(int index, boolean value);

    /**
     * @throws IndexOutOfBoundsException
     *     if {@code from} or {@code to} is negative, {@code to} is less than {@code from},
     *     or {@code to} is greater than {@link #domainSize()} and the range is not empty.
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
     *     if {@code from} or {@code to} is negative, {@code to} is less than {@code from}, or
     *     {@code to} is greater than the {@link #domainSize()} and the range is not empty.
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
     * Replaces the contents of this set by its complement within {@code {0, ..., domainSize() - 1}}.
     */
    void complement();
}
