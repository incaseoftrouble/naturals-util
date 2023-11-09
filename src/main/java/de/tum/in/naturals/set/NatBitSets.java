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

import com.zaxxer.sparsebits.SparseBitSet;
import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.SparseBitSets;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import javax.annotation.Nonnegative;
import org.roaringbitmap.RoaringBitmap;

public final class NatBitSets {
    public static final int UNKNOWN_LENGTH = -1;
    public static final int UNKNOWN_SIZE = -1;
    private static NatBitSetFactory factory = new RoaringNatBitSetFactory();

    private NatBitSets() {}

    /**
     * Returns an unmodifiable iterator yielding all elements in {@code {0, ..., length - 1} \ set} in
     * ascending order.
     *
     * @param set
     *     The set to be complemented.
     * @param length
     *     The size of the domain.
     *
     * @return an unmodifiable iterator over the complement.
     */
    public static IntIterator complementIterator(NatBitSet set, @Nonnegative int length) {
        if (set.isEmpty() || set.firstInt() >= length) {
            return IntIterators.fromTo(0, length);
        }
        if (set instanceof FixedSizeNatBitSet) {
            int size = set.size();
            if (size >= length) {
                return IntIterators.EMPTY_ITERATOR;
            }
            return IntIterators.fromTo(size, length);
        }
        if (set instanceof MutableSingletonNatBitSet) {
            int element = set.firstInt();
            if (element == 0) {
                return IntIterators.fromTo(1, length);
            }
            if (length <= element + 1) {
                return IntIterators.fromTo(0, length);
            }
            return IntIterators.concat(IntIterators.fromTo(0, element), IntIterators.fromTo(element + 1, length));
        }
        return IntIterators.unmodifiable(new NatBitSetComplementIterator(set, length));
    }

    public static IntIterator complementReverseIterator(NatBitSet set, @Nonnegative int length) {
        if (set.isEmpty() || set.firstInt() >= length) {
            return new ReverseRangeIterator(0, length);
        }
        if (set instanceof FixedSizeNatBitSet) {
            int size = set.size();
            if (size >= length) {
                return IntIterators.EMPTY_ITERATOR;
            }
            return new ReverseRangeIterator(size, length);
        }
        if (set instanceof MutableSingletonNatBitSet) {
            int element = set.firstInt();
            if (element == 0) {
                return new ReverseRangeIterator(1, length);
            }
            if (length <= element + 1) {
                return IntIterators.fromTo(0, length);
            }
            IntIterator firstIterator = new ReverseRangeIterator(element + 1, length);
            IntIterator secondIterator = new ReverseRangeIterator(0, element);
            return IntIterators.concat(firstIterator, secondIterator);
        }
        return IntIterators.unmodifiable(new NatBitSetComplementReverseIterator(set, length));
    }

    // --- Unbounded Sets ---

    public static NatBitSet longSet() {
        return new LongNatBitSet();
    }

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public static NatBitSet simpleSet() {
        return new SimpleNatBitSet(new BitSet());
    }

    public static NatBitSet simpleSet(@Nonnegative int expectedSize) {
        return new SimpleNatBitSet(new BitSet(expectedSize));
    }

    public static NatBitSet sparseSet() {
        return new SparseNatBitSet(new SparseBitSet());
    }

    public static NatBitSet sparseSet(@Nonnegative int expectedSize) {
        return new SparseNatBitSet(new SparseBitSet(expectedSize));
    }

    public static NatBitSet roaringSet() {
        return new RoaringNatBitSet(new RoaringBitmap());
    }

    /**
     * Returns a view on the given {@code bitSet}.
     */
    public static NatBitSet asSet(BitSet bitSet) {
        return new SimpleNatBitSet(bitSet);
    }

    /**
     * Returns a view on the given {@code bitSet}.
     */
    public static NatBitSet asSet(SparseBitSet bitSet) {
        return new SparseNatBitSet(bitSet);
    }

    /**
     * Returns a view on the given {@code set}.
     *
     * <p>The set may not contain negative indices.</p>
     *
     * <p><strong>Warning:</strong> The returned view is not efficient.</p>
     */
    public static NatBitSet view(IntSortedSet set) {
        assert set.isEmpty() || set.firstInt() >= 0;
        return new ForwardingNatBitSet(set);
    }

    // --- Bounded Sets ---

    public static BoundedNatBitSet boundedLongSet(int domainSize) {
        return new LongBoundedNatBitSet(domainSize);
    }

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public static BoundedNatBitSet boundedSimpleSet(int domainSize) {
        return new SimpleBoundedNatBitSet(new BitSet(), domainSize);
    }

    public static BoundedNatBitSet boundedSparseSet(int domainSize) {
        return new SparseBoundedNatBitSet(new SparseBitSet(), domainSize);
    }

    public static BoundedNatBitSet boundedRoaringSet(int domainSize) {
        return new RoaringBoundedNatBitSet(new RoaringBitmap(), domainSize);
    }

    /**
     * Return a view on the given {@code bitSet}.
     */
    public static BoundedNatBitSet asBoundedSet(BitSet bitSet, int domainSize) {
        return new SimpleBoundedNatBitSet(bitSet, domainSize);
    }

    /**
     * Return a view on the given {@code bitSet}.
     */
    public static BoundedNatBitSet asBoundedSet(SparseBitSet bitSet, int domainSize) {
        return new SparseBoundedNatBitSet(bitSet, domainSize);
    }

    /**
     * Ensures that the given {@code set} is a {@link BoundedNatBitSet}. When possible, the backing
     * data structure is shallow copied. For example, when passing a {@link SimpleNatBitSet}, a
     * {@link SimpleBoundedNatBitSet} with the same backing bit set will be returned. Note that after
     * this operation, only the returned set should be used to ensure integrity.
     *
     * <p><strong>Warning</strong>: If {@code set} already is a {@link BoundedNatBitSet} with
     * different domain size, an exception will be thrown, to avoid potentially unexpected behavior
     * </p>
     *
     * @throws IndexOutOfBoundsException
     *     if {@code set} contains an index larger than {@code domainSize}.
     * @throws IllegalArgumentException
     *     if {@code set} already is a {@link BoundedNatBitSet} and has a differing domain size.
     */
    public static BoundedNatBitSet asBounded(NatBitSet set, @Nonnegative int domainSize) {
        assert domainSize >= 0;
        if (!set.isEmpty() && set.lastInt() >= domainSize) {
            throw new IndexOutOfBoundsException();
        }
        if (set instanceof BoundedNatBitSet) {
            BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;
            int oldDomainSize = boundedSet.domainSize();
            if (oldDomainSize != domainSize) {
                throw new IllegalArgumentException(String.format(
                        "Given set has domain size %d, expected %d", boundedSet.domainSize(), domainSize));
            }
            return boundedSet;
        }
        if (set instanceof SimpleNatBitSet) {
            SimpleNatBitSet simpleSet = (SimpleNatBitSet) set;
            return new SimpleBoundedNatBitSet(simpleSet.getBitSet(), domainSize);
        }
        if (set instanceof SparseNatBitSet) {
            SparseNatBitSet sparseSet = (SparseNatBitSet) set;
            return new SparseBoundedNatBitSet(sparseSet.getBitSet(), domainSize);
        }
        if (set instanceof LongNatBitSet) {
            LongNatBitSet longSet = (LongNatBitSet) set;
            return new LongBoundedNatBitSet(longSet.getStore(), domainSize);
        }
        if (set instanceof MutableSingletonNatBitSet) {
            MutableSingletonNatBitSet singletonSet = (MutableSingletonNatBitSet) set;
            return singletonSet.isEmpty()
                    ? new BoundedMutableSingletonNatBitSet(domainSize)
                    : new BoundedMutableSingletonNatBitSet(singletonSet.firstInt(), domainSize);
        }

        return new BoundedWrapper(set, domainSize);
    }

    // --- Special Cases ---

    // Empty

    /**
     * Returns an empty set.
     */
    public static NatBitSet emptySet() {
        return new MutableSingletonNatBitSet();
    }

    /**
     * Returns an empty set over the given domain.
     */
    public static BoundedNatBitSet boundedEmptySet(@Nonnegative int domainSize) {
        return new FixedSizeNatBitSet(domainSize).complement();
    }

    // Singleton

    public static NatBitSet singleton(@Nonnegative int element) {
        return new MutableSingletonNatBitSet(element);
    }

    public static BoundedNatBitSet boundedSingleton(int domainSize, int element) {
        return new BoundedMutableSingletonNatBitSet(element, domainSize);
    }

    // Full

    /**
     * Returns the set containing the full domain {@code {0, ..., length - 1}}.
     */
    public static BoundedNatBitSet boundedFullSet(@Nonnegative int length) {
        return new FixedSizeNatBitSet(length);
    }

    // Power

    /**
     * Returns the set containing all subsets of the given basis.
     * <strong>Warning</strong>: For performance reasons, the iterator of this set may modify the
     * returned elements in place.
     */
    public static Set<NatBitSet> powerSet(NatBitSet basis) {
        if (basis.isEmpty()) {
            return Collections.singleton(emptySet());
        }
        return new PowerNatBitSet(basis);
    }

    /**
     * Returns the set containing subsets of {0, ..., i-1}.
     * <strong>Warning</strong>: For performance reasons, the iterator of this set may modify the
     * returned elements in place.
     */
    public static Set<NatBitSet> powerSet(@Nonnegative int domainSize) {
        return powerSet(boundedFullSet(domainSize));
    }

    // --- Extraction ---

    public static BitSet toBitSet(NatBitSet indices) {
        if (indices.isEmpty()) {
            return new BitSet(0);
        }
        if (indices instanceof SimpleNatBitSet) {
            return (BitSet) ((SimpleNatBitSet) indices).getBitSet().clone();
        }
        if (indices instanceof SimpleBoundedNatBitSet) {
            SimpleBoundedNatBitSet boundedSet = (SimpleBoundedNatBitSet) indices;
            BitSet bitSet = (BitSet) boundedSet.getBitSet().clone();
            if (boundedSet.isComplement()) {
                bitSet.flip(0, boundedSet.domainSize());
            }
            return bitSet;
        }
        if (indices instanceof SparseNatBitSet) {
            return BitSets.of(((SparseNatBitSet) indices).getBitSet());
        }
        if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet boundedSet = (SparseBoundedNatBitSet) indices;
            if (boundedSet.isComplement()) {
                BitSet bitSet = new BitSet(boundedSet.domainSize());
                boundedSet.forEach((IntConsumer) bitSet::set);
                return bitSet;
            }
            return BitSets.of(boundedSet.getBitSet());
        }

        BitSet bitSet = new BitSet(indices.lastInt() + 1);
        indices.forEach((IntConsumer) bitSet::set);
        return bitSet;
    }

    public static SparseBitSet toSparseBitSet(NatBitSet indices) {
        if (indices.isEmpty()) {
            return new SparseBitSet(1); // 0 is buggy here
        }
        if (indices instanceof SimpleNatBitSet) {
            return SparseBitSets.of(((SimpleNatBitSet) indices).getBitSet());
        }
        if (indices instanceof SimpleBoundedNatBitSet) {
            SimpleBoundedNatBitSet boundedSet = (SimpleBoundedNatBitSet) indices;
            if (boundedSet.isComplement()) {
                SparseBitSet bitSet = new SparseBitSet(boundedSet.domainSize());
                boundedSet.forEach((IntConsumer) bitSet::set);
                return bitSet;
            }
            return SparseBitSets.of(boundedSet.getBitSet());
        }
        if (indices instanceof SparseNatBitSet) {
            return ((SparseNatBitSet) indices).getBitSet().clone();
        }
        if (indices instanceof SparseBoundedNatBitSet) {
            SparseBoundedNatBitSet boundedSet = (SparseBoundedNatBitSet) indices;
            SparseBitSet bitSet = boundedSet.getBitSet().clone();
            if (boundedSet.isComplement()) {
                bitSet.flip(0, boundedSet.domainSize());
            }
            return bitSet;
        }

        SparseBitSet bitSet = new SparseBitSet(indices.lastInt() + 1);
        indices.forEach((IntConsumer) bitSet::set);
        return bitSet;
    }

    public static RoaringBitmap toRoaringBitmap(NatBitSet indices) {
        if (indices.isEmpty()) {
            return new RoaringBitmap();
        }
        if (indices instanceof RoaringNatBitSet) {
            return ((RoaringNatBitSet) indices).bitmap().clone();
        }
        if (indices instanceof RoaringBoundedNatBitSet) {
            RoaringBoundedNatBitSet boundedSet = (RoaringBoundedNatBitSet) indices;
            RoaringBitmap bitmap = boundedSet.bitmap().clone();
            if (boundedSet.isComplement()) {
                bitmap.flip(0L, boundedSet.domainSize());
            }
            return bitmap;
        }

        RoaringBitmap bitmap = new RoaringBitmap();
        indices.forEach((IntConsumer) bitmap::add);
        return bitmap;
    }

    // --- Factory ---

    /**
     * Sets the default factory.
     *
     * <strong>Warning:</strong> This method is only intended for local performance testing, e.g.,
     * whether using only sparse or simple bit sets is faster. This should not remain in production
     * code, since it could lead to conflicts.
     */
    public static void setFactory(NatBitSetFactory factory) {
        Objects.requireNonNull(factory);
        NatBitSets.factory = factory;
    }

    // Delegated Methods

    /**
     * Determines whether the given {@code set} can handle arbitrary (positive) modifications.
     */
    public static boolean isModifiable(NatBitSet set) {
        return factory.isModifiable(set);
    }

    /**
     * Determines whether the given {@code set} can handle arbitrary modifications within its domain.
     */
    public static boolean isModifiable(BoundedNatBitSet set) {
        return factory.isModifiable(set);
    }

    /**
     * Determines whether the given {@code set} can handle arbitrary modifications of values between 0
     * and {@code length - 1}.
     */
    public static boolean isModifiable(NatBitSet set, @Nonnegative int length) {
        return factory.isModifiable(set, length);
    }

    /**
     * Returns a modifiable set over the specified domain which contains the whole domain.
     */
    public static BoundedNatBitSet boundedFilledSet(int domainSize) {
        return factory.boundedFilledSet(domainSize);
    }

    public static BoundedNatBitSet boundedFilledSet(int domainSize, int expectedSize) {
        return factory.boundedFilledSet(domainSize, expectedSize);
    }

    public static BoundedNatBitSet boundedSet(int domainSize) {
        return factory.boundedSet(domainSize, UNKNOWN_SIZE);
    }

    public static BoundedNatBitSet boundedSet(int domainSize, int expectedSize) {
        return factory.boundedSet(domainSize, expectedSize);
    }

    /**
     * Try to compact the given set by potentially representing it as, e.g., empty or singleton set.
     * The returned set might not be modifiable.
     *
     * @param set
     *     The set to be compacted.
     *
     * @return a potentially compacted representation of the given set.
     */
    public static NatBitSet compact(NatBitSet set) {
        return factory.compact(set, false);
    }

    public static NatBitSet compact(NatBitSet set, boolean forceCopy) {
        return factory.compact(set, forceCopy);
    }

    /**
     * Copies the given indices. The returned set might not be modifiable.
     *
     * @param indices
     *     The indices to be copied.
     *
     * @return a copy of the given indices.
     *
     * @see #modifiableCopyOf(NatBitSet)
     */
    public static NatBitSet copyOf(Collection<Integer> indices) {
        return factory.copyOf(indices);
    }

    /**
     * Ensures that the given {@code set} is a {@link BoundedNatBitSet}, copying it if necessary.
     * Note that this also clones the set if, e.g., it is a bounded set with a larger domain.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code set} contains an index larger than {@code domainSize}.
     */
    public static BoundedNatBitSet ensureBounded(NatBitSet set, @Nonnegative int domainSize) {
        return factory.ensureBounded(set, domainSize);
    }

    /**
     * Ensures that the given {@code set} can be modified with arbitrary values. If necessary, the set
     * is copied into a general purpose representation.
     *
     * @see #isModifiable(NatBitSet)
     */
    public static NatBitSet ensureModifiable(NatBitSet set) {
        return factory.ensureModifiable(set);
    }

    /**
     * Ensures that the given {@code set} can be modified with arbitrary values from
     * {@code {0, ..., n}}. If necessary, the set is copied into a general purpose representation.
     *
     * @see #isModifiable(NatBitSet, int)
     */
    public static NatBitSet ensureModifiable(NatBitSet set, @Nonnegative int length) {
        return factory.ensureModifiable(set, length);
    }

    /**
     * Ensures that the given {@code set} can be modified in its domain. If necessary, the set is
     * copied into a general purpose representation.
     */
    public static NatBitSet ensureModifiable(BoundedNatBitSet set) {
        return factory.ensureModifiable(set);
    }

    /**
     * Returns a copy of the given {@code set} which is guaranteed to be modifiable.
     *
     * @see #isModifiable(NatBitSet)
     */
    public static NatBitSet modifiableCopyOf(NatBitSet set) {
        return factory.modifiableCopyOf(set);
    }

    /**
     * Returns a copy of the given {@code set} which is guaranteed to be modifiable up to
     * {@code length - 1}.
     *
     * @see #isModifiable(NatBitSet, int)
     */
    public static NatBitSet modifiableCopyOf(NatBitSet set, @Nonnegative int length) {
        return factory.modifiableCopyOf(set, length);
    }

    /**
     * Returns a copy of the given {@code set} which is guaranteed to be modifiable (within the
     * domain).
     *
     * @see #isModifiable(BoundedNatBitSet)
     */
    public static BoundedNatBitSet modifiableCopyOf(BoundedNatBitSet set) {
        return factory.modifiableCopyOf(set);
    }

    /**
     * Creates a modifiable set with the expected size and length.
     */
    public static NatBitSet set(int expectedSize, int expectedLength) {
        return factory.set(expectedSize, expectedLength);
    }

    /**
     * Creates a modifiable set.
     */
    public static NatBitSet set() {
        return factory.set();
    }

    /**
     * Creates a modifiable set with expected length.
     */
    public static NatBitSet setWithExpectedLength(@Nonnegative int expectedLength) {
        return factory.setWithExpectedLength(expectedLength);
    }

    /**
     * Creates a modifiable set with expected size.
     */
    public static NatBitSet setWithExpectedSize(@Nonnegative int expectedSize) {
        return factory.setWithExpectedSize(expectedSize);
    }

    /**
     * Creates a modifiable set which is modifiable up to {@code maximalLength}.
     */
    public static NatBitSet setWithMaximalLength(int maximalLength) {
        return factory.setWithExpectedSize(maximalLength);
    }
}
