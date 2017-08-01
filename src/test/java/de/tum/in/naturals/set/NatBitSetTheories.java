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

import static org.hamcrest.Matchers.is;
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
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class NatBitSetTheories {
  private static final int MAXIMAL_KEY = 100;
  private static final int MAXIMAL_MODIFICATIONS = 100;
  private static final int NUMBER_OF_CROSS_COMPARE_SIZE = 100;
  private static final int NUMBER_OF_CROSS_COMPARE_TESTS = 50;
  private static final int NUMBER_OF_LARGE_TESTS = 100;
  private static final int NUMBER_OF_SMALL_TESTS = 100;

  private static final List<List<Consumer<NatBitSet>>> actions;
  private static final List<IntSet> crossCompareBasis;
  private static final Random generator = new Random(10L);
  private static final List<Supplier<NatBitSet>> implementations;

  static {
    implementations = new ArrayList<>(4);
    implementations.add(() -> NatBitSets.simpleSet(MAXIMAL_KEY + generator.nextInt(10)));
    implementations.add(() -> NatBitSets.boundedSimpleSet(MAXIMAL_KEY + generator.nextInt(10)));
    implementations.add(() -> NatBitSets.sparseSet(MAXIMAL_KEY + generator.nextInt(10)));
    implementations.add(() -> NatBitSets.boundedSparseSet(MAXIMAL_KEY + generator.nextInt(10)));

    List<List<Consumer<NatBitSet>>> generatedActions =
        new ArrayList<>(NUMBER_OF_SMALL_TESTS + NUMBER_OF_LARGE_TESTS);

    for (int i = 0; i < NUMBER_OF_SMALL_TESTS; i++) {
      int sequenceLength = generator.nextInt(MAXIMAL_MODIFICATIONS);
      List<Consumer<NatBitSet>> actionSequence = new ArrayList<>(sequenceLength);
      for (int j = 0; j < sequenceLength; j++) {
        Consumer<NatBitSet> action = generateAction(() -> generator.nextInt(10));
        actionSequence.add(action);
      }
      generatedActions.add(actionSequence);
    }
    for (int i = 0; i < NUMBER_OF_LARGE_TESTS; i++) {
      int sequenceLength = generator.nextInt(MAXIMAL_MODIFICATIONS);
      List<Consumer<NatBitSet>> actionSequence = new ArrayList<>(sequenceLength);
      for (int j = 0; j < sequenceLength; j++) {
        Consumer<NatBitSet> action = generateAction(() -> generator.nextInt(MAXIMAL_KEY));
        actionSequence.add(action);
      }
      generatedActions.add(actionSequence);
    }

    actions = generatedActions;

    crossCompareBasis = new ArrayList<>(NUMBER_OF_CROSS_COMPARE_TESTS);
    for (int i = 0; i < NUMBER_OF_CROSS_COMPARE_TESTS; i++) {
      IntSet set = new IntAVLTreeSet();
      int length = generator.nextInt(NUMBER_OF_CROSS_COMPARE_SIZE);
      for (int j = 0; j < length; j++) {
        set.add(generator.nextInt(MAXIMAL_KEY));
      }
      crossCompareBasis.add(set);
    }
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

    IntCollection operationCollection;
    int operationId = generator.nextInt(2 + implementations.size());
    if (operationId == 0) {
      operationCollection = new IntAVLTreeSet();
    } else if (operationId == 1) {
      operationCollection = new IntArrayList();
    } else {
      operationCollection = implementations.get(operationId - 2).get();
    }

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
  public static List<IntSet> getCompareBasis() {
    return crossCompareBasis;
  }

  @DataPoints("implementations")
  public static List<Supplier<NatBitSet>> getImplementations() {
    return implementations;
  }

  private void checkEquality(NatBitSet actual, NatBitSet expected) {
    assertThat(actual, is(expected));
    assertThat(actual.containsAll(expected), is(true));
    if (!actual.isEmpty()) {
      assertThat(actual.lastInt(), is(expected.lastInt()));
      assertThat(actual.firstInt(), is(expected.firstInt()));
    }

    for (int i = 0; i < 100; i++) {
      int num = generator.nextInt(MAXIMAL_KEY);
      assertThat(actual.contains(num), is(expected.contains(num)));
      assertThat(actual.intersects(IntSets.singleton(num)), is(expected.contains(num)));
    }

    IntSortedSet check = new IntAVLTreeSet();
    actual.iterator().forEachRemaining((IntConsumer) check::add);
    assertThat(check, is(expected));
  }

  @Theory(nullsAccepted = false)
  public void testAnd(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntIterable basis,
      @FromDataPoints("basis") IntIterable otherBasis) {
    NatBitSet set = implementation.get();
    NatBitSet otherSet = otherImplementation.get();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet());

    basis.forEach((int i) -> {
      set.set(i);
      reference.set(i);
    });
    otherBasis.forEach((IntConsumer) otherSet::set);

    set.and(otherSet);
    reference.and(otherSet);
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testAndNot(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntIterable basis,
      @FromDataPoints("basis") IntIterable otherBasis) {
    NatBitSet set = implementation.get();
    NatBitSet otherSet = otherImplementation.get();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet());

    basis.forEach((int i) -> {
      set.set(i);
      reference.set(i);
    });
    otherBasis.forEach((IntConsumer) otherSet::set);

    set.andNot(otherSet);
    reference.andNot(otherSet);
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testComplement(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("basis") IntIterable basis) {
    NatBitSet testSet = implementation.get();
    assumeThat(testSet instanceof BoundedNatBitSet, is(true));
    //noinspection ConstantConditions
    BoundedNatBitSet set = (BoundedNatBitSet) testSet;
    basis.forEach((IntConsumer) set::set);

    NatBitSet complement = set.complement();
    assertThat(complement.intersects(set), is(false));
    assertThat(set.intersects(complement), is(false));

    IntSortedSet union = new IntAVLTreeSet();
    union.addAll(set);
    assertThat(union.size(), is(set.size()));
    union.addAll(complement);
    assertThat(union.size(), is(set.domainSize()));

    complement.clear();
    assertThat(set.size(), is(set.domainSize()));
    complement.addAll(union);
    assertThat(set.isEmpty(), is(true));
  }

  @Theory(nullsAccepted = false)
  public void testMutations(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("actions") Iterable<Consumer<NatBitSet>> sequence,
      @FromDataPoints("basis") IntIterable basis) {
    NatBitSet set = implementation.get();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet());
    basis.forEach((int i) -> {
      set.set(i);
      reference.set(i);
    });

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
  public void testOr(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntIterable basis,
      @FromDataPoints("basis") IntIterable otherBasis) {
    NatBitSet set = implementation.get();
    NatBitSet otherSet = otherImplementation.get();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet());

    basis.forEach((int i) -> {
      set.set(i);
      reference.set(i);
    });
    otherBasis.forEach((IntConsumer) otherSet::set);

    set.or(otherSet);
    reference.or(otherSet);
    checkEquality(set, reference);
  }

  @Theory(nullsAccepted = false)
  public void testXor(
      @FromDataPoints("implementations") Supplier<NatBitSet> implementation,
      @FromDataPoints("implementations") Supplier<NatBitSet> otherImplementation,
      @FromDataPoints("basis") IntIterable basis,
      @FromDataPoints("basis") IntIterable otherBasis) {
    NatBitSet set = implementation.get();
    NatBitSet otherSet = otherImplementation.get();
    NatBitSet reference = new ForwardingNatBitSet(new IntAVLTreeSet());

    basis.forEach((int i) -> {
      set.set(i);
      reference.set(i);
    });
    otherBasis.forEach((IntConsumer) otherSet::set);

    set.xor(otherSet);
    reference.xor(otherSet);
    checkEquality(set, reference);
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
      return "clear";
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
      delegate.subSet(from, to).clear();
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
      IntIterators.fromTo(from, to).forEachRemaining((IntConsumer) this::set);
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
      return String.format("collection{%d}", index);
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
      return String.format("collection{%d,%d}", fromIndex, toIndex);
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
      return String.format("clear{%d,%s}", index, value);
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
