// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static de.tum.in.naturals.set.NatBitSetsUtil.*;

import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.RoaringBitmaps;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import javax.annotation.Nonnegative;
import org.roaringbitmap.RoaringBitmap;

public final class NatBitSets {
    public static final int UNKNOWN_LENGTH = -1;
    public static final int UNKNOWN_SIZE = -1;

    private NatBitSets() {}

    // --- Sets ---

    /** An empty, growable set. */
    public static NatBitSet of() {
        return new HybridNatBitSet();
    }

    public static NatBitSet of(int singleton) {
        checkNonNegative(singleton);
        return new HybridNatBitSet(new int[] {singleton}, 1);
    }

    public static NatBitSet of(int[] elements) {
        if (elements.length == 0) {
            return of();
        }
        if (elements.length == 1) {
            return of(elements[0]);
        }
        // The elements are all in hand, so their shape is one pass away - cheaper than discovering it by
        // climbing the growth ladder, which re-decides the representation on the way up
        int last = 0;
        for (int element : elements) {
            checkNonNegative(element);
            if (element > last) {
                last = element;
            }
        }
        NatBitSet set = HybridNatBitSet.forShape(elements.length, last);
        for (int element : elements) {
            set.set(element);
        }
        return set;
    }

    public static NatBitSet ofVar(int... elements) {
        return of(elements);
    }

    public static NatBitSet filled(@Nonnegative int from, int to) {
        NatBitSet set = new HybridNatBitSet();
        set.set(from, to);
        return set;
    }

    /**
     * Copies the given indices.
     *
     * @param indices
     *     The indices to be copied.
     *
     * @return a copy of the given indices.
     */
    public static NatBitSet copyOf(Collection<Integer> indices) {
        // A collection that already keeps its members in a shape we can adopt is copied store and all,
        // rather than element by element through the growth ladder.
        if (indices instanceof HybridNatBitSet) {
            return new HybridNatBitSet((HybridNatBitSet) indices);
        }
        BitSet words = words(indices);
        if (words != null) {
            return new HybridNatBitSet((BitSet) words.clone());
        }
        RoaringBitmap bitmap = bitmap(indices);
        if (bitmap != null) {
            return new HybridNatBitSet(bitmap.clone());
        }

        NatBitSet copy = withExpectedCardinality(indices.size());
        if (indices instanceof IntCollection) {
            copy.or((IntCollection) indices);
        } else {
            indices.forEach(copy::set);
        }
        return copy;
    }

    /**
     * Converts the given set, copying if necessary.
     *
     * @param indices
     *     The indices to be copied.
     *
     * @return a copy of the given indices.
     */
    public static NatBitSet castOrCopy(Collection<Integer> indices) {
        return indices instanceof NatBitSet ? (NatBitSet) indices : copyOf(indices);
    }

    /**
     * A set which is expected to hold roughly {@code expectedSize} elements.
     *
     * <p>Cardinality alone does not determine the shape - how far the elements reach matters just as
     * much - so this assumes they will be about twice as far apart as they are numerous. That is a
     * dense set, and it will be given words. Callers that know the span should say so through
     * {@link #withExpectedShape(int, int)} instead of relying on that guess.</p>
     */
    public static NatBitSet withExpectedCardinality(@Nonnegative int expectedSize) {
        return withExpectedShape(expectedSize, (int) Math.min(Integer.MAX_VALUE, 2L * expectedSize));
    }

    /**
     * A set which is expected to hold roughly {@code expectedSize} elements, none of them larger than
     * roughly {@code expectedLast}. Both are hints: exceeding either costs a growth step, not
     * correctness.
     */
    public static NatBitSet withExpectedShape(@Nonnegative int expectedSize, @Nonnegative int expectedLast) {
        checkNonNegative(expectedSize);
        checkNonNegative(expectedLast);
        return HybridNatBitSet.forShape(expectedSize, expectedLast);
    }

    /**
     * Returns a set backed by the given bitmap. Ownership passes to the returned set, the bitmap must not be
     * used afterwards.
     */
    public static NatBitSet wrap(RoaringBitmap bitmap) {
        return new HybridNatBitSet(bitmap);
    }

    /**
     * Returns a set backed by the given bitSet. Ownership passes to the returned set, the bitSet must not be
     * used afterwards.
     */
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public static NatBitSet wrap(BitSet bitSet) {
        return new HybridNatBitSet(bitSet);
    }

    /**
     * Returns a set backed by the given array. Ownership passes to the returned set, the array must not be
     * used afterwards. The array must be sorted.
     */
    public static NatBitSet wrap(int[] elements) {
        return new HybridNatBitSet(elements, elements.length);
    }

    // --- Bounded Sets ---

    public static BoundedNatBitSet boundedSet(int domainSize) {
        return domainSize <= LongBoundedNatBitSet.maximalSize()
                ? new LongBoundedNatBitSet(domainSize)
                : new SimpleBoundedNatBitSet(new BitSet(domainSize), domainSize);
    }

    public static BoundedNatBitSet boundedSingleton(int domainSize, int element) {
        BoundedNatBitSet set = boundedSet(domainSize);
        set.set(element);
        return set;
    }

    public static BoundedNatBitSet boundedLongSet(int domainSize) {
        return new LongBoundedNatBitSet(domainSize);
    }

    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public static BoundedNatBitSet boundedSimpleSet(int domainSize) {
        return new SimpleBoundedNatBitSet(new BitSet(), domainSize);
    }

    /**
     * Return a view on the given {@code bitSet}.
     */
    public static BoundedNatBitSet asBoundedSet(BitSet bitSet, int domainSize) {
        return new SimpleBoundedNatBitSet(bitSet, domainSize);
    }

    /**
     * Ensures that the given {@code set} is a {@link BoundedNatBitSet}. When possible, the backing
     * data structure is shallow copied. For example, when passing a word backed set, a
     * {@link SimpleBoundedNatBitSet} with the same backing bit set will be returned; otherwise the
     * contents are copied. Note that after this operation, only the returned set should be used.
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
        checkDomainSize(domainSize);
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
        BitSet words = words(set);
        if (words != null) {
            return new SimpleBoundedNatBitSet(words, domainSize);
        }

        BoundedNatBitSet copy = boundedSet(domainSize);
        copy.or(set);
        return copy;
    }

    /**
     * Ensures that the given {@code set} is a {@link BoundedNatBitSet}, copying it if necessary.
     * Note that this also clones the set if, e.g., it is a bounded set with a larger domain.
     *
     * @throws IndexOutOfBoundsException
     *     if {@code set} contains an index larger than {@code domainSize}.
     */
    public static BoundedNatBitSet ensureBounded(NatBitSet set, @Nonnegative int domainSize) {
        checkDomainSize(domainSize);
        if (!set.isEmpty() && set.lastInt() >= domainSize) {
            throw new IndexOutOfBoundsException();
        }
        if (set instanceof BoundedNatBitSet) {
            BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;
            int oldDomainSize = boundedSet.domainSize();
            if (oldDomainSize == domainSize) {
                return boundedSet;
            }
            if (set instanceof SimpleBoundedNatBitSet) {
                BitSet bitSetCopy =
                        (BitSet) ((SimpleBoundedNatBitSet) set).getBitSet().clone();
                if (domainSize < oldDomainSize) {
                    bitSetCopy.clear(domainSize, oldDomainSize);
                }
                return new SimpleBoundedNatBitSet(bitSetCopy, domainSize);
            }
        }
        BoundedNatBitSet copy = boundedSet(domainSize);
        copy.or(set);
        return copy;
    }

    /**
     * An empty domain is a domain: only a negative one is an error. Both bounded factories go through
     * here so that they agree on where that line is.
     */
    private static void checkDomainSize(int domainSize) {
        if (domainSize < 0) {
            throw new IllegalArgumentException(String.format("Negative domain size %d", domainSize));
        }
    }

    // --- Iterators ---

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
        return new NatBitSetComplementIterator(set, length);
    }

    public static IntIterator complementReverseIterator(NatBitSet set, @Nonnegative int length) {
        if (set.isEmpty() || set.firstInt() >= length) {
            return new ReverseRangeIterator(0, length);
        }
        return new NatBitSetComplementReverseIterator(set, length);
    }

    // --- Extraction ---

    public static BitSet toBitSet(NatBitSet indices) {
        if (indices.isEmpty()) {
            return new BitSet(0);
        }
        BitSet words = words(indices);
        if (words != null) {
            return (BitSet) words.clone();
        }
        RoaringBitmap bitmap = bitmap(indices);
        if (bitmap != null) {
            return BitSets.of(bitmap, indices.lastInt() + 1);
        }
        BitSet bitSet = new BitSet(indices.lastInt() + 1);
        indices.forEach((IntConsumer) bitSet::set);
        return bitSet;
    }

    public static RoaringBitmap toRoaringBitmap(NatBitSet indices) {
        if (indices.isEmpty()) {
            return new RoaringBitmap();
        }
        RoaringBitmap bitmap = bitmap(indices);
        if (bitmap != null) {
            return bitmap.clone();
        }
        BitSet words = words(indices);
        if (words != null) {
            return RoaringBitmaps.of(words);
        }
        RoaringBitmap copy = new RoaringBitmap();
        indices.forEach((IntConsumer) copy::add);
        return copy;
    }

    // --- Utilities ---

    /**
     * Returns the set containing all subsets of the given basis.
     * <strong>Warning</strong>: For performance reasons, the iterator of this set may modify the
     * returned elements in place.
     */
    public static Set<NatBitSet> powerSet(NatBitSet basis) {
        return basis.isEmpty() ? Collections.singleton(of()) : new PowerNatBitSet(basis);
    }

    public static boolean intersects(Set<Integer> one, Set<Integer> other) {
        if (one == other) { // NOPMD
            return !one.isEmpty();
        }
        if (one instanceof NatBitSet) {
            return ((NatBitSet) one).intersects(other);
        }
        if (other instanceof NatBitSet) {
            return ((NatBitSet) other).intersects(one);
        }
        if (one instanceof IntSet && other instanceof IntSet) {
            boolean oneSmaller = one.size() < other.size();
            IntSet iterate = (IntSet) (oneSmaller ? one : other);
            IntSet contains = (IntSet) (oneSmaller ? other : one);
            IntIterator iterator = iterate.intIterator();
            while (iterator.hasNext()) {
                if (contains.contains(iterator.nextInt())) {
                    return true;
                }
            }
            return false;
        }
        return !Collections.disjoint(one, other);
    }

    public static IntSet lazyUnion(Collection<? extends IntSet> sets) {
        return sets.size() == 1 ? sets.iterator().next() : union(sets);
    }

    public static <S> IntSet lazyUnion(Collection<S> sets, Function<S, IntSet> map) {
        if (sets.size() == 1) {
            return map.apply(sets.iterator().next());
        }
        return union(sets, map);
    }

    public static <S> IntSet lazyFilteredUnion(
            Collection<S> items, Predicate<? super S> filter, Function<? super S, ? extends IntSet> map) {
        IntSet candidate = null;
        NatBitSet union = null;
        for (S item : items) {
            if (!filter.test(item)) {
                continue;
            }
            if (candidate == null) {
                candidate = map.apply(item);
            } else {
                if (union == null) {
                    union = copyOf(candidate);
                }
                union.or(map.apply(item));
            }
        }
        if (candidate == null) {
            return IntSet.of();
        }
        return union == null ? candidate : union;
    }

    public static <S> NatBitSet union(Collection<S> sets, Function<S, IntSet> map) {
        List<IntCollection> operands = new ArrayList<>(sets.size());
        for (S set : sets) {
            operands.add(map.apply(set));
        }
        return union(operands);
    }

    /**
     * Union of several sets at once. Cheaper than folding {@code or} over them: the result shape is bounded
     * up front, so the target is allocated once in the representation that shape calls for.
     */
    public static NatBitSet union(Collection<? extends IntCollection> sets) {
        return HybridNatBitSet.union(sets);
    }

    public static NatBitSet lazyIntersection(NatBitSet one, NatBitSet other) {
        if (one.containsAll(other)) {
            return other;
        }
        if (other.containsAll(one)) {
            return one;
        }
        NatBitSet copy = copyOf(one);
        copy.retainAll(other);
        return copy;
    }
}
