// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nonnegative;

/**
 * A set of non-negative integers, allowing potentially more efficient indices methods.
 *
 * <p>Since the Java indices API requires to, e.g., return a boolean indicating whether the
 * indices was modified due to a particular modification, it disallows certain efficient
 * implementations based on, e.g., bit-wise operations. This interface tries to tackle this
 * problem by providing modification methods without return type, corresponding to the API of
 * {@link BitSet}.</p>
 */
public interface NatBitSet extends NatSet, Cloneable {
    // Accessors

    /**
     * Returns the first (smallest) element currently in this set.
     *
     * @throws NoSuchElementException
     *     if this set is empty
     * @see SortedSet#first()
     */
    @Nonnegative
    default int firstInt() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return iterator().nextInt();
    }

    /**
     * Returns the last (highest) element currently in this set.
     *
     * @throws NoSuchElementException
     *     if this set is empty
     * @see SortedSet#last()
     */
    @Nonnegative
    default int lastInt() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return reverseIterator().nextInt();
    }

    /**
     * Returns the smallest index larger or equal to {@code index} which is contained in this set or
     * -1 if none.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     * @see BitSet#nextSetBit(int)
     */
    int nextPresentIndex(@Nonnegative int index);

    /**
     * Returns the smallest index larger or equal to {@code index} which is not contained in this set.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     * @see BitSet#nextClearBit(int)
     */
    int nextAbsentIndex(@Nonnegative int index);

    /**
     * Returns the largest index smaller or equal to {@code index} which is contained in this set or
     * -1 if none.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     * @see BitSet#previousSetBit(int)
     */
    int previousPresentIndex(@Nonnegative int index);

    /**
     * Returns the largest index smaller or equal to {@code index} which is not contained in this set
     * or -1 if none.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     * @see BitSet#nextSetBit(int)
     */
    int previousAbsentIndex(@Nonnegative int index);

    /**
     * Returns an {@link IntIterator iterator} returning the elements of this set in ascending order.
     */
    @Override
    default IntIterator iterator() {
        if (isEmpty()) {
            return IntIterators.EMPTY_ITERATOR;
        }
        return new NatBitSetIterator(this);
    }

    @Override
    default IntIterator reverseIterator() {
        if (isEmpty()) {
            return IntIterators.EMPTY_ITERATOR;
        }
        return new ReverseIntBidiIterator(new NatBitSetBidiIterator(this, lastInt() + 1));
    }

    // Mutators

    /**
     * Adds the given index to this set.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     * @see Collection#add(Object)
     * @see BitSet#set(int)
     */
    void set(@Nonnegative int index);

    /**
     * Adds or removes the given index, based on the given value.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code value} is {@code true} and {@code index} is negative. Removing an index this set
     *     cannot hold is a no-op, see {@link #clear(int)}.
     * @see BitSet#set(int, boolean)
     */
    void set(@Nonnegative int index, boolean value);

    /**
     * Adds the given range to this set.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code from} or {@code to} is negative or {@code to} is less than {@code from}.
     * @see BitSet#set(int, int)
     */
    void set(@Nonnegative int from, @Nonnegative int to);

    /**
     * Removes the given index. An index this set cannot hold - a negative one, or one outside the domain
     * of a {@link BoundedNatBitSet} - is absent by definition, so removing it is a no-op rather than an
     * error.
     *
     * @see #clear(int, int)
     * @see Collection#remove(Object)
     * @see BitSet#clear(int)
     */
    void clear(int index);

    /**
     * Removes the given range ({@code from} inclusive, {@code to} exclusive). The range is clamped to the
     * indices this set can hold.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code to} is less than {@code from}.
     * @see #clear(int)
     * @see BitSet#clear(int, int)
     */
    void clear(int from, int to);

    /**
     * Removes all indices larger or equal than {@code from}. Equivalent to calling<pre>
     * set.clear(from, Integer.MAX_VALUE);
     * </pre>
     *
     * @see #clear(int, int)
     * @see #lastInt()
     */
    void clearFrom(int from);

    /**
     * Flips the given index.
     *
     * <p>This is equivalent to<pre>
     * if (contains(index)) remove(index);
     * else add(index);
     * </pre>
     *
     * @throws IndexOutOfBoundsException
     *     if {@code index} is negative.
     * @see #flip(int, int)
     * @see BitSet#flip(int)
     */
    void flip(@Nonnegative int index);

    /**
     * Flips all values in the given range.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code from} or {@code to} is negative or {@code to} is less than {@code from}.
     * @see #flip(int)
     * @see BitSet#flip(int)
     */
    void flip(@Nonnegative int from, @Nonnegative int to);

    // Bulk operations

    /**
     * Determines whether this set has any element in common with the given indices.
     *
     * @see BitSet#intersects(BitSet)
     */
    default boolean intersects(Collection<Integer> indices) {
        IntIterator iterator = iterator();
        while (iterator.hasNext()) {
            if (indices.contains(iterator.nextInt())) {
                return true;
            }
        }
        return false;
    }

    default boolean isSubsetOf(Collection<Integer> indices) {
        return indices.containsAll(this);
    }

    /**
     * Computes the intersection with the given indices.
     *
     * @see Collection#retainAll(Collection)
     * @see BitSet#and(BitSet)
     */
    void and(IntCollection indices);

    /**
     * Removes all elements of the indices from this set.
     *
     * @see Collection#removeAll(Collection)
     * @see BitSet#andNot(BitSet)
     */
    void andNot(IntCollection indices);

    /**
     * Adds all elements of the given indices to this set.
     *
     * @see Collection#addAll(Collection)
     * @see BitSet#or(BitSet)
     */
    void or(IntCollection indices);

    /**
     * Computes the exclusive or with the given indices. After the call to this method the set
     * contains all values which are contained either in the set before the call or in the given
     * indices, but not both.
     *
     * @see BitSet#xor(BitSet)
     */
    void xor(IntCollection indices);

    @Override
    default boolean retainAll(Collection<?> indices) {
        if (indices instanceof IntCollection) {
            return retainAll((IntCollection) indices);
        }
        if (indices instanceof Set) {
            Set<?> set = (Set<?>) indices;
            return removeIf(i -> !set.contains(i));
        }
        IntSet set = new IntOpenHashSet();
        indices.forEach(i -> {
            if (i instanceof Integer) {
                set.add((int) i);
            }
        });
        return retainAll(set);
    }

    @Override
    default boolean removeAll(Collection<?> indices) {
        if (indices instanceof IntCollection) {
            return removeAll((IntCollection) indices);
        }
        int before = size();
        indices.forEach(i -> {
            if (i instanceof Integer) {
                clear((int) i);
            }
        });
        return before != size();
    }

    /**
     * Optimise internal representation for read-heavy use. Contents are unchanged.
     *
     * @return whether the representation changed.
     */
    default boolean optimize() {
        // Nothing to gain
        return false;
    }

    // Clone

    NatBitSet clone();
}
