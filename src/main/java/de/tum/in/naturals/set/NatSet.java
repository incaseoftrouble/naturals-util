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
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     */
    @Override
    boolean remove(@Nonnegative int index);

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
