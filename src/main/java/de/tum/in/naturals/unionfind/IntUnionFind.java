// SPDX-License-Identifier: Apache-2.0

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
     * Returns true if the two sites are in the same component.
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
     * Merges the component containing site {@code p} with the component containing site {@code q}.
     */
    void union(int p, int q);
}
