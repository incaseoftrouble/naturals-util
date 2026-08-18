// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static de.tum.in.naturals.set.NatBitSetsUtil.SPLITERATOR_CHARACTERISTICS;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSpliterator;
import it.unimi.dsi.fastutil.ints.IntSpliterators;
import java.util.Spliterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnegative;

/**
 * A set of non-negative integers.
 */
public interface NatSet extends IntSet {
    /**
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     */
    @Override
    boolean add(@Nonnegative int index);

    /**
     * An index this set cannot hold is absent by definition, so removing it answers {@code false} rather
     * than throwing.
     */
    @Override
    boolean remove(int index);

    @SuppressWarnings("deprecation")
    @Override
    default Stream<Integer> stream() {
        return intStream().boxed();
    }

    /**
     * Returns an int stream compatible with the {@link #spliterator() spliterator}.
     */
    @Override
    default IntStream intStream() {
        return StreamSupport.intStream(this::spliterator, SPLITERATOR_CHARACTERISTICS, false);
    }

    /**
     * Returns a spliterator over this set. The spliterator is expected to be
     * {@link Spliterator#SIZED sized}, {@link Spliterator#DISTINCT distinct},
     * {@link Spliterator#ORDERED ordered}, and {@link Spliterator#SORTED sorted}.
     */
    @Override
    default IntSpliterator spliterator() {
        return IntSpliterators.asSpliterator(iterator(), size(), SPLITERATOR_CHARACTERISTICS);
    }

    /**
     * Returns an {@link IntIterator iterator} returning the elements of this set in descending order.
     *
     * @see #iterator()
     */
    IntIterator reverseIterator();
}
