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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.SparseBitSet;
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
import java.util.function.IntSupplier;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class NatBitSetTheories {
  private static final int MAXIMAL_MUTATION_LENGTH = 100;
  private static final int MAXIMAL_SUBSET_SIZE = 512;
  private static final int NUMBER_OF_FIXED_SETS = 15;
  private static final int NUMBER_OF_LARGE_MUTATIONS = 100;
  private static final int NUMBER_OF_SMALL_MUTATIONS = 100;
  private static final int NUMBER_OF_SUBSETS_PER_IMPLEMENTATION = 20;
  private static final int POWER_SET_MAXIMUM = 12;
  private static final int SMALL_SUBSET_SIZE = Long.SIZE - 4;
  private static final Set<IntCollection> fixedSets;
  private static final Random generator = new Random(10L);
  private static final List<Pair> implementations;
  private static final List<List<Consumer<NatBitSet>>> largeMutations;
  private static final List<List<Consumer<NatBitSet>>> smallMutations;

  static {
    smallMutations =
        generateMutations(NUMBER_OF_SMALL_MUTATIONS, MAXIMAL_MUTATION_LENGTH, SMALL_SUBSET_SIZE);
    largeMutations =
        generateMutations(NUMBER_OF_LARGE_MUTATIONS, MAXIMAL_MUTATION_LENGTH, MAXIMAL_SUBSET_SIZE);

    fixedSets = new LinkedHashSet<>(NUMBER_OF_FIXED_SETS * 2);
    for (int i = 0; i < NUMBER_OF_FIXED_SETS; i++) {
      fixedSets.add(generateSet(MAXIMAL_SUBSET_SIZE, MAXIMAL_SUBSET_SIZE));
    }
    for (int i = 0; i < NUMBER_OF_FIXED_SETS; i++) {
      fixedSets.add(generateSet(SMALL_SUBSET_SIZE, SMALL_SUBSET_SIZE));
    }

    implementations = new ArrayList<>();
    implementations.add(new Pair(NatBitSets.emptySet()));

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      NatBitSet singleton = NatBitSets.singleton(generator.nextInt(MAXIMAL_SUBSET_SIZE));
      implementations.add(new Pair(singleton));
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
      BoundedNatBitSet boundedEmpty = NatBitSets.emptySet(generator.nextInt(MAXIMAL_SUBSET_SIZE));
      implementations.add(new Pair(boundedEmpty));
      implementations.add(new Pair(boundedEmpty.complement()));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      BoundedNatBitSet fullSet = NatBitSets.fullSet(generator.nextInt(MAXIMAL_SUBSET_SIZE));
      implementations.add(new Pair(fullSet));
      implementations.add(new Pair(fullSet.complement()));
    }

    for (int i = 0; i < NUMBER_OF_SUBSETS_PER_IMPLEMENTATION; i++) {
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE);
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
      int size = generator.nextInt(MAXIMAL_SUBSET_SIZE);
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
      int size = generator.nextInt(Long.SIZE);
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
  }

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

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

  private static List<List<Consumer<NatBitSet>>> generateMutations(int count, int length,
      int size) {
    List<List<Consumer<NatBitSet>>> mutations = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      int sequenceLength = generator.nextInt(length);
      List<Consumer<NatBitSet>> mutation = new ArrayList<>(sequenceLength);
      for (int j = 0; j < sequenceLength; j++) {
        Consumer<NatBitSet> mutationAction = generateMutation(() -> generator.nextInt(size));
        mutation.add(mutationAction);
      }
      mutations.add(mutation);
    }
    return mutations;
  }

  private static IntCollection generateSet(int maximalKey, int maximalEntries) {
    IntCollection set = generator.nextBoolean() ? new IntAVLTreeSet() : new IntOpenHashSet();
    int entries = generator.nextInt(maximalEntries + 1);
    for (int j = 0; j < entries; j++) {
      set.add(generator.nextInt(maximalKey));
    }
    return set;
  }

  @DataPoints("base")
  public static Set<IntCollection> getBases() {
    return fixedSets;
  }

  @DataPoints("implementation")
  public static List<Pair> getImplementations() {
    return implementations;
  }

  @DataPoints("small")
  public static List<List<Consumer<NatBitSet>>> getLargeMutations() {
    return smallMutations;
  }

  @DataPoints("large")
  public static List<List<Consumer<NatBitSet>>> getSmallMutations() {
    return largeMutations;
  }

  private void checkEquality(NatBitSet actual, NatBitSet expected) {
    assertThat(actual, is(expected));
    assertThat(actual.size(), is(expected.size()));
    assertThat(actual.containsAll(expected), is(true));
    assertThat(expected.containsAll(actual), is(true));
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

  private int getReadBounds(NatBitSet set) {
    return set instanceof BoundedNatBitSet
        ? ((BoundedNatBitSet) set).domainSize()
        : MAXIMAL_SUBSET_SIZE;
  }

  @SuppressWarnings("TypeMayBeWeakened")
  @Theory(nullsAccepted = false)
  public void testAdd(@FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("base") IntCollection basis) {
    assumeThat(implementation.modifiableType(), is(true));
    assumeThat(implementation.modifiable(basis), is(true));
    NatBitSet set = implementation.checkedCopy();

    ForwardingNatBitSet reference = new ForwardingNatBitSet(
        new IntAVLTreeSet(implementation.reference));

    basis.forEach((int i) -> assertThat(set.add(i), is(reference.add(i))));
    basis.forEach((int i) -> assertThat(set.add(i), is(false)));
    assertThat(set, is(reference));
  }

  @Theory(nullsAccepted = false)
  public void testAddAll(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assumeThat(implementation.modifiable(other.length()), is(true));
    NatBitSet set = implementation.checkedCopy();

    ForwardingNatBitSet reference = new ForwardingNatBitSet(
        new IntAVLTreeSet(implementation.reference));

    assertThat(set.addAll(other.set), is(reference.addAll(other.set)));
    checkEquality(set, reference);
    assertThat(set.addAll(other.set), is(false));
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testAnd(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assumeThat(implementation.modifiableType(), is(true));
    NatBitSet set = implementation.checkedCopy();

    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    set.and(other.set);
    reference.and(other.set);
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testAndNot(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assumeThat(implementation.modifiableType(), is(true));
    NatBitSet set = implementation.checkedCopy();

    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(implementation.reference));

    set.andNot(other.set);
    reference.andNot(other.set);
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testAsBounded(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    int domainSize;
    if (set instanceof BoundedNatBitSet) {
      domainSize = ((BoundedNatBitSet) set).domainSize();
    } else if (set instanceof LongNatBitSet) {
      domainSize = Long.SIZE - generator.nextInt(4);
    } else {
      domainSize = MAXIMAL_SUBSET_SIZE;
    }
    BoundedNatBitSet copy = NatBitSets.asBounded(set, domainSize);

    assertThat(copy.domainSize(), is(domainSize));
    checkEquality(copy, set);

    implementation.check();
  }

  @Theory(nullsAccepted = false)
  public void testAsSet(@FromDataPoints("base") IntIterable basis) {
    BitSet set = new BitSet();
    IntSet reference = new IntAVLTreeSet();
    basis.forEach((IntConsumer) index -> {
      set.set(index);
      reference.add(index);
    });

    NatBitSet ints = NatBitSets.asSet(set);
    assertThat(ints, is(reference));
  }

  @Theory(nullsAccepted = false)
  public void testClear(@FromDataPoints("implementation") Pair implementation) {
    assumeThat(implementation.modifiableType(), is(true));
    NatBitSet set = implementation.checkedCopy();

    set.clear();
    assertThat(set.isEmpty(), is(true));
  }

  @Theory(nullsAccepted = false)
  public void testClone(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    assertThat(set.clone(), is(set));
  }

  @Theory(nullsAccepted = false)
  public void testCompact(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    NatBitSet compacted = NatBitSets.compact(set);
    assertThat(compacted, is(set));

    implementation.check();
  }

  @Theory(nullsAccepted = false)
  public void testComplement(@FromDataPoints("implementation") Pair implementation) {
    assumeThat(implementation.set, instanceOf(BoundedNatBitSet.class));
    BoundedNatBitSet boundedSet = (BoundedNatBitSet) implementation.checkedCopy();

    BoundedNatBitSet complement = boundedSet.complement();
    assumeThat(complement, not(is(boundedSet)));
    assertThat(complement.intersects(boundedSet), is(false));
    assertThat(boundedSet.intersects(complement), is(false));
    assertThat(complement.domainSize(), is(boundedSet.domainSize()));

    for (int i = 0; i < boundedSet.domainSize(); i++) {
      assertThat(boundedSet.contains(i), not(is(complement.contains(i))));
    }

    IntSortedSet union = new IntAVLTreeSet();
    union.addAll(boundedSet);
    assertThat(union.size(), is(boundedSet.size()));
    union.addAll(complement);
    assertThat(union.toString(), union.size(), is(boundedSet.domainSize()));

    assumeThat(NatBitSets.isModifiable(complement), is(true));
    complement.clear();
    assertThat(boundedSet.size(), is(boundedSet.domainSize()));
    complement.addAll(union);
    assertThat(boundedSet.isEmpty(), is(true));
  }

  @Theory(nullsAccepted = false)
  public void testComplementIteratorLarger(@FromDataPoints("implementation") Pair implementation) {
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

  @Theory(nullsAccepted = false)
  public void testComplementIteratorSmaller(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    int size = set.isEmpty()
        ? generator.nextInt(MAXIMAL_SUBSET_SIZE)
        : set.lastInt() + generator.nextInt(10);
    IntIterator iterator = NatBitSets.complementIterator(set, size);
    NatBitSet complementSet = NatBitSets.boundedSet(size);
    iterator.forEachRemaining((IntConsumer) complementSet::set);
    assertThat(set, not(contains(complementSet)));
    assumeThat(set.intersects(complementSet), is(false));

    for (int i = 0; i < size; i++) {
      assertThat(complementSet.contains(i), not(is(set.contains(i))));
    }
  }

  @Theory(nullsAccepted = false)
  public void testContains(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < MAXIMAL_SUBSET_SIZE; i++) {
      assertThat(set.contains(i), is(implementation.reference.contains(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testContainsAll(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assertThat(implementation.set.containsAll(other.set),
        is(implementation.reference.containsAll(other.set)));
  }

  @Theory(nullsAccepted = false)
  public void testContainsBounded(@FromDataPoints("implementation") Pair implementation) {
    assumeThat(implementation.set, instanceOf(BoundedNatBitSet.class));
    BoundedNatBitSet boundedSet = (BoundedNatBitSet) implementation.checkedCopy();

    assertThat(boundedSet.contains(boundedSet.domainSize()), is(false));
  }

  @Theory(nullsAccepted = false)
  public void testCopyOfDirect(@FromDataPoints("base") IntCollection basis) {
    NatBitSet copy = NatBitSets.copyOf(basis);
    assertThat(copy, is(asSet(basis)));
  }

  @Theory(nullsAccepted = false)
  public void testCopyOfSet(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    NatBitSet copy = NatBitSets.copyOf(set);
    assertThat(copy, is(implementation.reference));
  }

  @Theory(nullsAccepted = false)
  public void testEnsureBounded(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.checkedCopy();

    int domainSize = implementation.length() + generator.nextInt(10);
    BoundedNatBitSet copy = NatBitSets.ensureBounded(set, domainSize);
    assertThat(copy.domainSize(), is(domainSize));
    checkEquality(copy, set);
  }

  @Theory(nullsAccepted = false)
  public void testEnsureModifiable(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.checkedCopy();

    NatBitSet copy = NatBitSets.ensureModifiable(set);
    checkEquality(copy, set);
    assertThat(NatBitSets.isModifiable(copy), is(true));

    copy.set(0, MAXIMAL_SUBSET_SIZE + 100);
  }

  @Theory(nullsAccepted = false)
  public void testEnsureModifiableLength(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.checkedCopy();

    NatBitSet copy = NatBitSets.ensureModifiable(set, 2 * MAXIMAL_SUBSET_SIZE);
    checkEquality(copy, set);
    assertThat(NatBitSets.isModifiable(copy, 2 * MAXIMAL_SUBSET_SIZE), is(true));

    copy.set(2 * MAXIMAL_SUBSET_SIZE - 1);
  }

  @Theory(nullsAccepted = false)
  public void testEquals(
      @FromDataPoints("implementation") Pair oneSet,
      @FromDataPoints("implementation") Pair otherSet) {
    assertThat(oneSet.set.equals(otherSet.set), is(oneSet.reference.equals(otherSet.reference)));
  }

  @Theory(nullsAccepted = false)
  public void testFirstInt(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;
    IntSortedSet reference = new IntAVLTreeSet(implementation.reference);

    if (reference.isEmpty()) {
      thrown.expect(NoSuchElementException.class);
    }
    assertThat(set.firstInt(), is(reference.firstInt()));
  }

  @Theory(nullsAccepted = false)
  public void testIterator(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    IntIterator iterator = set.iterator();
    IntIterator referenceIterator = new IntAVLTreeSet(implementation.reference).iterator();
    while (iterator.hasNext()) {
      assertThat(referenceIterator.hasNext(), is(true));
      assertThat(iterator.nextInt(), is(referenceIterator.nextInt()));
    }
    assertThat(referenceIterator.hasNext(), is(false));
  }

  @Theory(nullsAccepted = false)
  public void testIteratorAscending(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;
    int previous = -1;
    IntIterator iterator = set.iterator();
    while (iterator.hasNext()) {
      int current = iterator.nextInt();
      assertThat(current, greaterThan(previous));
      previous = current;
    }
  }

  @Theory(nullsAccepted = false)
  public void testLargeMutations(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("large") Iterable<Consumer<NatBitSet>> sequence) {
    assumeThat(NatBitSets.isModifiable(implementation.set, MAXIMAL_SUBSET_SIZE), is(true));
    testMutations(implementation, sequence);
  }

  @Theory(nullsAccepted = false)
  public void testLastInt(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;
    IntSortedSet reference = new IntAVLTreeSet(implementation.reference);

    if (reference.isEmpty()) {
      thrown.expect(NoSuchElementException.class);
    }
    assertThat(set.lastInt(), is(reference.lastInt()));
  }

  @Theory(nullsAccepted = false)
  public void testModifiableCopyOf(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    NatBitSet copy = NatBitSets.modifiableCopyOf(set, 2 * MAXIMAL_SUBSET_SIZE);
    checkEquality(copy, set);
    assertThat(copy, not(sameInstance(set)));
    assertThat(NatBitSets.isModifiable(copy, 2 * MAXIMAL_SUBSET_SIZE), is(true));

    copy.set(0, 2 * MAXIMAL_SUBSET_SIZE);
    assertThat(copy, not(is(set)));
  }

  @Theory(nullsAccepted = false)
  public void testModifiableCopyOfBoundedSet(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;
    assumeThat(set, instanceOf(BoundedNatBitSet.class));

    BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;

    BoundedNatBitSet copy = NatBitSets.modifiableCopyOf(boundedSet);
    checkEquality(copy, set);
    assertThat(copy, not(sameInstance(set)));
    assertThat(copy.domainSize(), is(boundedSet.domainSize()));

    copy.set(0, copy.domainSize());
  }

  private void testMutations(Pair implementation, Iterable<Consumer<NatBitSet>> sequence) {
    NatBitSet set = implementation.checkedCopy();
    NatBitSet reference = implementation.reference.clone();

    for (Consumer<NatBitSet> mutationAction : sequence) {
      mutationAction.accept(set);
      mutationAction.accept(reference);
      if (generator.nextInt(10) == 0) {
        checkEquality(set, reference);
      }
    }
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testNextAbsentIndex(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.nextAbsentIndex(i), is(implementation.bitSet.nextClearBit(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testNextPresentIndex(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.nextPresentIndex(i), is(implementation.bitSet.nextSetBit(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testOr(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assumeThat(implementation.modifiable(other.length()), is(true));
    NatBitSet set = implementation.checkedCopy();

    ForwardingNatBitSet reference = new ForwardingNatBitSet(
        new IntAVLTreeSet(implementation.reference));

    set.or(other.set);
    reference.or(other.reference);
    checkEquality(set, reference);

    other.check();
  }

  @Theory(nullsAccepted = false)
  public void testOrNot(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assumeThat(implementation.modifiableType(), is(true));
    assumeThat(implementation.set, instanceOf(BoundedNatBitSet.class));
    assumeThat(other.set, instanceOf(BoundedNatBitSet.class));

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

  @Theory(nullsAccepted = false)
  public void testPowerSet(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;
    assumeThat(set.size(), lessThan(POWER_SET_MAXIMUM));

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

  @Theory(nullsAccepted = false)
  public void testPreviousAbsentIndex(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.previousAbsentIndex(i), is(implementation.bitSet.previousClearBit(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testPreviousPresentIndex(
      @FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    for (int i = 0; i < SMALL_SUBSET_SIZE; i++) {
      assertThat(set.previousPresentIndex(i), is(implementation.bitSet.previousSetBit(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testRemove(@FromDataPoints("implementation") Pair implementation) {
    assumeThat(implementation.modifiableType(), is(true));
    NatBitSet set = implementation.checkedCopy();
    int limit = implementation.length();

    for (int i = 0; i < limit; i++) {
      assertThat(set.remove(i), is(implementation.reference.contains(i)));
    }
    assertThat(set.isEmpty(), is(true));
    for (int i = 0; i < limit; i++) {
      assertThat(set.remove(i), is(false));
    }
  }

  @Theory(nullsAccepted = false)
  public void testRemoveAll(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assumeThat(implementation.modifiable(other.length()), is(true));
    NatBitSet set = implementation.checkedCopy();

    ForwardingNatBitSet reference = new ForwardingNatBitSet(
        new IntAVLTreeSet(implementation.reference));

    assertThat(set.removeAll(other.set), is(reference.removeAll(other.reference)));
    checkEquality(set, reference);
    assertThat(set.removeAll(other.set), is(false));
  }

  @Theory(nullsAccepted = false)
  public void testRetainAll(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    assumeThat(implementation.modifiableType(), is(true));
    NatBitSet set = implementation.checkedCopy();

    ForwardingNatBitSet reference = new ForwardingNatBitSet(
        new IntAVLTreeSet(implementation.reference));

    assertThat(set.retainAll(other.set), is(reference.retainAll(other.reference)));
    checkEquality(set, reference);
    assertThat(set.retainAll(other.set), is(false));
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testReverseIterator(@FromDataPoints("implementation") Pair implementation) {
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

  @Theory(nullsAccepted = false)
  public void testSmallMutations(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("small") Iterable<Consumer<NatBitSet>> sequence) {
    assumeThat(NatBitSets.isModifiable(implementation.set, SMALL_SUBSET_SIZE), is(true));
    testMutations(implementation, sequence);
  }

  @Theory(nullsAccepted = false)
  public void testStream(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;
    IntSet reference = new IntAVLTreeSet(implementation.reference);

    IntSet streamAdd = new IntAVLTreeSet();
    set.intStream().forEach(streamAdd::add);
    assertThat(streamAdd, is(reference));

    assertThat(set.intStream().allMatch(implementation.reference::contains), is(true));
  }

  @Theory(nullsAccepted = false)
  public void testToBitSet(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    BitSet bitSet = NatBitSets.toBitSet(set);
    assertThat(bitSet, is(implementation.bitSet));
  }

  @Theory(nullsAccepted = false)
  public void testToSparseBitSet(@FromDataPoints("implementation") Pair implementation) {
    NatBitSet set = implementation.set;

    SparseBitSet bitSet = NatBitSets.toSparseBitSet(set);
    IntSet reference = new IntAVLTreeSet();
    BitSets.forEach(bitSet, reference::add);
    assertThat(set, is(reference));
  }

  @Theory(nullsAccepted = false)
  public void testXor(
      @FromDataPoints("implementation") Pair implementation,
      @FromDataPoints("implementation") Pair other) {
    int maximal = Math.max(implementation.length(), other.length());
    assumeThat(implementation.modifiable(maximal), is(true));
    assumeThat(other.modifiable(maximal), is(true));

    NatBitSet set = implementation.checkedCopy();

    ForwardingNatBitSet reference = new ForwardingNatBitSet(
        new IntAVLTreeSet(implementation.reference));

    assumeThat(!(set instanceof BoundedNatBitSet) || (other.set instanceof BoundedNatBitSet
        && ((BoundedNatBitSet) other.set).domainSize()
        <= ((BoundedNatBitSet) set).domainSize()), is(true));

    set.xor(other.set);
    reference.xor(other.set);
    checkEquality(set, reference);
    assertThat(other.set, is(asSet(other.reference)));
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

  private static class ForwardingNatBitSet extends AbstractNatBitSet {
    private final IntSortedSet delegate;

    public ForwardingNatBitSet(IntSortedSet delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean add(int key) {
      return delegate.add(key);
    }

    @Override
    public boolean addAll(IntCollection o) {
      return delegate.addAll(o);
    }

    @Override
    public void and(IntCollection o) {
      retainAll(o);
    }

    @Override
    public void andNot(IntCollection o) {
      removeAll(o);
    }

    @Override
    public void clear(int index) {
      remove(index);
    }

    @Override
    public void clear(int from, int to) {
      IntIterators.fromTo(from, to).forEachRemaining((IntConsumer) delegate::remove);
    }

    @Override
    public void clear() {
      delegate.clear();
    }

    @Override
    public void clearFrom(int from) {
      delegate.tailSet(from).clear();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ForwardingNatBitSet clone() {
      throw new AssertionError();
    }

    @Override
    public boolean contains(int key) {
      return delegate.contains(key);
    }

    @Override
    public boolean containsAll(IntCollection o) {
      return delegate.containsAll(o);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
      return delegate.equals(o);
    }

    @Override
    public int firstInt() {
      return delegate.firstInt();
    }

    @Override
    public void flip(int index) {
      if (!delegate.remove(index)) {
        delegate.add(index);
      }
    }

    @Override
    public void flip(int from, int to) {
      IntIterators.fromTo(from, to).forEachRemaining((IntConsumer) this::flip);
    }

    @Override
    public void forEach(IntConsumer action) {
      delegate.forEach(action);
    }

    @Override
    public int hashCode() {
      return delegate.hashCode();
    }

    @Override
    public boolean intersects(IntCollection o) {
      return IntIterators.any(delegate.iterator(), o::contains);
    }

    @Override
    public boolean isEmpty() {
      return delegate.isEmpty();
    }

    @Override
    public IntIterator iterator() {
      return delegate.iterator();
    }

    @Override
    public int lastInt() {
      return delegate.lastInt();
    }

    @Override
    public int nextAbsentIndex(int index) {
      int i = index;
      while (delegate.contains(i)) {
        i++;
      }
      return i;
    }

    @Override
    public int nextPresentIndex(int index) {
      IntSortedSet tail = delegate.tailSet(index);
      return tail.isEmpty() ? -1 : tail.firstInt();
    }

    @Override
    public void or(IntCollection o) {
      addAll(o);
    }

    @Override
    public int previousAbsentIndex(int index) {
      int i = index;
      while (i > -1 && delegate.contains(i)) {
        i--;
      }
      return i;
    }

    @Override
    public int previousPresentIndex(int index) {
      IntSortedSet tail = delegate.headSet(index);
      return tail.isEmpty() ? -1 : tail.lastInt();
    }

    @Override
    public boolean remove(int k) {
      return delegate.remove(k);
    }

    @Override
    public boolean removeAll(IntCollection o) {
      return delegate.removeAll(o);
    }

    @Override
    public boolean retainAll(IntCollection o) {
      return delegate.retainAll(o);
    }

    @Override
    public void set(int index) {
      add(index);
    }

    @Override
    public void set(int index, boolean value) {
      if (value) {
        add(index);
      } else {
        remove(index);
      }
    }

    @Override
    public void set(int from, int to) {
      IntIterators.fromTo(from, to).forEachRemaining((IntConsumer) delegate::add);
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public int[] toArray(int[] a) {
      return delegate.toArray(a);
    }

    @Override
    public int[] toIntArray() {
      return delegate.toIntArray();
    }

    @Override
    public String toString() {
      return delegate.toString();
    }

    @Override
    public void xor(IntCollection o) {
      SparseBitSet collection = new SparseBitSet();
      forEach((IntConsumer) collection::set);
      SparseBitSet other = new SparseBitSet();
      o.forEach((IntConsumer) other::set);

      clear();
      collection.xor(other);
      BitSets.forEach(collection, this::set);
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
      return this == o
          || o instanceof Pair
          && set.equals(((Pair) o).set);
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

    @SuppressWarnings("TypeMayBeWeakened")
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
      return set.getClass().getSimpleName() + " " + set.toString();
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
