// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.Size64;
import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

class PowerBitSetSimple extends AbstractSet<BitSet> implements Size64 {
    private final int baseSize;

    PowerBitSetSimple(int size) {
        baseSize = size;
    }

    @Override
    public boolean contains(@Nullable Object obj) {
        return obj instanceof BitSet && ((BitSet) obj).length() <= size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PowerBitSetSimple) {
            PowerBitSetSimple other = (PowerBitSetSimple) obj;
            return baseSize == other.baseSize;
        }
        if (obj instanceof PowerBitSet) {
            PowerBitSet other = (PowerBitSet) obj;
            return baseSize == other.getBaseCardinality() && baseSize == other.getBaseLength();
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return HashCommon.mix(baseSize);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Returns an iterator over the power set. <strong>Warning</strong>: To avoid repeated allocation,
     * the returned set is modified in-place!
     */
    @Override
    public Iterator<BitSet> iterator() {
        return new PowerBitSetSimpleIterator(baseSize);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int size() {
        return baseSize >= Integer.SIZE ? Integer.MAX_VALUE : 1 << baseSize;
    }

    @Override
    public long size64() {
        return 1L << baseSize;
    }

    @Override
    public String toString() {
        if (baseSize == 0) {
            return "powerSet({})";
        }
        return String.format("powerSet({0,..,%s})", baseSize);
    }

    int getBaseSize() {
        return baseSize;
    }
}
