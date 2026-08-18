// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.BitSet;
import java.util.NoSuchElementException;

/**
 * Ascending iterator over the set bits, which walks a run at a time where that pays.
 *
 * <p>Inside a run the next element is the one after this one, and finding out costs an increment against
 * the bit search a plain scan makes for every element - worth up to four times on run structured sets. On
 * scattered ones it is a loss instead, since the run then ends at every element and the extra search that
 * establishes that is on top of the one the plain scan already makes.
 *
 * <p>Which it is cannot be known in advance, but this does not have to know in advance: both ways of
 * finding the next element are correct at any point, so it starts run aware, measures the first
 * {@value BitSets#SAMPLE_RUNS} runs it walks anyway, and drops to the plain scan for the rest if they came
 * out shorter than {@value BitSets#RUN_LENGTH_THRESHOLD} elements on average - the same rule that
 * {@link BitSets#forEach(BitSet, java.util.function.IntConsumer)} applies to its own traversal.
 */
final class BitSetIterator implements IntIterator {
    /** No index is below this, so a run end of {@code -1} sends every element through the bit search. */
    private static final int PER_ELEMENT = -1;

    private final BitSet bitSet;
    private int current = -1;
    private int next;
    /** Exclusive end of the run holding {@link #next}, or {@link #PER_ELEMENT}. */
    private int runEnd = PER_ELEMENT;

    private boolean runAware = true;
    private int runs;
    private int elements;

    BitSetIterator(BitSet bitSet) {
        this.bitSet = bitSet;
        advance(0);
    }

    /** Moves to the first element at or after {@code from}, and takes in the run it belongs to. */
    private void advance(int from) {
        int start = bitSet.nextSetBit(from);
        next = start;
        if (!runAware || start < 0) {
            return;
        }
        int end = bitSet.nextClearBit(start);
        runEnd = end;
        if (runs < BitSets.SAMPLE_RUNS) {
            runs += 1;
            elements += end - start;
            if (runs == BitSets.SAMPLE_RUNS && elements < BitSets.RUN_LENGTH_THRESHOLD * runs) {
                runAware = false;
                runEnd = PER_ELEMENT;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return next != -1;
    }

    @Override
    public int nextInt() {
        if (next == -1) {
            throw new NoSuchElementException();
        }
        current = next;
        int candidate = current + 1;
        if (candidate < runEnd) {
            next = candidate;
        } else {
            advance(candidate);
        }
        return current;
    }

    @Override
    public void remove() {
        if (current == -1) {
            throw new IllegalStateException();
        }
        assert bitSet.get(current);
        bitSet.clear(current);
        current = -1;
    }
}
