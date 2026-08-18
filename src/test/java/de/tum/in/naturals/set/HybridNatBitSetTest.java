// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

/**
 * Targets the points where {@link HybridNatBitSet} changes its backing representation: the array cap at
 * {@code MAXIMAL_ARRAY_SIZE} elements, and the footprint comparison that decides between words and Roaring.
 * The constants here are chosen to sit immediately either side of those.
 */
class HybridNatBitSetTest {
    private static BoundedNatBitSet boundedFullSet(int domainSize) {
        BoundedNatBitSet set = NatBitSets.boundedSet(domainSize);
        set.set(0, domainSize);
        return set;
    }

    private static HybridNatBitSet arrayMode(int... elements) {
        HybridNatBitSet set = new HybridNatBitSet();
        for (int element : elements) {
            set.set(element);
        }
        assertThat(set.store(), instanceOf(int[].class));
        return set;
    }

    private static HybridNatBitSet bitSetMode(int... elements) {
        BitSet bitSet = new BitSet();
        for (int element : elements) {
            bitSet.set(element);
        }
        return new HybridNatBitSet(bitSet);
    }

    private static HybridNatBitSet roaringMode(int... elements) {
        return new HybridNatBitSet(RoaringBitmap.bitmapOf(elements));
    }

    private static IntSortedSet reference(int... elements) {
        IntSortedSet set = new IntAVLTreeSet();
        for (int element : elements) {
            set.add(element);
        }
        return set;
    }

    private static int[] range(int from, int to) {
        int[] elements = new int[to - from];
        for (int i = from; i < to; i++) {
            elements[i - from] = i;
        }
        return elements;
    }

    @Test
    void arrayStaysSortedUnderOutOfOrderInsertion() {
        HybridNatBitSet set = arrayMode(500, 3, 900, 1, 700);
        assertThat(set, is(reference(1, 3, 500, 700, 900)));
        assertThat(set.firstInt(), is(1));
        assertThat(set.lastInt(), is(900));
    }

    @Test
    void duplicateInsertionDoesNotGrow() {
        HybridNatBitSet set = arrayMode(1000, 2000, 3000);
        for (int i = 0; i < 50; i++) {
            set.set(2000);
        }
        assertThat(set.size(), is(3));
        assertThat(set.store(), instanceOf(int[].class));
    }

    // Array -> words / Roaring

    @Test
    void bitSetPromotesToRoaringOnDistantElement() {
        HybridNatBitSet set = bitSetMode(range(0, 17));
        set.set(100_000);
        assertThat(set.store(), instanceOf(RoaringBitmap.class));
        assertThat(set.size(), is(18));
        assertThat(set.contains(100_000), is(true));
        assertThat(set.contains(5), is(true));
    }

    // Ranges crossing a transition

    @Test
    void rangeSetCrossesTransition() {
        HybridNatBitSet set = arrayMode(1, 2, 3);
        set.set(0, 100);
        assertThat(set.store(), instanceOf(BitSet.class));
        assertThat(set.size(), is(100));
        assertThat(set, is(reference(range(0, 100))));
    }

    @Test
    void rangeSetFarAwayPromotesToRoaring() {
        HybridNatBitSet set = arrayMode(1, 2, 3);
        set.set(1_000_000, 1_000_100);
        assertThat(set.store(), instanceOf(RoaringBitmap.class));
        assertThat(set.size(), is(103));
    }

    @Test
    void rangeFlipCrossesTransition() {
        HybridNatBitSet set = arrayMode(1, 2, 3);
        set.flip(0, 100);
        IntSortedSet expected = reference(range(0, 100));
        expected.removeAll(reference(1, 2, 3));
        assertThat(set, is(expected));
        assertThat(set.size(), is(97));
    }

    @Test
    void clearRangeInArrayMode() {
        HybridNatBitSet set = arrayMode(100, 200, 300, 400, 500);
        set.clear(150, 450);
        assertThat(set, is(reference(100, 500)));
        assertThat(set.size(), is(2));
    }

    @Test
    void clearFromInArrayMode() {
        HybridNatBitSet set = arrayMode(100, 200, 300);
        set.clearFrom(200);
        assertThat(set, is(reference(100)));
    }

    // Lazily cached size

    @Test
    void sizeIsCorrectAfterEveryTransition() {
        HybridNatBitSet set = new HybridNatBitSet();
        IntSortedSet expected = new IntAVLTreeSet();
        for (int i = 0; i < 40; i++) {
            int value = i * 37;
            set.set(value);
            expected.add(value);
            assertThat(set.size(), is(expected.size()));
        }
        set.or(reference(1_000_000, 2_000_000));
        expected.addAll(reference(1_000_000, 2_000_000));
        assertThat(set.size(), is(expected.size()));

        set.andNot(reference(0, 37, 74));
        expected.removeAll(reference(0, 37, 74));
        assertThat(set.size(), is(expected.size()));
        assertThat(set, is(expected));
    }

    // optimize()

    @Test
    void optimizeKeepsRoaringForAContiguousRange() {
        HybridNatBitSet set = new HybridNatBitSet(new RoaringBitmap());
        for (int i = 0; i < 1000; i++) {
            set.set(i);
        }
        set.optimize();
        assertThat(set.store(), instanceOf(RoaringBitmap.class));
        assertThat(set.size(), is(1000));
        assertThat(set, is(reference(range(0, 1000))));
    }

    @Test
    void optimizeDemotesRoaringToBitSet() {
        HybridNatBitSet set = new HybridNatBitSet(new RoaringBitmap());
        int[] elements = new int[1000];
        for (int i = 0; i < 1000; i++) {
            elements[i] = 2 * i;
            set.set(2 * i);
        }
        assertThat(set.store(), instanceOf(RoaringBitmap.class));

        set.optimize();
        assertThat(set.store(), instanceOf(BitSet.class));
        assertThat(set.size(), is(1000));
        assertThat(set, is(reference(elements)));
    }

    @Test
    void optimizePrefersRunsForANearCompleteRange() {
        BitSet bitSet = new BitSet();
        bitSet.set(0, 1_000_000);
        bitSet.clear(500_000);
        HybridNatBitSet set = new HybridNatBitSet(bitSet);

        set.optimize();
        assertThat(set.store(), instanceOf(RoaringBitmap.class));
        assertThat(set.size(), is(999_999));
        assertThat(set.contains(500_000), is(false));
        assertThat(set.contains(499_999), is(true));
    }

    @Test
    void optimizeDemotesRoaringToArray() {
        HybridNatBitSet set = roaringMode(0, 1000, 2000, 3000, 1_000_000);
        set.optimize();
        assertThat(set.store(), instanceOf(int[].class));
        assertThat(set, is(reference(0, 1000, 2000, 3000, 1_000_000)));
    }

    @Test
    void optimizePromotesBitSetToRoaring() {
        BitSet bitSet = new BitSet();
        for (int i = 0; i < 100; i++) {
            bitSet.set(i * 50_000);
        }
        HybridNatBitSet set = new HybridNatBitSet(bitSet);
        set.optimize();
        assertThat(set.store(), instanceOf(RoaringBitmap.class));
        assertThat(set.size(), is(100));
    }

    @Test
    void optimizeOfEmptySetResetsToArray() {
        HybridNatBitSet set = new HybridNatBitSet(new RoaringBitmap());
        set.set(5);
        set.clear(5);
        set.optimize();
        assertThat(set.store(), instanceOf(int[].class));
        assertThat(set.isEmpty(), is(true));
    }

    // Navigation across modes

    @Test
    void navigationAgreesAcrossModes() {
        int[] elements = {0, 1, 2, 17, 18, 500, 1000};
        HybridNatBitSet[] sets = {arrayMode(elements), bitSetMode(elements), roaringMode(elements)};
        for (HybridNatBitSet set : sets) {
            assertThat(set.firstInt(), is(0));
            assertThat(set.lastInt(), is(1000));
            assertThat(set.nextPresentIndex(3), is(17));
            assertThat(set.nextPresentIndex(1001), is(-1));
            assertThat(set.nextAbsentIndex(0), is(3));
            assertThat(set.nextAbsentIndex(17), is(19));
            assertThat(set.previousPresentIndex(16), is(2));
            assertThat(set.previousPresentIndex(1_000_000), is(1000));
            assertThat(set.previousAbsentIndex(2), is(-1));
            assertThat(set.previousAbsentIndex(18), is(16));
        }
    }

    // Iteration

    @Test
    void iteratorRemoveInArrayMode() {
        HybridNatBitSet set = arrayMode(100, 200, 300, 400);
        IntIterator iterator = set.iterator();
        while (iterator.hasNext()) {
            if (iterator.nextInt() % 200 == 0) {
                iterator.remove();
            }
        }
        assertThat(set, is(reference(100, 300)));
        assertThat(set.size(), is(2));
    }

    @Test
    void iteratorRemoveInBitSetMode() {
        HybridNatBitSet set = bitSetMode(range(0, 20));
        IntIterator iterator = set.iterator();
        while (iterator.hasNext()) {
            if (iterator.nextInt() % 2 == 0) {
                iterator.remove();
            }
        }
        assertThat(set.size(), is(10));
        assertThat(set.contains(0), is(false));
        assertThat(set.contains(1), is(true));
    }

    // Clone and equality

    @Test
    void cloneKeepsModeAndIsIndependent() {
        int[] elements = {1, 2, 3, 1_000_000};
        for (HybridNatBitSet set :
                new HybridNatBitSet[] {arrayMode(elements), bitSetMode(elements), roaringMode(elements)}) {
            HybridNatBitSet clone = set.clone();
            assertThat(clone, is(set));

            clone.set(7);
            assertThat(set.contains(7), is(false));
            assertThat(clone.contains(7), is(true));
        }
    }

    @Test
    void equalsHoldsAcrossModes() {
        int[] elements = {1, 2, 3, 1_000_000};
        HybridNatBitSet array = arrayMode(elements);
        HybridNatBitSet bitSet = bitSetMode(elements);
        HybridNatBitSet roaring = roaringMode(elements);

        assertThat(array, is(bitSet));
        assertThat(bitSet, is(array));
        assertThat(array, is(roaring));
        assertThat(roaring, is(array));
        assertThat(bitSet, is(roaring));
        assertThat(roaring, is(bitSet));
        assertThat(array.hashCode(), is(bitSet.hashCode()));
        assertThat(array.hashCode(), is(roaring.hashCode()));
    }

    // Bulk operations between modes

    @Test
    void bulkOperationsAgreeAcrossModePairs() {
        int[] first = {1, 2, 3, 17, 600, 1_000_000};
        int[] second = {2, 3, 40, 600, 2_000_000};

        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                assertThat(apply(a, b, first, second, "or"), is(expected(first, second, "or")));
                assertThat(apply(a, b, first, second, "and"), is(expected(first, second, "and")));
                assertThat(apply(a, b, first, second, "andNot"), is(expected(first, second, "andNot")));
                assertThat(apply(a, b, first, second, "xor"), is(expected(first, second, "xor")));
            }
        }
    }

    private static HybridNatBitSet mode(int index, int... elements) {
        switch (index) {
            case 0:
                return arrayMode(elements);
            case 1:
                return bitSetMode(elements);
            default:
                return roaringMode(elements);
        }
    }

    private static NatBitSet apply(int modeA, int modeB, int[] first, int[] second, String operation) {
        HybridNatBitSet target = mode(modeA, first);
        HybridNatBitSet operand = mode(modeB, second);
        switch (operation) {
            case "or":
                target.or(operand);
                break;
            case "and":
                target.and(operand);
                break;
            case "andNot":
                target.andNot(operand);
                break;
            default:
                target.xor(operand);
                break;
        }
        return target;
    }

    private static IntSortedSet expected(int[] first, int[] second, String operation) {
        IntSortedSet result = reference(first);
        IntSortedSet other = reference(second);
        switch (operation) {
            case "or":
                result.addAll(other);
                break;
            case "and":
                result.retainAll(other);
                break;
            case "andNot":
                result.removeAll(other);
                break;
            default:
                IntSortedSet intersection = new IntAVLTreeSet(result);
                intersection.retainAll(other);
                result.addAll(other);
                result.removeAll(intersection);
                break;
        }
        return result;
    }

    // n-ary union

    @Test
    void unionAgreesWithFoldingOverEachShape() {
        int[][] shapes = {
            {1, 2, 3}, {1_000_000, 1_000_001}, range(0, 12), {7}, {}, range(500, 512),
        };
        for (int modes = 0; modes < 3; modes++) {
            List<IntCollection> operands = new ArrayList<>();
            IntSortedSet expected = new IntAVLTreeSet();
            for (int i = 0; i < shapes.length; i++) {
                operands.add(mode((i + modes) % 3, shapes[i]));
                expected.addAll(reference(shapes[i]));
            }

            NatBitSet union = NatBitSets.union(operands);
            assertThat(union, is(expected));
            assertThat(union.size(), is(expected.size()));
        }
    }

    @Test
    void unionOfNothingIsEmpty() {
        assertThat(NatBitSets.union(List.of()).isEmpty(), is(true));
        assertThat(NatBitSets.union(List.of(new HybridNatBitSet())).isEmpty(), is(true));
    }

    @Test
    void fullRangeOperandsProduceCorrectResults() {
        BoundedNatBitSet full = boundedFullSet(100);

        HybridNatBitSet union = arrayMode(1, 2, 500);
        union.or(full);
        IntSortedSet expectedUnion = reference(range(0, 100));
        expectedUnion.add(500);
        assertThat(union, is(expectedUnion));

        HybridNatBitSet intersection = arrayMode(1, 2, 500);
        intersection.and(full);
        assertThat(intersection, is(reference(1, 2)));

        HybridNatBitSet difference = arrayMode(1, 2, 500);
        difference.andNot(full);
        assertThat(difference, is(reference(500)));

        HybridNatBitSet flipped = arrayMode(1, 2, 500);
        flipped.xor(full);
        IntSortedSet expectedFlip = reference(range(0, 100));
        expectedFlip.removeAll(reference(1, 2));
        expectedFlip.add(500);
        assertThat(flipped, is(expectedFlip));
    }

    @Test
    void intersectsAgreesAcrossModesAndDirections() {
        int[][] operands = {{}, {0}, {1, 3, 5}, {0, 2, 4}, {999_999}, {1_000_000}};
        List<Function<int[], HybridNatBitSet>> modes = List.of(
                HybridNatBitSetTest::arrayMode, HybridNatBitSetTest::bitSetMode, HybridNatBitSetTest::roaringMode);

        for (int[] left : operands) {
            for (int[] right : operands) {
                IntSortedSet expected = reference(left);
                expected.retainAll(reference(right));
                boolean intersects = !expected.isEmpty();

                for (Function<int[], HybridNatBitSet> leftMode : modes) {
                    for (Function<int[], HybridNatBitSet> rightMode : modes) {
                        HybridNatBitSet one = leftMode.apply(left);
                        HybridNatBitSet other = rightMode.apply(right);
                        assertThat(one.intersects(other), is(intersects));
                        assertThat(other.intersects(one), is(intersects));
                    }
                }
            }
        }
    }

    // and/andNot walk the smaller side too

    private static HybridNatBitSet largeWords() {
        BitSet bitSet = new BitSet();
        for (int i = 0; i < 100_000; i++) {
            bitSet.set(2 * i);
        }
        return new HybridNatBitSet(bitSet);
    }

    private static HybridNatBitSet largeBitmap() {
        RoaringBitmap bitmap = new RoaringBitmap();
        for (int i = 0; i < 100_000; i++) {
            bitmap.add(2 * i);
        }
        return new HybridNatBitSet(bitmap);
    }

    @Test
    void andNotOfASmallOperandKeepsTheCachedSize() {
        for (HybridNatBitSet set : List.of(largeWords(), largeBitmap())) {
            assertThat(set.size(), is(100_000));

            set.andNot(new IntOpenHashSet(new int[] {0, 2, 3, 5}));
            assertThat(set.size(), is(99_998));
            assertThat(set.contains(0), is(false));
            assertThat(set.contains(2), is(false));
            assertThat(set.contains(4), is(true));
            assertThat(set.firstInt(), is(4));

            // Duplicates must not be counted twice, negative values are simply absent
            set.andNot(new IntArrayList(new int[] {4, 4, -1, -100, 6}));
            assertThat(set.size(), is(99_996));
            assertThat(set.firstInt(), is(8));
        }
    }

    @Test
    void andOfASmallOperandRebuildsFromIt() {
        for (HybridNatBitSet set : List.of(largeWords(), largeBitmap())) {
            set.and(new IntOpenHashSet(new int[] {4, 5, 6, 199_998, 200_000}));
            assertThat(set.size(), is(3));
            assertThat(set, is(reference(4, 6, 199_998)));
            assertThat(set.firstInt(), is(4));
            assertThat(set.lastInt(), is(199_998));
        }
    }

    @Test
    void smallOperandPathsAgreeWithTheGenericOnes() {
        int[][] operands = {{}, {0}, {1, 2, 3}, {0, 2, 4, 6}, {199_998, 200_000}, {5, 199_999}};
        for (int[] operand : operands) {
            IntSet argument = new IntOpenHashSet(operand);
            for (HybridNatBitSet set : List.of(largeWords(), largeBitmap())) {
                IntSortedSet expectedAnd = reference(set.toIntArray());
                expectedAnd.retainAll(argument);
                HybridNatBitSet intersection = set.clone();
                intersection.and(argument);
                assertThat(intersection, is(expectedAnd));
                assertThat(intersection.size(), is(expectedAnd.size()));

                IntSortedSet expectedAndNot = reference(set.toIntArray());
                expectedAndNot.removeAll(argument);
                HybridNatBitSet difference = set.clone();
                difference.andNot(argument);
                assertThat(difference.size(), is(expectedAndNot.size()));
                assertThat(difference, is(expectedAndNot));
            }
        }
    }

    @Test
    void removeAllOfASmallOperandReportsWithoutCounting() {
        for (HybridNatBitSet set : List.of(largeWords(), largeBitmap())) {
            assertThat(set.size(), is(100_000));
            assertThat(set.removeAll(new IntOpenHashSet(new int[] {1, 3, 5})), is(false));
            assertThat(set.size(), is(100_000));
            assertThat(set.removeAll(new IntOpenHashSet(new int[] {0, 1, 2})), is(true));
            assertThat(set.size(), is(99_998));
            assertThat(set.firstInt(), is(4));
        }
    }

    @Test
    void removeAllOfASmallOperandOnAnUncountedSet() {
        for (HybridNatBitSet set : List.of(largeWords(), largeBitmap())) {
            assertThat(set.removeAll(new IntOpenHashSet(new int[] {0, 2, 3})), is(true));
            assertThat(set.size(), is(99_998));
        }
    }

    @Test
    void removeIfKeepsTheSize() {
        for (HybridNatBitSet set : List.of(largeWords(), largeBitmap())) {
            assertThat(set.size(), is(100_000));
            assertThat(set.removeIf(value -> value % 4 == 0), is(true));
            assertThat(set.size(), is(50_000));
            assertThat(set.removeIf(value -> value % 4 == 0), is(false));
            assertThat(set.size(), is(50_000));
            assertThat(set.firstInt(), is(2));
        }
    }

    @Test
    void xorWithABitmapOperandFollowsItUpTheLadder() {
        HybridNatBitSet words = bitSetMode(1, 3, 5);
        words.xor(roaringMode(3, 5, 1_000_000));

        assertThat(words.store(), instanceOf(RoaringBitmap.class));
        assertThat(words, is(reference(1, 1_000_000)));
        assertThat(words.size(), is(2));
    }

    @Test
    void containsAllAgreesAcrossModes() {
        int[][] candidates = {{}, {0}, {1, 3}, {1, 3, 5}, {1, 3, 5, 200_001}, {200_001}};
        List<Function<int[], HybridNatBitSet>> modes = List.of(
                HybridNatBitSetTest::arrayMode, HybridNatBitSetTest::bitSetMode, HybridNatBitSetTest::roaringMode);

        HybridNatBitSet odd = bitSetMode(1, 3, 5, 7);
        for (int[] candidate : candidates) {
            boolean contained = reference(1, 3, 5, 7).containsAll(reference(candidate));
            for (Function<int[], HybridNatBitSet> mode : modes) {
                assertThat(odd.containsAll(mode.apply(candidate)), is(contained));
                assertThat(roaringMode(1, 3, 5, 7).containsAll(mode.apply(candidate)), is(contained));
                assertThat(arrayMode(1, 3, 5, 7).containsAll(mode.apply(candidate)), is(contained));
            }
        }
    }

    @Test
    void fullRangeQueries() {
        BoundedNatBitSet full = boundedFullSet(100);

        assertThat(arrayMode(1, 2).intersects(full), is(true));
        assertThat(arrayMode(500, 600).intersects(full), is(false));
        assertThat(arrayMode(1, 2).containsAll(full), is(false));

        HybridNatBitSet covering = new HybridNatBitSet();
        covering.set(0, 100);
        assertThat(covering.containsAll(full), is(true));
        covering.clear(50);
        assertThat(covering.containsAll(full), is(false));
    }

    @Test
    void emptyFullRangeComplementIsStillEmpty() {
        BoundedNatBitSet empty = NatBitSets.boundedSet(100);
        HybridNatBitSet set = arrayMode(1, 2);
        set.or(empty);
        assertThat(set, is(reference(1, 2)));
        set.and(empty);
        assertThat(set.isEmpty(), is(true));
    }

    // A filled range must not be materialised element by element

    @Test
    void fillingARangeUsesRunContainers() {
        HybridNatBitSet set = new HybridNatBitSet();
        set.set(0, 1_000_000);

        assertThat(set.store(), instanceOf(RoaringBitmap.class));
        assertThat(set.size(), is(1_000_000));
        assertThat(set.firstInt(), is(0));
        assertThat(set.lastInt(), is(999_999));
        // A run encoded range costs a few bytes per container, not one bit per element
        assertThat(((RoaringBitmap) set.store()).getSizeInBytes(), lessThan(1_000));
    }

    @Test
    void fillingASmallRangeStaysInWords() {
        HybridNatBitSet set = new HybridNatBitSet();
        set.set(0, 1_000);
        assertThat(set.store(), instanceOf(BitSet.class));
        assertThat(set.size(), is(1_000));
    }

    // Randomised cross-check, biased towards the thresholds

    @Test
    void randomOperationsMatchReference() {
        int[] interesting = {0, 1, 15, 16, 17, 63, 64, 65, 543, 544, 545, 1000, 65_535, 65_536, 1_000_000};
        Random random = new Random(2026L);

        for (int round = 0; round < 200; round++) {
            HybridNatBitSet set = new HybridNatBitSet();
            IntSortedSet expected = new IntAVLTreeSet();

            for (int step = 0; step < 60; step++) {
                int value = interesting[random.nextInt(interesting.length)] + random.nextInt(4);
                switch (random.nextInt(6)) {
                    case 0:
                        set.set(value);
                        expected.add(value);
                        break;
                    case 1:
                        set.clear(value);
                        expected.remove(value);
                        break;
                    case 2:
                        set.flip(value);
                        if (!expected.remove(value)) {
                            expected.add(value);
                        }
                        break;
                    case 3: {
                        int to = value + random.nextInt(80);
                        set.set(value, to);
                        for (int i = value; i < to; i++) {
                            expected.add(i);
                        }
                        break;
                    }
                    case 4: {
                        int to = value + random.nextInt(80);
                        set.clear(value, to);
                        for (int i = value; i < to; i++) {
                            expected.remove(i);
                        }
                        break;
                    }
                    default:
                        set.optimize();
                        break;
                }

                assertThat(set.size(), is(expected.size()));
                assertThat(set, is(expected));
                if (!expected.isEmpty()) {
                    assertThat(set.firstInt(), is(expected.firstInt()));
                    assertThat(set.lastInt(), is(expected.lastInt()));
                }
            }
        }
    }
}
