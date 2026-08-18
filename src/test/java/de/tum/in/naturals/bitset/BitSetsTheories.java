// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({"NewClassNamingConvention", "StaticCollection"})
class BitSetsTheories {
    private static final int COMPLEMENT_SIZE = 1000;
    private static final int MAXIMUM_ELEMENTS = 1 << 16;
    private static final int MAXIMUM_SIZE = 1000;
    private static final int NUMBER_OF_TESTS = 100;
    private static final Random generator = new Random(10L);

    /**
     * Random collections, plus the shapes the traversals in {@link BitSets} choose between - the boundaries
     * of that choice are a sample's run budget (32), the window it looks at (1024 bits) and the run length
     * it asks for (4), and each named shape sits on one of them.
     */
    private static final List<Named<IntCollection>> indices;

    static class Named<V> {
        final String name;
        final V value;

        public Named(String name, V value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static {
        List<Named<IntCollection>> generatedIndices = new ArrayList<>(NUMBER_OF_TESTS);
        for (int i = 0; i < NUMBER_OF_TESTS; i++) {
            int size = generator.nextInt(MAXIMUM_SIZE);
            IntCollection list = new IntArrayList(size);
            for (int j = 0; j < size; j++) {
                list.add(generator.nextInt(MAXIMUM_ELEMENTS));
            }
            generatedIndices.add(new Named<>(String.format("random_%03d", i), list));
        }

        generatedIndices.add(scenario("empty", bits -> {}));
        generatedIndices.add(scenario("singleton", bits -> bits.set(0)));
        generatedIndices.add(scenario("late singleton", bits -> bits.set(100_000)));
        generatedIndices.add(scenario("a few scattered", bits -> {
            for (int i = 0; i < 4; i++) {
                bits.set(i * 1000);
            }
        }));
        generatedIndices.add(scenario("one long run", bits -> bits.set(0, 5000)));
        generatedIndices.add(scenario("run beyond the window", bits -> bits.set(2000, 5000)));
        generatedIndices.add(scenario("every other bit", bits -> {
            for (int i = 0; i < 5000; i++) {
                bits.set(2 * i);
            }
        }));
        generatedIndices.add(scenario("exactly the run budget", bits -> {
            for (int i = 0; i < 32; i++) {
                bits.set(2 * i);
            }
        }));
        generatedIndices.add(scenario("run budget plus a tail", bits -> {
            for (int i = 0; i < 40; i++) {
                bits.set(2 * i);
            }
            bits.set(50_000, 60_000);
        }));
        generatedIndices.add(scenario("runs at the length threshold", bits -> {
            for (int i = 0; i < 64; i++) {
                bits.set(8 * i, 8 * i + 4);
            }
        }));
        generatedIndices.add(scenario("scattered head, long tail", bits -> {
            for (int i = 0; i < 100; i++) {
                bits.set(3 * i);
            }
            bits.set(10_000, 40_000);
        }));
        indices = Collections.unmodifiableList(generatedIndices);
    }

    private static Named<IntCollection> scenario(String name, Consumer<BitSet> builder) {
        BitSet bitSet = bitSetOf(builder);
        IntCollection ints = new IntArrayList(bitSet.cardinality());
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            ints.add(i);
        }
        return new Named<>(name, ints);
    }

    public static Stream<Named<IntCollection>> indices() {
        return indices.stream();
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testComplementIterator(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet bitSet = new BitSet();
        ints.forEach((IntConsumer) bitSet::set);

        IntSet result = new IntOpenHashSet(bitSet.cardinality());
        BitSets.complementIterator(bitSet, COMPLEMENT_SIZE).forEachRemaining((IntConsumer) result::add);

        IntSet expected = new IntOpenHashSet();
        for (int i = 0; i < COMPLEMENT_SIZE; i++) {
            if (!ints.contains(i)) {
                expected.add(i);
            }
        }

        assertThat(result, is(expected));
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testForEach(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet bitSet = new BitSet();
        ints.forEach((IntConsumer) bitSet::set);

        IntSet result = new IntOpenHashSet(bitSet.cardinality());
        BitSets.forEach(bitSet, result::add);

        IntSet expected = new IntOpenHashSet(ints);

        assertThat(result, is(expected));
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testImmutableBitSet(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet bitSet = new BitSet();
        ints.forEach((IntConsumer) bitSet::set);

        ImmutableBitSet immutableBitSet = ImmutableBitSet.copyOf(bitSet);
        bitSet.clear();

        IntSet result = new IntOpenHashSet(immutableBitSet.cardinality());
        BitSets.forEach(immutableBitSet, result::add);

        IntSet expected = new IntOpenHashSet(ints);

        assertThat(result, is(expected));
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testForEachVisitsEveryElementOnce(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet bitSet = new BitSet();
        ints.forEach((IntConsumer) bitSet::set);

        IntList visited = new IntArrayList();
        BitSets.forEach(bitSet, visited::add);

        IntList expected = new IntArrayList();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            expected.add(i);
        }
        assertThat(named.name, visited, is(expected));
    }

    private static BitSet bitSetOf(Consumer<BitSet> builder) {
        BitSet bitSet = new BitSet();
        builder.accept(bitSet);
        return bitSet;
    }

    private static boolean elementwiseSubset(BitSet first, BitSet second) {
        for (int i = first.nextSetBit(0); i >= 0; i = first.nextSetBit(i + 1)) {
            if (!second.get(i)) {
                return false;
            }
        }
        return true;
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testIsSubsetAgreesWithElementwiseCheck(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet set = new BitSet();
        ints.forEach((IntConsumer) set::set);

        for (BitSet other : subsetScenarios(set)) {
            assertThat(named.name, BitSets.isSubset(set, other), is(elementwiseSubset(set, other)));
            assertThat(named.name, BitSets.isSubset(other, set), is(elementwiseSubset(other, set)));
        }
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testIsSubsetConsumingAgreesWithIsSubset(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet set = new BitSet();
        ints.forEach((IntConsumer) set::set);

        for (BitSet other : subsetScenarios(set)) {
            BitSet consumed = (BitSet) set.clone();
            assertThat(named.name, BitSets.isSubsetConsuming(consumed, other), is(elementwiseSubset(set, other)));
            // Either the extent decided and the set was left alone, or it now holds the difference
            BitSet difference = (BitSet) set.clone();
            difference.andNot(other);
            assertThat(named.name, consumed, is(anyOf(is(set), is(difference))));
        }
    }

    /** The pairs a containment test has to get right: identical, disjoint, and off by one element. */
    private static List<BitSet> subsetScenarios(BitSet set) {
        List<BitSet> others = new ArrayList<>();
        others.add(set);
        others.add(new BitSet());
        others.add(bitSetOf(bits -> bits.set(0, MAXIMUM_ELEMENTS)));
        others.add(bitSetOf(bits -> {
            bits.or(set);
            bits.set(MAXIMUM_ELEMENTS + 1);
        }));
        if (!set.isEmpty()) {
            // Dropping one element each from the front, the middle and the back of the set
            int middle = set.nextSetBit(set.length() / 2);
            for (int dropped :
                    new int[] {set.nextSetBit(0), middle < 0 ? set.nextSetBit(0) : middle, set.length() - 1}) {
                others.add(bitSetOf(bits -> {
                    bits.or(set);
                    bits.clear(dropped);
                }));
            }
        }

        return others;
    }

    /** Maximal blocks of consecutive set bits, counted the obvious way. */
    private static int countRuns(BitSet bitSet) {
        int runs = 0;
        for (int from = bitSet.nextSetBit(0); from >= 0; from = bitSet.nextSetBit(bitSet.nextClearBit(from))) {
            runs += 1;
        }
        return runs;
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testHasAtMostRunsAgreesWithCountRuns(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet bitSet = new BitSet();
        ints.forEach((IntConsumer) bitSet::set);
        int runs = countRuns(bitSet);

        for (int limit : new int[] {-1, 0, 1, runs - 1, runs, runs + 1, Integer.MAX_VALUE}) {
            assertThat(
                    named.name + ": limit " + limit + " of " + runs + " runs",
                    BitSets.hasAtMostRuns(bitSet, limit),
                    is(0 <= limit && runs <= limit));
        }
    }

    @ParameterizedTest
    @MethodSource("indices")
    void testIterator(Named<IntCollection> named) {
        IntCollection ints = named.value;
        BitSet bitSet = new BitSet();
        ints.forEach((IntConsumer) bitSet::set);

        IntSet result = new IntOpenHashSet(bitSet.cardinality());
        BitSets.iterator(bitSet).forEachRemaining((IntConsumer) result::add);

        IntSet expected = new IntOpenHashSet(ints);

        assertThat(result, is(expected));
    }
}
