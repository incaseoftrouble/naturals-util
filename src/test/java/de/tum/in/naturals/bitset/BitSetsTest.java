// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.bitset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BitSetsTest {
    @Test
    void testPowerBitSetEmpty() {
        Set<BitSet> powerSet = BitSets.powerSet(new BitSet(0));
        Set<BitSet> powerSetSimple = BitSets.powerSet(0);

        assertThat(powerSetSimple, is(powerSet));
        assertThat(powerSet, is(powerSetSimple));

        assertThat(powerSet, contains(new BitSet()));
        assertThat(powerSetSimple, contains(new BitSet()));
    }

    @Test
    void testPowerBitSet() {
        BitSet base = new BitSet(4);
        base.set(0, 4);

        Set<BitSet> powerSet = BitSets.powerSet(base);
        Set<BitSet> powerSetSimple = BitSets.powerSet(4);

        assertThat(powerSetSimple, is(powerSet));
        assertThat(powerSet, is(powerSetSimple));

        int size = 1 << 4;
        assertThat(powerSet, iterableWithSize(size));

        BitSet test = new BitSet(4);
        for (int i = 0; i < size; i++) {
            test.clear();
            for (int j = 0; j < 4; j++) {
                test.set(j, (i & (1 << j)) != 0);
            }
            assertThat(powerSet, hasItem(test));
            assertThat(powerSetSimple, hasItem(test));
        }
    }

    private static BitSet bitSetOf(Consumer<BitSet> builder) {
        BitSet bitSet = new BitSet();
        builder.accept(bitSet);
        return bitSet;
    }

    /**
     * The run branch of {@link BitSets#isSubset} carries the position of the containing set's next gap from
     * run to run. These shapes place gaps around the sample boundary (32 runs within 1024 bits) so that a
     * carried position that is reused when it should have been recomputed shows up as a wrong answer.
     */
    static Stream<Arguments> gapShapes() {
        return Stream.of(
                Arguments.of("runs", bitSetOf(bits -> {
                    for (int i = 0; i < 100; i++) {
                        bits.set(20 * i, 20 * i + 16);
                    }
                })),
                Arguments.of("scattered", bitSetOf(bits -> {
                    for (int i = 0; i < 100; i++) {
                        bits.set(20 * i);
                    }
                })));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("gapShapes")
    void testIsSubsetFindsGapsBehindTheSample(String name, BitSet first) {
        assertThat(name, BitSets.isSubset(first, first), is(true));

        for (int i = first.nextSetBit(0); i >= 0; i = first.nextSetBit(i + 1)) {
            int dropped = i;
            BitSet second = bitSetOf(bits -> {
                bits.set(0, 2100);
                bits.clear(dropped);
            });
            assertThat(name + ": gap at " + dropped, BitSets.isSubset(first, second), is(false));
            assertThat(name + ": gap at " + dropped, BitSets.isSubset(second, first), is(false));
        }
    }

    /** The pairs {@link BitSets#isSubsetConsuming} answers from the extent alone, without reading the set. */
    static Stream<Arguments> extentDecidingPairs() {
        BitSet first = bitSetOf(bits -> {
            bits.set(10);
            bits.set(20);
        });
        return Stream.of(
                Arguments.of("largest element past everything the other has", first, bitSetOf(bits -> bits.set(10))),
                Arguments.of("everything below the other set", first, bitSetOf(bits -> bits.set(100, 200))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("extentDecidingPairs")
    void testIsSubsetConsumingLeavesTheSetAloneWhenTheExtentDecides(String name, BitSet first, BitSet other) {
        BitSet consumed = (BitSet) first.clone();
        assertThat(name, BitSets.isSubsetConsuming(consumed, other), is(false));
        assertThat(name, consumed, is(first));
    }

    /** The pairs the extent cannot answer, and what is left of the set once they have been read. */
    static Stream<Arguments> consumedRemainders() {
        BitSet first = bitSetOf(bits -> {
            bits.set(10);
            bits.set(20);
        });
        BitSet spanning = bitSetOf(bits -> {
            bits.set(10);
            bits.set(20);
            bits.set(30);
        });
        return Stream.of(
                // The smallest element missing is not one of the constant time questions
                Arguments.of(
                        "smallest element missing",
                        first,
                        bitSetOf(bits -> bits.set(15, 25)),
                        false,
                        bitSetOf(bits -> bits.set(10))),
                Arguments.of("nothing to read", new BitSet(), first, true, new BitSet()),
                // Both ends present, so only reading what lies between them answers this one
                Arguments.of(
                        "both ends present",
                        spanning,
                        bitSetOf(bits -> {
                            bits.set(0, 41);
                            bits.clear(20);
                        }),
                        false,
                        bitSetOf(bits -> bits.set(20))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consumedRemainders")
    void testIsSubsetConsumingReadsTheSetDownToTheRemainder(
            String name, BitSet first, BitSet other, boolean subset, BitSet remainder) {
        BitSet consumed = (BitSet) first.clone();
        assertThat(name, BitSets.isSubsetConsuming(consumed, other), is(subset));
        assertThat(name, consumed, is(remainder));
    }

    /**
     * Asking a run where the containing set's next gap is walks past the run - across a nearly complete set,
     * all the way to its end. Repeating that per run is quadratic, and used to make this pair take minutes.
     */
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testIsSubsetOfCompleteDomainStaysLinear() {
        int domainSize = 4_000_000;
        Random random = new Random(2026L);
        BitSet dense = new BitSet(domainSize);
        for (int i = 0; i < domainSize; i++) {
            if (random.nextInt(10) != 0) {
                dense.set(i);
            }
        }
        BitSet complete = new BitSet(domainSize);
        complete.set(0, domainSize);

        assertThat(BitSets.isSubset(dense, complete), is(true));
        assertThat(BitSets.isSubset(complete, dense), is(false));
    }

    /**
     * {@link BitSets#iterator} walks a run at a time until the runs it has seen say that is not paying, so
     * the shapes that matter are the ones that change character around that decision.
     */
    static Stream<Arguments> iteratorShapes() {
        return Stream.of(
                Arguments.of("empty", new BitSet()),
                Arguments.of("singleton", bitSetOf(bits -> bits.set(7))),
                Arguments.of("one long run", bitSetOf(bits -> bits.set(0, 5000))),
                Arguments.of("scattered", bitSetOf(bits -> {
                    for (int i = 0; i < 500; i++) {
                        bits.set(3 * i);
                    }
                })),
                // Long runs for longer than the sample, then nothing but single bits, and the other way round
                Arguments.of("runs then scattered", bitSetOf(bits -> {
                    for (int i = 0; i < 64; i++) {
                        bits.set(20 * i, 20 * i + 16);
                    }
                    for (int i = 0; i < 500; i++) {
                        bits.set(2000 + 3 * i);
                    }
                })),
                Arguments.of("scattered then runs", bitSetOf(bits -> {
                    for (int i = 0; i < 500; i++) {
                        bits.set(3 * i);
                    }
                    for (int i = 0; i < 64; i++) {
                        bits.set(2000 + 20 * i, 2000 + 20 * i + 16);
                    }
                })));
    }

    /** The elements of the bit set, in order, read the obvious way. */
    private static IntList elementsOf(BitSet bitSet) {
        IntList elements = new IntArrayList();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            elements.add(i);
        }
        return elements;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("iteratorShapes")
    void testIteratorVisitsTheSetInOrder(String name, BitSet bitSet) {
        IntList visited = new IntArrayList();
        BitSets.iterator(bitSet).forEachRemaining((IntConsumer) visited::add);

        assertThat(name, visited, is(elementsOf(bitSet)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("iteratorShapes")
    void testIteratorRemoveKeepsWhatIsLeftToVisit(String name, BitSet bitSet) {
        // Removing every third element must not disturb what the iterator has yet to visit
        BitSet consumed = (BitSet) bitSet.clone();
        IntList kept = new IntArrayList();
        IntIterator iterator = BitSets.iterator(consumed);
        int index = 0;
        while (iterator.hasNext()) {
            int value = iterator.nextInt();
            if (index % 3 == 0) {
                iterator.remove();
            } else {
                kept.add(value);
            }
            index += 1;
        }

        assertThat(name, index, is(elementsOf(bitSet).size()));
        assertThat(name, elementsOf(consumed), is(kept));
    }
}
