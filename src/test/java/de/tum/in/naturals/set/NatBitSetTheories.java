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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.SparseBitSet;
import it.unimi.dsi.fastutil.ints.AbstractIntCollection;
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
import java.util.BitSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class NatBitSetTheories {
  private static final int CROSS_COMPARE_BASIS_SET_SIZE = 100;
  private static final int MAXIMAL_KEY = 60;
  private static final int MAXIMAL_MUTATION_LENGTH = 100;
  private static final int NUMBER_OF_CROSS_COMPARE_BASIS_SETS = 15;
  private static final int NUMBER_OF_LARGE_MUTATIONS = 100;
  private static final int NUMBER_OF_SMALL_MUTATIONS = 100;
  private static final List<List<Consumer<NatBitSet>>> actions;
  private static final List<IntCollection> crossCompareBasis;
  private static final Random generator = new Random(10L);
  private static final List<Supplier<? extends NatBitSet>> implementations;

  static {
    List<List<Consumer<NatBitSet>>> generatedActions =
        new ArrayList<>(NUMBER_OF_SMALL_MUTATIONS + NUMBER_OF_LARGE_MUTATIONS);

    for (int i = 0; i < NUMBER_OF_SMALL_MUTATIONS; i++) {
      int sequenceLength = generator.nextInt(MAXIMAL_MUTATION_LENGTH);
      List<Consumer<NatBitSet>> actionSequence = new ArrayList<>(sequenceLength);
      for (int j = 0; j < sequenceLength; j++) {
        Consumer<NatBitSet> action = generateAction(() -> generator.nextInt(10));
        actionSequence.add(action);
      }
      generatedActions.add(actionSequence);
    }
    for (int i = 0; i < NUMBER_OF_LARGE_MUTATIONS; i++) {
      int sequenceLength = generator.nextInt(MAXIMAL_MUTATION_LENGTH);
      List<Consumer<NatBitSet>> actionSequence = new ArrayList<>(sequenceLength);
      for (int j = 0; j < sequenceLength; j++) {
        Consumer<NatBitSet> action = generateAction(() -> generator.nextInt(MAXIMAL_KEY));
        actionSequence.add(action);
      }
      generatedActions.add(actionSequence);
    }

    actions = generatedActions;

    crossCompareBasis = new ArrayList<>(NUMBER_OF_CROSS_COMPARE_BASIS_SETS + 2);
    for (int i = 0; i < NUMBER_OF_CROSS_COMPARE_BASIS_SETS; i++) {
      IntCollection set = generator.nextBoolean() ? new IntAVLTreeSet() : new IntOpenHashSet();
      int length = generator.nextInt(CROSS_COMPARE_BASIS_SET_SIZE);
      for (int j = 0; j < length; j++) {
        set.add(generator.nextInt(MAXIMAL_KEY));
      }
      crossCompareBasis.add(set);
      IntCollection complement = generator.nextBoolean() ? new IntAVLTreeSet() : new IntArrayList();
      for (int j = 0; j < MAXIMAL_KEY; j++) {
        if (set.contains(j)) {
          complement.add(j);
        }
      }
      crossCompareBasis.add(complement);
    }
    crossCompareBasis.add(IntSets.EMPTY_SET);
    IntSet all = new IntOpenHashSet();
    IntIterators.fromTo(0, MAXIMAL_KEY).forEachRemaining((IntConsumer) all::add);
    crossCompareBasis.add(all);

    Supplier<BoundedNatBitSet> boundedSimple = () ->
        NatBitSets.boundedSimpleSet(MAXIMAL_KEY + generator.nextInt(10));
    Supplier<BoundedNatBitSet> boundedSparse = () ->
        NatBitSets.boundedSparseSet(MAXIMAL_KEY + generator.nextInt(10));
    Supplier<BoundedNatBitSet> boundedLong = () ->
        NatBitSets.boundedLongSet(MAXIMAL_KEY + generator.nextInt(3));
    Supplier<BoundedNatBitSet> boundedRandom = () ->
        NatBitSets.boundedSet(MAXIMAL_KEY + generator.nextInt(100));
    Supplier<BoundedNatBitSet> boundedWrapper = () ->
        NatBitSets.asBounded(NatBitSets.setWithExpectedLength(MAXIMAL_KEY + generator.nextInt(100)),
            MAXIMAL_KEY);

    implementations = new ArrayList<>();
    implementations.add(named("singleton", () -> {
      NatBitSet singleton = NatBitSets.singleton(generator.nextInt(MAXIMAL_KEY));
      singleton.clear();
      return singleton;
    }));
    implementations.add(named("fixed", () ->
        NatBitSets.fullSet(MAXIMAL_KEY + generator.nextInt(10))));
    implementations.add(named("simple", () ->
        NatBitSets.simpleSet(MAXIMAL_KEY + generator.nextInt(10))));
    implementations.add(named("sparse", () ->
        NatBitSets.sparseSet(MAXIMAL_KEY + generator.nextInt(10))));
    implementations.add(named("long", NatBitSets::longSet));

    implementations.add(named("bounded simple", boundedSimple));
    implementations.add(named("bounded simple complement", complement(boundedSimple)));
    implementations.add(named("bounded sparse", boundedSparse));
    implementations.add(named("bounded sparse complement", complement(boundedSparse)));
    implementations.add(named("bounded long", boundedLong));
    implementations.add(named("bounded long complement", complement(boundedLong)));
    implementations.add(named("bounded random", boundedRandom));
    implementations.add(named("bounded random complement", complement(boundedRandom)));
    implementations.add(named("bounded random wrapper", boundedWrapper));
    implementations.add(named("bounded random wrapper complement", complement(boundedWrapper)));
  }

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  private static IntSet asSet(IntCollection ints) {
    return ints instanceof IntSet ? (IntSet) ints : new IntAVLTreeSet(ints);
  }

  private static Supplier<BoundedNatBitSet> complement(
      Supplier<? extends BoundedNatBitSet> supplier) {
    return () -> {
      BoundedNatBitSet set = supplier.get().complement();
      set.clear();
      return set;
    };
  }

  @SuppressWarnings("MagicNumber")
  private static Consumer<NatBitSet> generateAction(IntSupplier intSupplier) {
    int actionId = generator.nextInt(55);
    if (actionId == 0) {
      return new Clear();
    }
    if (actionId < 10) {
      return new SetIndex(intSupplier.getAsInt());
    }
    if (actionId < 13) {
      return new SetValue(intSupplier.getAsInt(), generator.nextBoolean());
    }
    if (actionId < 15) {
      int first = intSupplier.getAsInt();
      int second = intSupplier.getAsInt();
      return new SetRange(Math.min(first, second), Math.max(first, second));
    }
    if (actionId < 18) {
      int index = intSupplier.getAsInt();
      return new SetRange(index, index);
    }

    if (actionId < 23) {
      return new ClearIndex(intSupplier.getAsInt());
    }
    if (actionId < 25) {
      int first = intSupplier.getAsInt();
      int second = intSupplier.getAsInt();
      return new ClearRange(Math.min(first, second), Math.max(first, second));
    }

    if (actionId < 30) {
      return new Flip(intSupplier.getAsInt());
    }
    if (actionId < 35) {
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

    if (actionId < 40) {
      return new And(operationCollection);
    }
    if (actionId < 45) {
      return new Or(operationCollection);
    }
    if (actionId < 50) {
      return new Xor(operationCollection);
    }
    if (actionId < 55) {
      return new AndNot(operationCollection);
    }
    throw new AssertionError();
  }

  @DataPoints("actions")
  public static List<List<Consumer<NatBitSet>>> getActions() {
    return actions;
  }

  @DataPoints("basis")
  public static List<IntCollection> getCompareBasis() {
    return crossCompareBasis;
  }

  @DataPoints("implementations")
  public static List<Supplier<? extends NatBitSet>> getImplementations() {
    return implementations;
  }

  private static <V> Supplier<V> named(String name, Supplier<V> supplier) {
    return new NamedSupplier<>(name, supplier);
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

    for (int i = 0; i < 100; i++) {
      int num = generator.nextInt(MAXIMAL_KEY);
      assertThat(actual.contains(num), is(expected.contains(num)));
      assertThat(actual.intersects(IntSets.singleton(num)), is(expected.contains(num)));
    }

    IntSortedSet iteratorCheck = new IntAVLTreeSet();
    actual.iterator().forEachRemaining((IntConsumer) iteratorCheck::add);
    assertThat(iteratorCheck, is(expected));
  }

  @Theory(nullsAccepted = false)
  public void testAdd(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));

    basis.forEach((int i) -> assertThat(set.add(i), is(true)));
    basis.forEach((int i) -> assertThat(set.add(i), is(false)));
    assertThat(set, is(new IntAVLTreeSet(basis)));
  }

  @Theory(nullsAccepted = false)
  public void testAddAll(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));

    set.addAll(basis);
    otherSet.addAll(otherBasis);
    ForwardingNatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(basis));

    assertThat(set.addAll(otherSet), is(reference.addAll(otherBasis)));
    checkEquality(set, reference);
    assertThat(set.addAll(otherSet), is(false));
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testAnd(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(basis));

    set.addAll(basis);
    otherSet.addAll(otherBasis);

    set.and(otherSet);
    reference.and(otherSet);
    checkEquality(set, reference);
    assertThat(otherSet, is(asSet(otherBasis)));
  }

  @Theory(nullsAccepted = false)
  public void testAndNot(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(basis));

    set.addAll(basis);
    otherSet.addAll(otherBasis);

    set.andNot(otherSet);
    reference.andNot(otherSet);
    checkEquality(set, reference);
    assertThat(otherSet, is(asSet(otherBasis)));
  }

  @Theory(nullsAccepted = false)
  public void testAsBounded(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    int domainSize = set instanceof BoundedNatBitSet
        ? ((BoundedNatBitSet) set).domainSize() : MAXIMAL_KEY;
    BoundedNatBitSet copy = NatBitSets.asBounded(set, domainSize);

    assertThat(copy.domainSize(), is(domainSize));
    checkEquality(copy, set);
  }

  @Theory(nullsAccepted = false)
  public void testAsSet(
      @FromDataPoints("basis") IntIterable basis) {
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
  public void testClone(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    assertThat(set.clone(), is(set));
  }

  @Theory(nullsAccepted = false)
  public void testCompact(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(set, instanceOf(BoundedNatBitSet.class));
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    NatBitSet compacted = NatBitSets.compact(set);
    assertThat(compacted, is(set));
  }

  @Theory(nullsAccepted = false)
  public void testComplement(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(set, instanceOf(BoundedNatBitSet.class));
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;
    boundedSet.addAll(basis);

    BoundedNatBitSet complement = boundedSet.complement();
    assumeThat(complement, not(is(set)));
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

    complement.clear();
    assertThat(boundedSet.size(), is(boundedSet.domainSize()));
    complement.addAll(union);
    assertThat(boundedSet.isEmpty(), is(true));
  }

  @Theory(nullsAccepted = false)
  public void testComplementIteratorLarger(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    int size = 2 * MAXIMAL_KEY;
    IntIterator iterator = NatBitSets.complementIterator(set, size);
    NatBitSet complementSet = NatBitSets.boundedSet(size);
    iterator.forEachRemaining((IntConsumer) complementSet::set);
    assertThat(set, not(contains(complementSet)));
    assertThat(set.size() + complementSet.size(), is(size));

    complementSet.or(set);
    assertThat(complementSet.size(), is(size));
  }

  @Theory(nullsAccepted = false)
  public void testComplementIteratorSmaller(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    int size = MAXIMAL_KEY / 2;
    IntIterator iterator = NatBitSets.complementIterator(set, size);
    NatBitSet complementSet = NatBitSets.boundedSet(size);
    iterator.forEachRemaining((IntConsumer) complementSet::set);
    assertThat(set, not(contains(complementSet)));

    for (int i = 0; i < size; i++) {
      assertThat(complementSet.contains(i), not(is(set.contains(i))));
    }
  }

  @Theory(nullsAccepted = false)
  public void testContains(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    for (int i = 0; i < MAXIMAL_KEY; i++) {
      assertThat(set.contains(i), is(basis.contains(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testContainsAll(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));

    set.addAll(basis);
    otherSet.addAll(otherBasis);

    assertThat(set.containsAll(otherSet), is(basis.containsAll(otherBasis)));
  }

  @Theory(nullsAccepted = false)
  public void testContainsBounded(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(set, instanceOf(BoundedNatBitSet.class));

    BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;
    boundedSet.addAll(basis);

    assertThat(boundedSet.contains(boundedSet.domainSize()), is(false));
  }

  @Theory(nullsAccepted = false)
  public void testCopyOfDirect(
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet copy = NatBitSets.copyOf(basis);
    assertThat(copy, is(asSet(basis)));
  }

  @Theory(nullsAccepted = false)
  public void testCopyOfSet(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    NatBitSet copy = NatBitSets.copyOf(set);
    assertThat(copy, is(asSet(basis)));
  }

  @Theory(nullsAccepted = false)
  public void testEnsureBounded(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    BoundedNatBitSet copy = NatBitSets.ensureBounded(set, MAXIMAL_KEY);
    assertThat(copy.domainSize(), is(MAXIMAL_KEY));
    checkEquality(copy, set);
  }

  @Theory(nullsAccepted = false)
  public void testEnsureModifiable(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set), is(true));
    set.addAll(basis);

    NatBitSet copy = NatBitSets.ensureModifiable(set);
    checkEquality(copy, set);
    assertThat(NatBitSets.isModifiable(copy), is(true));

    copy.set(0, 10 * MAXIMAL_KEY - 1);
  }

  @Theory(nullsAccepted = false)
  public void testEnsureModifiableLength(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    NatBitSet copy = NatBitSets.ensureModifiable(set, 2 * MAXIMAL_KEY);
    checkEquality(copy, set);
    assertThat(NatBitSets.isModifiable(copy, 2 * MAXIMAL_KEY), is(true));

    copy.set(2 * MAXIMAL_KEY - 1);
  }

  @Theory(nullsAccepted = false)
  public void testEquals(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));

    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(basis));
    set.addAll(basis);
    NatBitSet otherReference = new ForwardingNatBitSet(new IntAVLTreeSet(otherBasis));
    otherSet.addAll(otherBasis);

    assertThat(set.equals(otherSet), is(reference.equals(otherReference)));
  }

  @Theory(nullsAccepted = false)
  public void testFirstInt(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);
    IntSortedSet reference = new IntAVLTreeSet(basis);

    if (basis.isEmpty()) {
      thrown.expect(NoSuchElementException.class);
    }
    assertThat(set.firstInt(), is(reference.firstInt()));
  }

  @Theory(nullsAccepted = false)
  public void testIterator(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    IntIterator iterator = set.iterator();
    IntIterator referenceIterator = new IntAVLTreeSet(basis).iterator();
    while (iterator.hasNext()) {
      assertThat(referenceIterator.hasNext(), is(true));
      assertThat(iterator.nextInt(), is(referenceIterator.nextInt()));
    }
    assertThat(referenceIterator.hasNext(), is(false));
  }

  @Theory(nullsAccepted = false)
  public void testLastInt(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);
    IntSortedSet reference = new IntAVLTreeSet(basis);

    if (basis.isEmpty()) {
      thrown.expect(NoSuchElementException.class);
    }
    assertThat(set.lastInt(), is(reference.lastInt()));
  }

  @Theory(nullsAccepted = false)
  public void testModifiableCopyOf(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    NatBitSet copy = NatBitSets.modifiableCopyOf(set, 2 * MAXIMAL_KEY);
    checkEquality(copy, set);
    assertThat(copy, not(sameInstance(set)));
    assertThat(NatBitSets.isModifiable(copy, 2 * MAXIMAL_KEY), is(true));

    copy.set(0, 2 * MAXIMAL_KEY);
    assertThat(copy, not(is(set)));
  }

  @Theory(nullsAccepted = false)
  public void testModifiableCopyOfBoundedSet(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(set, instanceOf(BoundedNatBitSet.class));
    set.addAll(basis);

    BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;

    BoundedNatBitSet copy = NatBitSets.modifiableCopyOf(boundedSet);
    checkEquality(copy, set);
    assertThat(copy, not(sameInstance(set)));
    assertThat(copy.domainSize(), is(boundedSet.domainSize()));

    copy.set(0, copy.domainSize());
  }

  @Theory(nullsAccepted = false)
  public void testMutations(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("actions") Iterable<Consumer<NatBitSet>> sequence,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(basis));
    set.addAll(basis);

    for (Consumer<NatBitSet> action : sequence) {
      action.accept(set);
      action.accept(reference);
      if (generator.nextInt(10) == 0) {
        checkEquality(set, reference);
      }
    }
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testNextAbsentIndex(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);
    BitSet reference = new BitSet();
    basis.forEach((IntConsumer) reference::set);

    for (int i = 0; i < MAXIMAL_KEY; i++) {
      assertThat(set.nextAbsentIndex(i), is(reference.nextClearBit(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testNextPresentIndex(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);
    BitSet reference = new BitSet();
    basis.forEach((IntConsumer) reference::set);

    for (int i = 0; i < MAXIMAL_KEY; i++) {
      assertThat(set.nextPresentIndex(i), is(reference.nextSetBit(i)));
    }
  }

  @Theory(nullsAccepted = false)
  public void testOr(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));

    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(basis));
    set.addAll(basis);
    otherSet.addAll(otherBasis);

    set.or(otherSet);
    reference.or(otherSet);
    checkEquality(set, reference);
    assertThat(otherSet, is(asSet(otherBasis)));
  }

  @Theory(nullsAccepted = false)
  public void testOrNot(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    assumeThat(set, instanceOf(BoundedNatBitSet.class));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));
    assumeThat(otherSet, instanceOf(BoundedNatBitSet.class));

    BoundedNatBitSet boundedSet = (BoundedNatBitSet) set;

    set.addAll(basis);
    otherSet.addAll(otherBasis);
    IntSortedSet reference = new IntAVLTreeSet(basis);

    boundedSet.orNot(otherSet);
    for (int i = 0; i < boundedSet.domainSize(); i++) {
      if (!otherBasis.contains(i)) {
        reference.add(i);
      }
    }

    checkEquality(set, new ForwardingNatBitSet(reference));
  }

  @Theory(nullsAccepted = false)
  public void testRemove(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    for (int i = 0; i < MAXIMAL_KEY; i++) {
      assertThat(set.remove(i), is(basis.contains(i)));
    }
    assertThat(set.isEmpty(), is(true));
    for (int i = 0; i < MAXIMAL_KEY; i++) {
      assertThat(set.remove(i), is(false));
    }
  }

  @Theory(nullsAccepted = false)
  public void testRemoveAll(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));

    set.addAll(basis);
    otherSet.addAll(otherBasis);
    IntAVLTreeSet reference = new IntAVLTreeSet(basis);

    assertThat(set.removeAll(otherSet), is(reference.removeAll(otherBasis)));
    checkEquality(set, new ForwardingNatBitSet(reference));
    assertThat(set.removeAll(otherSet), is(false));
  }

  @Theory(nullsAccepted = false)
  public void testRetainAll(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));

    set.addAll(basis);
    otherSet.addAll(otherBasis);
    IntAVLTreeSet reference = new IntAVLTreeSet(basis);

    assertThat(set.retainAll(otherSet), is(reference.retainAll(otherBasis)));
    checkEquality(set, new ForwardingNatBitSet(reference));
    assertThat(set.retainAll(otherSet), is(false));
    checkEquality(set, new ForwardingNatBitSet(reference));
  }

  @Theory(nullsAccepted = false)
  public void testToBitSet(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    BitSet bitSet = NatBitSets.toBitSet(set);
    IntSet reference = new IntAVLTreeSet();
    BitSets.forEach(bitSet, reference::add);
    assertThat(set, is(reference));
  }

  @Theory(nullsAccepted = false)
  public void testToSparseBitSet(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntCollection basis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    set.addAll(basis);

    SparseBitSet bitSet = NatBitSets.toSparseBitSet(set);
    IntSet reference = new IntAVLTreeSet();
    BitSets.forEach(bitSet, reference::add);
    assertThat(set, is(reference));
  }

  @Theory(nullsAccepted = false)
  public void testXor(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntCollection basis,
      @FromDataPoints("basis") IntCollection otherBasis) {
    NatBitSet set = implementation.get();
    assumeThat(NatBitSets.isModifiable(set, MAXIMAL_KEY), is(true));
    NatBitSet otherSet = otherImplementation.get();
    assumeThat(NatBitSets.isModifiable(otherSet, MAXIMAL_KEY), is(true));

    assumeThat(!(set instanceof BoundedNatBitSet) || (otherSet instanceof BoundedNatBitSet
            && ((BoundedNatBitSet) otherSet).domainSize() <= ((BoundedNatBitSet) set).domainSize()),
        is(true));

    set.addAll(basis);
    otherSet.addAll(otherBasis);
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet(basis));

    set.xor(otherSet);
    reference.xor(otherSet);
    checkEquality(set, reference);
    assertThat(otherSet, is(asSet(otherBasis)));
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

  private static class ForwardingNatBitSet extends AbstractIntCollection implements NatBitSet {
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

  private static final class NamedSupplier<V> implements Supplier<V> {
    // Helper class to have nice toString in test cases
    private final Supplier<V> delegate;
    private final String name;

    NamedSupplier(String name, Supplier<V> delegate) {
      this.name = name;
      this.delegate = delegate;
    }

    @Override
    public V get() {
      return delegate.get();
    }

    @Override
    public String toString() {
      return name;
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
