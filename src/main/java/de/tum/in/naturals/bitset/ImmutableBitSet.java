// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import java.util.BitSet;

/**
 * An immutable copy of a {@link BitSet}.
 *
 * <p>Note that this is a Guava-style copy instead of a Collections API-style view on the set. It
 * has the same performance properties as the original BitSet implementation.
 */
public final class ImmutableBitSet extends BitSet {
    private static final ImmutableBitSet EMPTY = new ImmutableBitSet();
    private static final long serialVersionUID = -481427560402287503L;

    private ImmutableBitSet() {
        super(0);
    }

    private ImmutableBitSet(BitSet bitSet) {
        super(bitSet.length());
        super.or(bitSet);
    }

    public static ImmutableBitSet copyOf(BitSet bitSet) {
        if (bitSet instanceof ImmutableBitSet) {
            return (ImmutableBitSet) bitSet;
        }
        if (bitSet.isEmpty()) {
            return EMPTY;
        }
        return new ImmutableBitSet(bitSet);
    }

    public static ImmutableBitSet of() {
        return EMPTY;
    }

    @Override
    public void and(BitSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void andNot(BitSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear(int bitIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableBitSet clone() {
        return this;
    }

    @Override
    public void flip(int bitIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flip(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void or(BitSet set) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int bitIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int bitIndex, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(int fromIndex, int toIndex, boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void xor(BitSet set) {
        throw new UnsupportedOperationException();
    }
}
