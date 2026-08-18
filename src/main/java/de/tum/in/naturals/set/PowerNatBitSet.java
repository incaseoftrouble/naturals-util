// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.Size64;
import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

class PowerNatBitSet extends AbstractSet<NatBitSet> implements Size64 {
    private final NatBitSet baseSet;
    private final int baseSize;

    PowerNatBitSet(NatBitSet baseSet) {
        assert !baseSet.isEmpty();
        this.baseSet = baseSet.clone();
        baseSize = this.baseSet.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(@Nullable Object obj) {
        if (obj instanceof IntCollection) {
            return baseSet.containsAll((IntCollection) obj);
        }
        if (!(obj instanceof Collection)) {
            return false;
        }
        // Any other collection of Integers is a member just as well. Probed element-wise rather than
        // through containsAll, which throws rather than answering false on a non-Integer element.
        for (Object element : (Collection<?>) obj) {
            if (!(element instanceof Integer) || !baseSet.contains((int) element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PowerNatBitSet) {
            PowerNatBitSet other = (PowerNatBitSet) obj;
            return baseSet.equals(other.baseSet);
        }
        Logger.getLogger(PowerNatBitSet.class.getName()).log(Level.WARNING, "Calling equals on PowerNatBitSet");
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        Logger.getLogger(PowerNatBitSet.class.getName()).log(Level.WARNING, "Calling hashCode on PowerNatBitSet");
        return super.hashCode();
    }

    /**
     * Returns an iterator over the power set.
     * <strong>Warning</strong>: To avoid repeated allocation, the returned set is modified in-place!
     */
    @Override
    public Iterator<NatBitSet> iterator() {
        return new PowerNatBitSetIterator(baseSet);
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
}
