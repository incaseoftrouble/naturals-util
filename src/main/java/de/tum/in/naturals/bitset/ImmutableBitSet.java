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

    @SuppressWarnings("MethodDoesntCallSuperMethod")
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
