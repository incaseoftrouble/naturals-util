// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A bounded set rejects an operation exactly when it would have to write outside its domain. The two
 * domain sizes select the two implementations ({@code long} word and {@link java.util.BitSet}).
 */
class BoundedContractTest {
    private static final int[] DOMAIN_SIZES = {10, 500};

    private static NatBitSet beyond(int domainSize) {
        return NatBitSets.ofVar(domainSize + 5, domainSize + 6);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void addingBeyondTheDomainThrows(int domainSize) {
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).set(domainSize));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).add(domainSize));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).flip(domainSize));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).set(0, domainSize + 1));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).set(domainSize, true));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).or(beyond(domainSize)));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).xor(beyond(domainSize)));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).addAll(beyond(domainSize)));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void removingBeyondTheDomainIsAllowed(int domainSize) {
        BoundedNatBitSet set = NatBitSets.boundedSet(domainSize);
        set.set(0, domainSize);

        set.clear(domainSize + 1);
        set.clearFrom(domainSize + 1);
        set.set(domainSize + 1, false);
        set.clear(0, domainSize + 5);
        assertThat(set.isEmpty(), is(true));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void emptyRangesBeyondTheDomainAreAllowed(int domainSize) {
        BoundedNatBitSet set = NatBitSets.boundedSet(domainSize);
        set.set(domainSize + 1, domainSize + 1);
        set.flip(domainSize + 1, domainSize + 1);
        assertThat(set.isEmpty(), is(true));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void intersectingWithAnOversizedOperandIsAllowed(int domainSize) {
        NatBitSet mixed = NatBitSets.ofVar(1, 3, domainSize + 5);

        BoundedNatBitSet retained = NatBitSets.boundedSet(domainSize);
        retained.set(0, 5);
        retained.and(mixed);
        assertThat(retained, contains(1, 3));

        BoundedNatBitSet removed = NatBitSets.boundedSet(domainSize);
        removed.set(0, 5);
        removed.andNot(mixed);
        assertThat(removed, contains(0, 2, 4));

        BoundedNatBitSet complemented = NatBitSets.boundedSet(domainSize);
        complemented.orNot(mixed);
        assertThat(complemented.size(), is(domainSize - 2));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void readingBeyondTheDomainIsAllowed(int domainSize) {
        BoundedNatBitSet set = NatBitSets.boundedSet(domainSize);
        set.set(0, 5);

        assertThat(set.contains(domainSize + 1), is(false));
        assertThat(set.remove(domainSize + 1), is(false));
        assertThat(set.containsAll(beyond(domainSize)), is(false));
        assertThat(set.intersects(beyond(domainSize)), is(false));
        assertThat(set.nextPresentIndex(domainSize + 1), is(-1));
        assertThat(set.nextAbsentIndex(domainSize + 1), is(domainSize + 1));
    }

    /** A negative index is outside every domain, so it behaves exactly like one past the domain. */
    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void negativeIndicesBehaveLikeAnyOtherIndexOutsideTheDomain(int domainSize) {
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).set(-1));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).flip(-1));
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).set(-1, 1));

        BoundedNatBitSet set = NatBitSets.boundedSet(domainSize);
        set.set(0, 5);
        set.clear(-1);
        set.set(-1, false);
        assertThat(set.size(), is(5));
        set.clear(-3, 2);
        assertThat(set, contains(2, 3, 4));
        set.clearFrom(-1);
        assertThat(set.isEmpty(), is(true));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void unboundedSetsTreatNegativeIndicesTheSameWay(int domainSize) {
        NatBitSet set = NatBitSets.of();
        set.set(0, domainSize);

        assertThrows(IndexOutOfBoundsException.class, () -> set.set(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.flip(-1));

        set.clear(-1);
        assertThat(set.size(), is(domainSize));
        set.clear(-3, 2);
        assertThat(set.size(), is(domainSize - 2));
        set.clearFrom(-1);
        assertThat(set.isEmpty(), is(true));
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500})
    void invertedRangesThrowEvenWhenClearing(int domainSize) {
        assertThrows(
                IndexOutOfBoundsException.class,
                () -> NatBitSets.boundedSet(domainSize).clear(3, 1));
        assertThrows(
                IndexOutOfBoundsException.class, () -> NatBitSets.ofVar(1, 2, 3).clear(3, 1));
    }

    /** Guards against the domain sizes above drifting away from the two implementations they select. */
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void theTwoDomainSizesSelectDistinctImplementations(int index) {
        assertThat(
                NatBitSets.boundedSet(DOMAIN_SIZES[index]).getClass()
                        == NatBitSets.boundedSet(DOMAIN_SIZES[1 - index]).getClass(),
                is(false));
    }
}
