// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

class NatBitSetTest {
    private static BoundedNatBitSet boundedFullSet(int domainSize) {
        BoundedNatBitSet set = NatBitSets.boundedSet(domainSize);
        set.set(0, domainSize);
        return set;
    }

    @Test
    void testCreateBoundedLongSet() {
        BoundedNatBitSet ints = NatBitSets.boundedLongSet(10);
        assertThat(ints.domainSize(), is(10));
        ints.set(0, 10);
        assertThat(ints.size(), is(10));
    }

    @Test
    void testCreateBoundedSet() {
        BoundedNatBitSet ints = NatBitSets.boundedSet(10);
        assertThat(ints.domainSize(), is(10));
        ints.set(0, 10);
        assertThat(ints.size(), is(10));
    }

    @Test
    void testCreateFullSet() {
        BoundedNatBitSet ints = boundedFullSet(500);
        assertThat(ints.domainSize(), is(500));

        assertThat(ints.firstInt(), is(0));
        assertThat(ints.lastInt(), is(499));

        assertThat(NatBitSets.boundedSet(500).isEmpty(), is(true));
    }

    @Test
    void testOfIsEmptyAndGrowable() {
        NatBitSet ints = NatBitSets.of();
        assertThat(ints.isEmpty(), is(true));
        ints.set(0, 10);
        assertThat(ints.size(), is(10));
        ints.set(1_000_000);
        assertThat(ints.size(), is(11));
        assertThat(ints.lastInt(), is(1_000_000));
    }

    @Test
    void testOfElements() {
        NatBitSet ints = NatBitSets.ofVar(3, 1, 2, 1);
        assertThat(ints.size(), is(3));
        assertThat(ints, contains(1, 2, 3));
    }

    @Test
    void testCastOrCopyReturnsTheArgumentForNatBitSets() {
        NatBitSet ints = NatBitSets.ofVar(1, 2, 3);
        assertThat(NatBitSets.castOrCopy(ints), sameInstance(ints));
    }

    @Test
    void testCopyOfIsIndependent() {
        NatBitSet ints = NatBitSets.ofVar(1, 2, 3);
        NatBitSet copy = NatBitSets.copyOf(ints);
        assertThat(copy, is(ints));
        assertThat(copy, not(sameInstance(ints)));

        copy.set(4);
        assertThat(ints.contains(4), is(false));
    }

    @Test
    void testCopyOfBoxedCollection() {
        NatBitSet ints = NatBitSets.copyOf(List.of(5, 3, 5, 900));
        assertThat(ints.size(), is(3));
        assertThat(ints, contains(3, 5, 900));
    }

    @Test
    void testAsSetAdoptsTheStore() {
        BitSet bitSet = new BitSet();
        bitSet.set(1, 5);
        NatBitSet ints = NatBitSets.wrap(bitSet);
        assertThat(ints.size(), is(4));

        NatBitSet bitmapBacked = NatBitSets.wrap(RoaringBitmap.bitmapOf(1, 2, 3, 4));
        assertThat(bitmapBacked, is(ints));
    }

    @Test
    void testWithExpectedSize() {
        NatBitSet ints = NatBitSets.withExpectedCardinality(4);
        assertThat(ints.isEmpty(), is(true));
        ints.set(0, 100);
        assertThat(ints.size(), is(100));
    }

    @Test
    void testFilled() {
        assertThat(NatBitSets.filled(0, 0).isEmpty(), is(true));
        assertThat(NatBitSets.filled(3, 4), contains(3));

        NatBitSet range = NatBitSets.filled(10, 1_000_000);
        assertThat(range.size(), is(999_990));
        assertThat(range.firstInt(), is(10));
        assertThat(range.lastInt(), is(999_999));
        assertThat(range.contains(9), is(false));
        assertThat(range.contains(10), is(true));

        assertThrows(IndexOutOfBoundsException.class, () -> NatBitSets.filled(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> NatBitSets.filled(5, 4));
    }
}
