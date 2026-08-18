// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class PowerBitSetIterator implements Iterator<BitSet> {
    private final BitSet baseSet;
    private final BitSet iteration;
    private final int baseCardinality;
    private int numSetBits = -1;

    PowerBitSetIterator(BitSet baseSet) {
        this.baseSet = baseSet;
        this.baseCardinality = baseSet.cardinality();
        this.iteration = new BitSet(baseSet.length());
    }

    @Override
    public boolean hasNext() {
        return numSetBits < baseCardinality;
    }

    @Override
    public BitSet next() {
        if (numSetBits == -1) {
            numSetBits = 0;
            return iteration;
        }

        if (numSetBits == baseCardinality) {
            throw new NoSuchElementException("No next element");
        }

        IntIterator iterator = BitSets.iterator(baseSet);
        while (iterator.hasNext()) {
            int index = iterator.nextInt();
            if (iteration.get(index)) {
                iteration.clear(index);
                numSetBits -= 1;
            } else {
                iteration.set(index);
                numSetBits += 1;
                break;
            }
        }

        return iteration;
    }
}
