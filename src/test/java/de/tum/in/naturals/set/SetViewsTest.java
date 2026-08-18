// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;

/** The power-set, cartesian-product and complement views, none of which had coverage. */
class SetViewsTest {
    private static IntList drain(IntIterator iterator) {
        IntList list = new IntArrayList();
        iterator.forEachRemaining((IntConsumer) list::add);
        return list;
    }

    @Test
    void cartesianProductContainsItsOwnElements() {
        NatCartesianProductSet product = new NatCartesianProductSet(new int[] {2, 2});
        assertThat(product.size64(), is(9L));

        for (int[] element : product) {
            assertThat(product.contains(element.clone()), is(true));
        }
        assertThat(product.contains(new int[] {3, 0}), is(false));
        assertThat(product.contains(new int[] {-1, 0}), is(false));
        assertThat(product.contains(new int[] {0}), is(false));
    }

    @Test
    void powerSetIteratorYieldsEverySubsetOnce() {
        PowerSetIterator iterator = new PowerSetIterator(new boolean[] {true, false, true});
        Set<Integer> indices = new HashSet<>();
        while (iterator.hasNext()) {
            iterator.next();
            indices.add(iterator.currentIndex());
        }
        assertThat(indices, is(Set.of(0, 1, 4, 5)));
    }

    @Test
    void powerSetIteratorRejectsIndicesItCannotRepresent() {
        boolean[] base = new boolean[40];
        base[0] = true;
        base[39] = true;
        PowerSetIterator iterator = new PowerSetIterator(base);
        iterator.next();
        assertThrows(IllegalStateException.class, iterator::currentIndex);
        assertThat(iterator.currentIndexLong(), is(0L));
    }

    @SuppressWarnings({"SuspiciousMethodCalls", "CollectionIncompatibleType"})
    @Test
    void powerSetContainsBoxedSubsets() {
        Set<NatBitSet> powerSet = NatBitSets.powerSet(NatBitSets.ofVar(0, 1, 2));
        assertThat(powerSet.contains(NatBitSets.of(1)), is(true));

        // Deliberately foreign element types - the point is that they are answered, not rejected
        Set<Integer> boxedSubset = new HashSet<>(List.of(1));
        Set<Integer> boxedNonSubset = new HashSet<>(List.of(9));
        Set<String> wrongElementType = new HashSet<>(List.of("nope"));
        assertThat(powerSet.contains(boxedSubset), is(true));
        assertThat(powerSet.contains(boxedNonSubset), is(false));
        assertThat(powerSet.contains(wrongElementType), is(false));
    }

    @Test
    void complementIteratorsCoverTheDomain() {
        NatBitSet set = NatBitSets.ofVar(1, 3);
        assertThat(drain(NatBitSets.complementIterator(set, 6)), is(IntList.of(0, 2, 4, 5)));
        assertThat(drain(NatBitSets.complementReverseIterator(set, 6)), is(IntList.of(5, 4, 2, 0)));

        assertThat(drain(NatBitSets.complementReverseIterator(NatBitSets.of(0), 1)), is(IntList.of()));
        assertThat(drain(NatBitSets.complementReverseIterator(NatBitSets.of(1), 3)), is(IntList.of(2, 0)));
        assertThat(drain(NatBitSets.complementIterator(NatBitSets.of(), 3)), is(IntList.of(0, 1, 2)));
    }

    @Test
    void complementIteratorsAreUnmodifiable() {
        NatBitSet set = NatBitSets.ofVar(1, 3);
        IntIterator forward = NatBitSets.complementIterator(set, 6);
        forward.nextInt();
        assertThrows(UnsupportedOperationException.class, forward::remove);

        IntIterator backward = NatBitSets.complementReverseIterator(set, 6);
        backward.nextInt();
        assertThrows(UnsupportedOperationException.class, backward::remove);
    }

    @Test
    void reverseIteratorSupportsRemoval() {
        NatBitSet set = NatBitSets.ofVar(0, 5, 9);
        IntIterator iterator = set.reverseIterator();
        assertThat(iterator.nextInt(), is(9));
        iterator.remove();
        assertThat(iterator.nextInt(), is(5));
        assertThat(iterator.nextInt(), is(0));
        iterator.remove();
        assertThat(set, contains(5));

        NatBitSet singleton = NatBitSets.of(7);
        IntIterator single = singleton.reverseIterator();
        single.nextInt();
        single.remove();
        assertThat(singleton.isEmpty(), is(true));
    }
}
