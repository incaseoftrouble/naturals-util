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

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.Size64;
import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Iterator;
import javax.annotation.Nullable;

class PowerBitSet extends AbstractSet<BitSet> implements Size64 {
    private final BitSet baseSet;
    private final int baseSize;

    PowerBitSet(BitSet baseSet) {
        this.baseSet = (BitSet) baseSet.clone();
        baseSize = this.baseSet.cardinality();
    }

    @Override
    public boolean contains(@Nullable Object obj) {
        return obj instanceof BitSet && BitSets.isSubset((BitSet) obj, baseSet);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PowerBitSet) {
            PowerBitSet other = (PowerBitSet) obj;
            return baseSet.equals(other.baseSet);
        }
        if (obj instanceof PowerBitSetSimple) {
            PowerBitSetSimple other = (PowerBitSetSimple) obj;
            return getBaseCardinality() == other.getBaseSize() && getBaseLength() == other.getBaseSize();
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return HashCommon.mix(baseSet.hashCode());
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
        return new PowerBitSetIterator(baseSet);
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
        return String.format("powerSet(%s)", baseSet);
    }

    int getBaseCardinality() {
        return baseSize;
    }

    int getBaseLength() {
        return baseSet.length();
    }
}
