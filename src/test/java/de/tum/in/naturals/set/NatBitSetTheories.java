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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.zaxxer.sparsebits.SparseBitSet;
import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.SparseBitSets;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NatBitSetTheories {
  private static final int MAXIMAL_MUTATION_LENGTH = 100;
  private static final int MAXIMAL_SUBSET_SIZE = 128;
  private static final int NUMBER_OF_FIXED_SETS = 50;
  private static final int NUMBER_OF_LARGE_MUTATIONS = 100;
  private static final int NUMBER_OF_SMALL_MUTATIONS = 100;
  private static final int NUMBER_OF_SUBSETS_PER_IMPLEMENTATION = 50;
  private static final int POWER_SET_MAXIMUM = 12;
  private static final int SMALL_SUBSET_SIZE = Long.SIZE - 4;
  private static final int TESTED_PAIRS = 50_000;
  private static final Set<IntCollection> fixedSets;
  private static final Random generator = new Random(10L);
  private static final List<Pair> implementations;
  private static final List<MutationList> largeMutations;
  private static final List<MutationList> smallMutations;

  static {
    smallMutations =
        generateMutations(NUMBER_OF_SMALL_MUTATIONS, MAXIMAL_MUTATION_LENGTH, SMALL_SUBSET_SIZE);
    largeMutations =
        generateMutations(NUMBER_OF_LARGE_MUTATIONS, MAXIMAL_MUTATION_LENGTH, MAXIMAL_SUBSET_SIZE);

    fixedSets = new LinkedHashSet<>(NUMBER_OF_FIXED_SETS * 3);

    for (int i = 0; i < NUMBER_OF_FIXED_SETS; i++) {
      fixedSets.add(generateSet(MAXIMAL_SUBSET_SIZE, MAXIMAL_SUBSET_SIZE));
    }
    for (int i = 0; i < NUMBER_OF_FIXED_SETS; i++) {
      fixedSets.add(generateSet(SMALL_SUBSET_SIZE, SMALL_SUBSET_SIZE));
    }

    implementations = new ArrayList<>();
    implementations.add(new Pair(NatBitSets.emptySet()));


    // Unbounded

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      NatBitSet singleton = NatBitSets.singleton(generator.nextInt(MAXIMAL_SUBSET_SIZE));
      implementations.add(new Pair(singleton));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(Long.SIZE);
      NatBitSet longSet = NatBitSets.longSet();
      longSet.addAll(generateSet(size, size));
      implementations.add(new Pair(longSet));
    }
    for (IntCollection fixedSet : fixedSets) {
      if (fixedSet.stream().anyMatch(value -> value >= Long.SIZE)) {
        continue;
      }
      NatBitSet longSet = NatBitSets.longSet();
      longSet.addAll(fixedSet);
      implementations.add(new Pair(longSet));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE);
      NatBitSet simpleSet = NatBitSets.simpleSet(size);
      simpleSet.addAll(generateSet(size, size));
      implementations.add(new Pair(simpleSet));
    }
    for (IntCollection fixedSet : fixedSets) {
      NatBitSet simpleSet = NatBitSets.simpleSet(MAXIMAL_SUBSET_SIZE);
      simpleSet.addAll(fixedSet);
      implementations.add(new Pair(simpleSet));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE);
      NatBitSet sparseSet = NatBitSets.sparseSet(size);
      sparseSet.addAll(generateSet(size, size));
      implementations.add(new Pair(sparseSet));
    }
    for (IntCollection fixedSet : fixedSets) {
      NatBitSet sparseSet = NatBitSets.sparseSet(MAXIMAL_SUBSET_SIZE);
      sparseSet.addAll(fixedSet);
      implementations.add(new Pair(sparseSet));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE);
      NatBitSet sparseSet = NatBitSets.roaringSet();
      sparseSet.addAll(generateSet(size, size));
      implementations.add(new Pair(sparseSet));
    }
    for (IntCollection fixedSet : fixedSets) {
      NatBitSet sparseSet = NatBitSets.roaringSet();
      sparseSet.addAll(fixedSet);
      implementations.add(new Pair(sparseSet));
    }


    // Bounded

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      BoundedNatBitSet boundedEmpty =
          NatBitSets.boundedEmptySet(generator.nextInt(MAXIMAL_SUBSET_SIZE - 1) + 1);
      implementations.add(new Pair(boundedEmpty));
      implementations.add(new Pair(boundedEmpty.complement()));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      BoundedNatBitSet fullSet =
          NatBitSets.boundedFullSet(generator.nextInt(MAXIMAL_SUBSET_SIZE - 1) + 1);
      implementations.add(new Pair(fullSet));
      implementations.add(new Pair(fullSet.complement()));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(Long.SIZE - 1) + 1;
      BoundedNatBitSet longSet = NatBitSets.boundedLongSet(size);
      longSet.addAll(generateSet(size, size));
      implementations.add(new Pair(longSet));
      implementations.add(new Pair(longSet.complement()));
    }
    for (IntCollection fixedSet : fixedSets) {
      if (fixedSet.stream().anyMatch(value -> value >= SMALL_SUBSET_SIZE)) {
        continue;
      }
      BoundedNatBitSet longSet = NatBitSets.boundedLongSet(Long.SIZE - generator.nextInt(4));
      longSet.addAll(fixedSet);
      implementations.add(new Pair(longSet));
      implementations.add(new Pair(longSet.complement()));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE - 1) + 1;
      BoundedNatBitSet simpleSet = NatBitSets.boundedSimpleSet(size);
      simpleSet.addAll(generateSet(size, size));
      implementations.add(new Pair(simpleSet));
      implementations.add(new Pair(simpleSet.complement()));
    }
    for (IntCollection fixedSet : fixedSets) {
      BoundedNatBitSet simpleSet = NatBitSets.boundedSimpleSet(MAXIMAL_SUBSET_SIZE);
      simpleSet.addAll(fixedSet);
      implementations.add(new Pair(simpleSet));
      implementations.add(new Pair(simpleSet.complement()));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE - 1) + 1;
      BoundedNatBitSet sparseSet = NatBitSets.boundedSparseSet(size);
      sparseSet.addAll(generateSet(size, size));
      implementations.add(new Pair(sparseSet));
      implementations.add(new Pair(sparseSet.complement()));
    }
    for (IntCollection fixedSet : fixedSets) {
      BoundedNatBitSet sparseSet = NatBitSets.boundedSparseSet(MAXIMAL_SUBSET_SIZE);
      sparseSet.addAll(fixedSet);
      implementations.add(new Pair(sparseSet));
      implementations.add(new Pair(sparseSet.complement()));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE - 1) + 1;
      BoundedNatBitSet roaringSet = NatBitSets.boundedRoaringSet(size);
      roaringSet.addAll(generateSet(size, size));
      implementations.add(new Pair(roaringSet));
      implementations.add(new Pair(roaringSet.complement()));
    }
    for (IntCollection fixedSet : fixedSets) {
      BoundedNatBitSet roaringSet = NatBitSets.boundedRoaringSet(MAXIMAL_SUBSET_SIZE);
      roaringSet.addAll(fixedSet);
      implementations.add(new Pair(roaringSet));
      implementations.add(new Pair(roaringSet.complement()));
    }
  }

  private static IntSet asSet(IntCollection ints) {
    return ints instanceof IntSet ? (IntSet) ints : new IntAVLTreeSet(ints);
  }

  @SuppressWarnings("MagicNumber")
  private static Consumer<NatBitSet> generateMutation(IntSupplier intSupplier) {
    int mutationId = generator.nextInt(55);
    if (mutationId == 0) {
      return new Clear();
    }
    if (mutationId < 10) {
      return new SetIndex(intSupplier.getAsInt());
    }
    if (mutationId < 13) {
      return new SetValue(intSupplier.getAsInt(), generator.nextBoolean());
    }
    if (mutationId < 15) {
      int first = intSupplier.getAsInt();
      int second = intSupplier.getAsInt();
      return new SetRange(Math.min(first, second), Math.max(first, second));
    }
    if (mutationId < 18) {
      int index = intSupplier.getAsInt();
      return new SetRange(index, index);
    }

    if (mutationId < 23) {
      return new ClearIndex(intSupplier.getAsInt());
    }
    if (mutationId < 25) {
      int first = intSupplier.getAsInt();
      int second = intSupplier.getAsInt();
      return new ClearRange(Math.min(first, second), Math.max(first, second));
    }
    if (mutationId < 29) {
      int from = intSupplier.getAsInt();
      return new ClearFrom(from);
    }

    if (mutationId < 30) {
      return new Flip(intSupplier.getAsInt());
    }
    if (mutationId < 35) {
      int first = intSupplier.getAsInt();
      int second = intSupplier.getAsInt();
      return new FlipRange(Math.min(first, second), Math.max(first, second));
    }

    IntCollection operationCollection = generator.nextBoolean()
        ? new IntAVLTreeSet() : new IntArrayList();

    int number = generator.nextInt(5) + 10;
    for (int i = 0; i < number; i++) {
      operationCollection.add(intSupplier.getAsInt());
    }

    if (mutationId < 40) {
      return new And(operationCollection);
    }
    if (mutationId < 45) {
      return new Or(operationCollection);
    }
    if (mutationId < 50) {
      return new Xor(operationCollection);
    }
    if (mutationId < 55) {
      return new AndNot(operationCollection);
    }
    throw new AssertionError();
  }

  @SuppressWarnings("SameParameterValue")
  private static List<MutationList> generateMutations(int count, int length, int size) {
    return IntStream.range(0, count).map(k -> generator.nextInt(length))
        .mapToObj(sequenceLength ->
            IntStream.range(0, sequenceLength)
                .mapToObj(i -> generateMutation(() -> generator.nextInt(size)))
                .collect(Collectors.toList()))
        .map(MutationList::new)
        .collect(Collectors.toList());
  }

  private static IntCollection generateSet(int maximalKey, int maximalEntries) {
    IntCollection set = generator.nextBoolean() ? new IntAVLTreeSet() : new IntOpenHashSet();
    int entries = generator.nextInt(maximalEntries + 1);
    for (int j = 0; j < entries; j++) {
      set.add(generator.nextInt(maximalKey));
    }
    return set;
  }

  public static Stream<IntCollection> bases() {
    return fixedSets.stream();
  }

  public static Stream<Pair> implementations() {
    return implementations.stream();
  }

  public static Stream<Arguments> small() {
    return implementations.stream().flatMap(implementation ->
        smallMutations.stream().limit(10).map(mutation -> Arguments.of(implementation, mutation)));
  }

  public static Stream<Arguments> large() {
    return implementations.stream().flatMap(implementation ->
        largeMutations.stream().limit(10).map(mutation -> Arguments.of(implementation, mutation)));
  }

  public static Stream<Arguments> implementationBasePairs() {
    Stream<Arguments> argumentsStream = implementations.stream().flatMap(one ->
        fixedSets.stream().map(other -> Arguments.of(one, other)));
    long size = implementations.size() * fixedSets.size();
    double ratio = TESTED_PAIRS / ((double) size);
    return ratio < 1.0
        ? argumentsStream.filter(a -> generator.nextDouble() < ratio)
        : argumentsStream;
  }

  public static Stream<Arguments> implementationPairs() {
    Stream<Arguments> argumentsStream = implementations.stream().flatMap(one ->
        implementations.stream().map(other -> Arguments.of(one, other)));
    long size = implementations.size() * implementations.size();
    double ratio = TESTED_PAIRS / ((double) size);
    return ratio < 1.0
        ? argumentsStream.filter(a -> generator.nextDouble() < ratio)
        : argumentsStream;
  }

  private static void checkEquality(NatBitSet actual, NatBitSet expected) {
    assertThat(actual, is(expected));
    assertThat(actual.size(), is(expected.size()));
    assertTrue(actual.containsAll(expected));
    assertTrue(expected.containsAll(actual));
    if (!actual.isEmpty()) {
      assertThat(actual.lastInt(), is(expected.lastInt()));
      assertThat(actual.firstInt(), is(expected.firstInt()));
    }

    int bounds = Math.min(getReadBounds(actual), getReadBounds(expected));
    if (bounds > 0) {
      for (int i = 0; i < 100; i++) {
        int num = generator.nextInt(bounds);
        assertThat(actual.contains(num), is(expected.contains(num)));
        assertThat(actual.intersects(IntSets.singleton(num)), is(expected.contains(num)));
      }
    }

    IntSortedSet iteratorCheck = new IntAVLTreeSet();
    actual.iterator().forEachRemaining((IntConsumer) iteratorCheck::add);
    assertThat(iteratorCheck, is(expected));
  }

  private static int getReadBounds(NatBitSet set) {
    return set instanceof BoundedNatBitSet
        ? ((BoundedNatBitSet) set).domainSize()
        : MAXIMAL_SUBSET_SIZE;
  }

  @SuppressWarnings("TypeMayBeWeakened")
  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationBasePairs")
  public void testAdd(Pair implementation, IntCollection basis) {
    assumeTrue(implementation.modifiableType());
    assumeTrue(implementation.modifiable(basis));
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    basis.forEach((int i) -> assertThat(set.add(i), is(reference.add(i))));
    basis.forEach((int i) -> assertFalse(set.add(i)));
    assertThat(set, is(reference));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testAddAll(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiable(other.length()));
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    assertThat(set.addAll(other.set), is(reference.addAll(other.reference)));
    checkEquality(set, reference);
    assertFalse(set.addAll(other.set));
    checkEquality(set, reference);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testAnd(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiableType());
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    set.and(other.set);
    reference.and(other.reference);
    checkEquality(set, reference);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testAndNot(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiableType());
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    set.andNot(other.set);
    reference.andNot(other.reference);

    checkEquality(set, reference);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testAsBounded(Pair implementation) {
    NatBitSet set = implementation.set;

    int domainSize;
    if (set instanceof BoundedNatBitSet) {
      domainSize = ((BoundedNatBitSet) set).domainSize();
    } else if (set instanceof LongNatBitSet) {
      domainSize = Long.SIZE - generator.nextInt(4);
    } else {
      domainSize = MAXIMAL_SUBSET_SIZE;
    }

    assumeTrue(set.isEmpty() || set.lastInt() < domainSize);

    BoundedNatBitSet copy = NatBitSets.asBounded(set, domainSize);

    assertThat(copy.domainSize(), is(domainSize));
    checkEquality(copy, set);

    implementation.check();
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("bases")
  public void testAsSet(IntIterable basis) {
    BitSet set = new BitSet();
    IntSet reference = new IntAVLTreeSet();
    basis.forEach((IntConsumer) index -> {
      set.set(index);
      reference.add(index);
    });

    NatBitSet ints = NatBitSets.asSet(set);
    assertThat(ints, is(reference));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testClear(Pair implementation) {
    assumeTrue(implementation.modifiableType());
    NatBitSet set = implementation.checkedCopy();

    set.clear();
    //noinspection ConstantConditions
    assertTrue(set.isEmpty());
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testClone(Pair implementation) {
    NatBitSet set = implementation.set;

    assertThat(set.clone(), is(set));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testCompact(Pair implementation) {
    NatBitSet set = implementation.set;

    NatBitSet compacted = NatBitSets.compact(set);
    assertThat(compacted, is(set));

    implementation.check();
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testComplement(Pair implementation) {
    assumeTrue(implementation.set instanceof BoundedNatBitSet);
    BoundedNatBitSet boundedSet = (BoundedNatBitSet) implementation.checkedCopy();

    BoundedNatBitSet complement = boundedSet.complement();
    assumeFalse(complement.equals(boundedSet));
    assertFalse(complement.intersects(boundedSet));
    assertFalse(boundedSet.intersects(complement));
    assertThat(complement.domainSize(), is(boundedSet.domainSize()));

    for (int i = 0; i < boundedSet.domainSize(); i++) {
      assertThat(boundedSet.contains(i), not(is(complement.contains(i))));
    }

    IntSortedSet union = new IntAVLTreeSet();
    union.addAll(boundedSet);
    assertThat(union.size(), is(boundedSet.size()));
    union.addAll(complement);
    assertThat(union.toString(), union.size(), is(boundedSet.domainSize()));

    assumeTrue(NatBitSets.isModifiable(complement));
    complement.clear();
    assertThat(boundedSet.size(), is(boundedSet.domainSize()));
    complement.addAll(union);
    assertTrue(boundedSet.isEmpty());
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testComplementIteratorLarger(Pair implementation) {
    NatBitSet set = implementation.set;

    int size = MAXIMAL_SUBSET_SIZE + 100;
    IntIterator iterator = NatBitSets.complementIterator(set, size);
    NatBitSet complementSet = NatBitSets.boundedSet(size);
    iterator.forEachRemaining((IntConsumer) complementSet::set);
    assertThat(set, not(contains(complementSet)));
    assertThat(set.size() + complementSet.size(), is(size));

    complementSet.or(set);
    assertThat(complementSet.size(), is(size));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testComplementIteratorSmaller(Pair implementation) {
    NatBitSet set = implementation.set;

    int size = set.isEmpty()
        ? generator.nextInt(MAXIMAL_SUBSET_SIZE - 1) + 1
        : set.lastInt() + generator.nextInt(10) + 1;
    IntIterator iterator = NatBitSets.complementIterator(set, size);
    NatBitSet complementSet = NatBitSets.boundedSet(size);
    iterator.forEachRemaining((IntConsumer) complementSet::set);
    assertThat(set, not(contains(complementSet)));
    assumeFalse(set.intersects(complementSet));

    for (int i = 0; i < size; i++) {
      assertThat(complementSet.contains(i), not(is(set.contains(i))));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testContains(Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < MAXIMAL_SUBSET_SIZE; i++) {
      assertThat(set.contains(i), is(implementation.reference.contains(i)));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testContainsAll(Pair implementation, Pair other) {
    assertThat(implementation.set.containsAll(other.set),
        is(implementation.reference.containsAll(other.reference)));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testContainsBounded(Pair implementation) {
    assumeTrue(implementation.set instanceof BoundedNatBitSet);
    BoundedNatBitSet boundedSet = (BoundedNatBitSet) implementation.checkedCopy();

    assertFalse(boundedSet.contains(boundedSet.domainSize()));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("bases")
  public void testCopyOfDirect(IntCollection basis) {
    NatBitSet copy = NatBitSets.copyOf(basis);
    assertThat(copy, is(asSet(basis)));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testCopyOfSet(Pair implementation) {
    NatBitSet set = implementation.set;

    NatBitSet copy = NatBitSets.copyOf(set);
    assertThat(copy, is(implementation.reference));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testEnsureBounded(Pair implementation) {
    NatBitSet set = implementation.checkedCopy();

    int domainSize = implementation.length() + generator.nextInt(10) + 1;
    BoundedNatBitSet copy = NatBitSets.ensureBounded(set, domainSize);
    assertThat(copy.domainSize(), is(domainSize));
    checkEquality(copy, set);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testEnsureModifiable(Pair implementation) {
    NatBitSet set = implementation.checkedCopy();

    NatBitSet copy = NatBitSets.ensureModifiable(set);
    checkEquality(copy, set);
    assertTrue(NatBitSets.isModifiable(copy));

    copy.set(0, MAXIMAL_SUBSET_SIZE + 100);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testEnsureModifiableLength(Pair implementation) {
    NatBitSet set = implementation.checkedCopy();

    NatBitSet copy = NatBitSets.ensureModifiable(set, 2 * MAXIMAL_SUBSET_SIZE);
    checkEquality(copy, set);
    assertTrue(NatBitSets.isModifiable(copy, 2 * MAXIMAL_SUBSET_SIZE));

    copy.set(2 * MAXIMAL_SUBSET_SIZE - 1);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testEquals(Pair oneSet, Pair otherSet) {
    assertThat(oneSet.set.equals(otherSet.set), is(oneSet.reference.equals(otherSet.reference)));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testFirstInt(Pair implementation) {
    NatBitSet set = implementation.set;
    IntSortedSet reference = new IntAVLTreeSet(implementation.reference);

    if (reference.isEmpty()) {
      assertThrows(NoSuchElementException.class, set::firstInt);
    } else {
      assertThat(set.firstInt(), is(reference.firstInt()));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testIterator(Pair implementation) {
    NatBitSet set = implementation.set;

    IntIterator iterator = set.iterator();
    IntIterator referenceIterator = new IntAVLTreeSet(implementation.reference).iterator();
    while (iterator.hasNext()) {
      assertTrue(referenceIterator.hasNext());
      assertThat(iterator.nextInt(), is(referenceIterator.nextInt()));
    }
    assertFalse(referenceIterator.hasNext());
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testIteratorAscending(Pair implementation) {
    NatBitSet set = implementation.set;
    int previous = -1;
    IntIterator iterator = set.iterator();
    while (iterator.hasNext()) {
      int current = iterator.nextInt();
      assertThat(current, greaterThan(previous));
      previous = current;
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testLastInt(Pair implementation) {
    NatBitSet set = implementation.set;
    IntSortedSet reference = new IntAVLTreeSet(implementation.reference);

    if (reference.isEmpty()) {
      assertThrows(NoSuchElementException.class, set::lastInt);
    } else {
      assertThat(set.lastInt(), is(reference.lastInt()));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testModifiableCopyOf(Pair implementation) {
    NatBitSet set = implementation.set;

    NatBitSet copy = NatBitSets.modifiableCopyOf(set, 2 * MAXIMAL_SUBSET_SIZE);
    checkEquality(copy, set);
    assertThat(copy, not(sameInstance(set)));
    assertTrue(NatBitSets.isModifiable(copy, 2 * MAXIMAL_SUBSET_SIZE));

    copy.set(0, 2 * MAXIMAL_SUBSET_SIZE);
    assertThat(copy, not(is(set)));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testModifiableCopyOfBoundedSet(Pair implementation) {
    NatBitSet set = implementation.set;
    assumeTrue(set instanceof BoundedNatBitSet);

    BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;

    BoundedNatBitSet copy = NatBitSets.modifiableCopyOf(boundedSet);
    checkEquality(copy, set);
    assertThat(copy, not(sameInstance(set)));
    assertThat(copy.domainSize(), is(boundedSet.domainSize()));

    copy.set(0, copy.domainSize());
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testNextAbsentIndex(Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.nextAbsentIndex(i), is(implementation.bitSet.nextClearBit(i)));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testNextPresentIndex(Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.nextPresentIndex(i), is(implementation.bitSet.nextSetBit(i)));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testOr(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiable(other.length()));
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    set.or(other.set);
    reference.or(other.reference);
    checkEquality(set, reference);

    other.check();
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testOrNot(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiableType());
    assumeTrue(implementation.set instanceof BoundedNatBitSet);
    assumeTrue(other.set instanceof BoundedNatBitSet);

    BoundedNatBitSet boundedSet = (BoundedNatBitSet) implementation.checkedCopy();
    NatBitSet otherSet = other.checkedCopy();

    boundedSet.orNot(otherSet);

    IntSortedSet reference = new IntAVLTreeSet(boundedSet);
    for (int i = 0; i < boundedSet.domainSize(); i++) {
      if (!other.bitSet.get(i)) {
        reference.add(i);
      }
    }

    checkEquality(boundedSet, new ForwardingNatBitSet(reference));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testPowerSet(Pair implementation) {
    NatBitSet set = implementation.set;
    assumeTrue(set.size() < POWER_SET_MAXIMUM);

    Set<NatBitSet> powerSet = NatBitSets.powerSet(set);
    assertThat(powerSet, hasSize(1 << set.size()));

    int iteratorSize = 0;
    Iterator<NatBitSet> iterator = powerSet.iterator();
    //noinspection WhileLoopReplaceableByForEach
    while (iterator.hasNext()) {
      iterator.next();
      iteratorSize += 1;
    }
    assertThat(iteratorSize, is(powerSet.size()));

    Set<BitSet> referencePowerSet = BitSets.powerSet(implementation.bitSet);
    for (BitSet subset : referencePowerSet) {
      BitSets.forEach(subset, index -> assertThat(set, hasItem(index)));
      assertThat(powerSet, hasItem(NatBitSets.asSet(subset)));
    }
    assertThat(powerSet, hasSize(referencePowerSet.size()));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testPreviousAbsentIndex(Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.previousAbsentIndex(i), is(implementation.bitSet.previousClearBit(i)));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testPreviousPresentIndex(Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.previousPresentIndex(i), is(implementation.bitSet.previousSetBit(i)));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testRemove(Pair implementation) {
    assumeTrue(implementation.modifiableType());
    NatBitSet set = implementation.checkedCopy();
    int limit = implementation.length();

    for (int i = 0; i < limit; i++) {
      assertThat(set.remove(i), is(implementation.reference.contains(i)));
    }
    assertTrue(set.isEmpty());
    for (int i = 0; i < limit; i++) {
      assertFalse(set.remove(i));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testRemoveAll(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiable(other.length()));
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    assertThat(set.removeAll(other.set), is(reference.removeAll(other.reference)));
    checkEquality(set, reference);
    assertFalse(set.removeAll(other.set));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testRemoveIf(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiable(other.length()));
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    IntPredicate predicate = other.set::contains;
    assertThat(set.removeIf(predicate), is(reference.removeIf(predicate)));
    checkEquality(set, reference);
    assertFalse(set.removeIf(predicate));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testRetainAll(Pair implementation, Pair other) {
    assumeTrue(implementation.modifiableType());
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    assertThat(set.retainAll(other.set), is(reference.retainAll(other.reference)));
    checkEquality(set, reference);
    assertFalse(set.retainAll(other.set));
    checkEquality(set, reference);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testReverseIterator(Pair implementation) {
    NatBitSet set = implementation.set;

    int size = set.size();
    int[] forwardArray = new int[size];
    int[] backwardArray = new int[size];
    int forward = IntIterators.unwrap(set.iterator(), forwardArray);
    int backward = IntIterators.unwrap(set.reverseIterator(), backwardArray);
    assertThat(Arrays.toString(backwardArray), backward, is(forward));

    for (int i = 0; i < size; i++) {
      assertThat(backwardArray[i], is(forwardArray[size - i - 1]));
    }
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testStream(Pair implementation) {
    NatBitSet set = implementation.set;
    IntSet reference = new IntAVLTreeSet(implementation.reference);

    IntSet streamAdd = new IntAVLTreeSet();
    set.intStream().forEach(streamAdd::add);
    assertThat(streamAdd, is(reference));

    assertTrue(set.intStream().allMatch(implementation.reference::contains));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testToBitSet(Pair implementation) {
    NatBitSet set = implementation.set;

    BitSet bitSet = NatBitSets.toBitSet(set);
    assertThat(bitSet, is(implementation.bitSet));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementations")
  public void testToSparseBitSet(Pair implementation) {
    NatBitSet set = implementation.set;

    SparseBitSet bitSet = NatBitSets.toSparseBitSet(set);
    IntSet reference = new IntAVLTreeSet();
    SparseBitSets.forEach(bitSet, reference::add);
    assertThat(set, is(reference));
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("implementationPairs")
  public void testXor(Pair implementation, Pair other) {
    int maximal = Math.max(implementation.length(), other.length());
    assumeTrue(implementation.modifiable(maximal));
    assumeTrue(other.modifiable(maximal));
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    assumeTrue(!(set instanceof BoundedNatBitSet) || (other.set instanceof BoundedNatBitSet
        && ((BoundedNatBitSet) other.set).domainSize()
        <= ((BoundedNatBitSet) set).domainSize()));

    set.xor(other.set);
    reference.xor(other.reference);
    checkEquality(set, reference);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("small")
  public void testSmallMutations(Pair implementation, MutationList sequence) {
    assumeTrue(NatBitSets.isModifiable(implementation.set, SMALL_SUBSET_SIZE));
    testMutations(implementation, sequence);
  }

  @ParameterizedTest(name = "{index}")
  @MethodSource("large")
  public void testLargeMutations(Pair implementation, MutationList sequence) {
    assumeTrue(NatBitSets.isModifiable(implementation.set, MAXIMAL_SUBSET_SIZE));
    testMutations(implementation, sequence);
  }

  private void testMutations(Pair implementation, MutationList sequence) {
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    int index = 0;
    Iterator<Consumer<NatBitSet>> iterator = sequence.iterator();
    while (iterator.hasNext()) {
      Consumer<NatBitSet> mutationAction = iterator.next();
      index += 1;

      mutationAction.accept(set);
      mutationAction.accept(reference);
      if (generator.nextInt(10) == 0 || !iterator.hasNext()) {
        try {
          checkEquality(set, reference);
        } catch (AssertionError e) {
          System.err.println(sequence.describe(implementation.checkedCopy(), // NOPMD
              implementation.reference.clone(), index));
          throw e;
        }
      }
    }
    checkEquality(set, reference);
  }

  private static final class MutationList implements Iterable<Consumer<NatBitSet>> {
    private final List<Consumer<NatBitSet>> list;

    private MutationList(List<Consumer<NatBitSet>> list) {
      this.list = new ArrayList<>(list);
    }

    @Override
    public Iterator<Consumer<NatBitSet>> iterator() {
      return list.iterator();
    }

    @Override
    public String toString() {
      return "\n" + list.stream().map(Object::toString).map(s -> "  " + s)
          .collect(Collectors.joining("\n")) + "\n";
    }

    public String describe(NatBitSet set, NatBitSet reference, int until) {
      StringBuilder builder = new StringBuilder("\n");
      for (int i = 0; i < until; i++) {
        Consumer<NatBitSet> action = list.get(i);
        action.accept(set);
        action.accept(reference);
        builder.append(action).append('\n');
        builder.append("  ").append(reference.size()).append(' ').append(reference).append('\n');
        builder.append("  ").append(set.size()).append(' ').append(set).append('\n');
        if (!set.equals(reference)) {
          break;
        }
      }
      return builder.toString();
    }
  }

  private static class And implements Consumer<NatBitSet> {
    final IntCollection collection;

    public And(IntCollection collection) {
      this.collection = collection;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.and(collection);
    }

    @Override
    public String toString() {
      return String.format("and{%s}", collection);
    }
  }

  private static class AndNot implements Consumer<NatBitSet> {
    final IntCollection collection;

    public AndNot(IntCollection collection) {
      this.collection = collection;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.andNot(collection);
    }

    @Override
    public String toString() {
      return String.format("andNot{%s}", collection);
    }
  }

  private static class Clear implements Consumer<NatBitSet> {
    @Override
    public void accept(NatBitSet ints) {
      ints.clear();
    }

    @Override
    public String toString() {
      return "clear";
    }
  }

  private static class ClearFrom implements Consumer<NatBitSet> {
    final int fromIndex;

    public ClearFrom(int fromIndex) {
      this.fromIndex = fromIndex;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.clearFrom(fromIndex);
    }

    @Override
    public String toString() {
      return String.format("clearFrom{%d}", fromIndex);
    }
  }

  private static class ClearIndex implements Consumer<NatBitSet> {
    final int index;

    public ClearIndex(int index) {
      this.index = index;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.clear(index);
    }

    @Override
    public String toString() {
      return String.format("clear{%d}", index);
    }
  }

  private static class ClearRange implements Consumer<NatBitSet> {
    final int fromIndex;
    final int toIndex;

    public ClearRange(int fromIndex, int toIndex) {
      this.fromIndex = fromIndex;
      this.toIndex = toIndex;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.clear(fromIndex, toIndex);
    }

    @Override
    public String toString() {
      return String.format("clear{%d,%d}", fromIndex, toIndex);
    }
  }

  private static class Flip implements Consumer<NatBitSet> {
    final int index;

    public Flip(int index) {
      this.index = index;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.flip(index);
    }

    @Override
    public String toString() {
      return String.format("flip{%d}", index);
    }
  }

  private static class FlipRange implements Consumer<NatBitSet> {
    final int fromIndex;
    final int toIndex;

    public FlipRange(int fromIndex, int toIndex) {
      this.fromIndex = fromIndex;
      this.toIndex = toIndex;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.flip(fromIndex, toIndex);
    }

    @Override
    public String toString() {
      return String.format("flip{%d,%d}", fromIndex, toIndex);
    }
  }

  private static class Or implements Consumer<NatBitSet> {
    final IntCollection collection;

    public Or(IntCollection collection) {
      this.collection = collection;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.or(collection);
    }

    @Override
    public String toString() {
      return String.format("or{%s}", collection);
    }
  }

  private static final class Pair {
    final BitSet bitSet;
    final NatBitSet reference;
    final NatBitSet set;

    Pair(NatBitSet set) {
      this.set = set;
      if (set.isEmpty()) {
        bitSet = new BitSet(0);
      } else {
        bitSet = new BitSet(set.lastInt());
        set.forEach((IntConsumer) bitSet::set);
      }
      this.reference = NatBitSets.asSet(bitSet);
    }

    void check() {
      assertThat(set, is(reference));
    }

    public NatBitSet checkedCopy() {
      NatBitSet clone = set.clone();
      assertThat(clone.getClass(), equalTo(set.getClass()));
      return clone;
    }

    @Override
    public boolean equals(Object o) {
      return this == o || (o instanceof Pair && set.equals(((Pair) o).set));
    }

    @Override
    public int hashCode() {
      return set.hashCode();
    }

    public int length() {
      return bitSet.length();
    }

    public boolean modifiable(int bound) {
      return NatBitSets.isModifiable(set, bound);
    }

    public boolean modifiable(IntIterable basis) {
      int maximum = 0;
      IntIterator iterator = basis.iterator();
      while (iterator.hasNext()) {
        int next = iterator.nextInt();
        if (next > maximum) {
          maximum = next;
        }
      }
      return NatBitSets.isModifiable(set, maximum + 1);
    }

    public boolean modifiableType() {
      return !(set instanceof MutableSingletonNatBitSet
          || set instanceof FixedSizeNatBitSet);
    }

    @Override
    public String toString() {
      return "\n" + set.getClass().getSimpleName() + "\n" + set;
    }
  }

  private static class SetIndex implements Consumer<NatBitSet> {
    final int index;

    public SetIndex(int index) {
      this.index = index;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.set(index);
    }

    @Override
    public String toString() {
      return String.format("set{%d}", index);
    }
  }

  private static class SetRange implements Consumer<NatBitSet> {
    final int fromIndex;
    final int toIndex;

    public SetRange(int fromIndex, int toIndex) {
      this.fromIndex = fromIndex;
      this.toIndex = toIndex;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.set(fromIndex, toIndex);
    }

    @Override
    public String toString() {
      return String.format("set{%d,%d}", fromIndex, toIndex);
    }
  }

  private static class SetValue implements Consumer<NatBitSet> {
    final int index;
    final boolean value;

    public SetValue(int index, boolean value) {
      this.index = index;
      this.value = value;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.set(index, value);
    }

    @Override
    public String toString() {
      return String.format("set{%d,%s}", index, value);
    }
  }

  private static class Xor implements Consumer<NatBitSet> {
    final IntCollection collection;

    public Xor(IntCollection collection) {
      this.collection = collection;
    }

    @Override
    public void accept(NatBitSet ints) {
      ints.xor(collection);
    }

    @Override
    public String toString() {
      return String.format("xor{%s}", collection);
    }
  }
}
