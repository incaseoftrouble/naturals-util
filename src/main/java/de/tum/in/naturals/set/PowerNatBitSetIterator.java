// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class PowerNatBitSetIterator implements Iterator<NatBitSet> {
    private final NatBitSet baseSet;
    private boolean hasNext;
    private final NatBitSet current;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    PowerNatBitSetIterator(NatBitSet baseSet) {
        assert !baseSet.isEmpty();
        this.baseSet = baseSet;
        this.current = NatBitSets.of();
        this.hasNext = true;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public NatBitSet next() {
        if (!hasNext) {
            throw new NoSuchElementException("No next element");
        }

        boolean advanced = false;
        IntIterator iterator = baseSet.iterator();
        while (iterator.hasNext()) {
            int index = iterator.nextInt();
            if (current.contains(index)) {
                current.clear(index);
            } else {
                advanced = true;
                current.set(index);
                break;
            }
        }

        hasNext = advanced;
        return current;
    }
}
