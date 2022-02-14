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

package de.tum.in.naturals.bitset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.zaxxer.sparsebits.SparseBitSet;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
  private static final List<IntCollection> indices;

  static {
    List<IntCollection> generatedIndices = new ArrayList<>(NUMBER_OF_TESTS);
    for (int i = 0; i < NUMBER_OF_TESTS; i++) {
      int size = generator.nextInt(MAXIMUM_SIZE);
      IntCollection list = new IntArrayList(size);
      for (int j = 0; j < size; j++) {
        list.add(generator.nextInt(MAXIMUM_ELEMENTS));
      }
      generatedIndices.add(list);
    }
    indices = Collections.unmodifiableList(generatedIndices);
  }

  public static Stream<IntCollection> indices() {
    return indices.stream();
  }

  @ParameterizedTest
  @MethodSource("indices")
  void testComplementIterator(IntCollection ints) {
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
  void testForEach(IntCollection ints) {
    BitSet bitSet = new BitSet();
    ints.forEach((IntConsumer) bitSet::set);

    IntSet result = new IntOpenHashSet(bitSet.cardinality());
    BitSets.forEach(bitSet, result::add);

    IntSet expected = new IntOpenHashSet(ints);

    assertThat(result, is(expected));
  }

  @ParameterizedTest
  @MethodSource("indices")
  void testImmutableBitSet(IntCollection ints) {
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
  void testIterator(IntCollection ints) {
    BitSet bitSet = new BitSet();
    ints.forEach((IntConsumer) bitSet::set);

    IntSet result = new IntOpenHashSet(bitSet.cardinality());
    BitSets.iterator(bitSet).forEachRemaining((IntConsumer) result::add);

    IntSet expected = new IntOpenHashSet(ints);

    assertThat(result, is(expected));
  }

  @ParameterizedTest
  @MethodSource("indices")
  void testSparseComplementIterator(IntCollection ints) {
    SparseBitSet bitSet = new SparseBitSet();
    ints.forEach((IntConsumer) bitSet::set);

    IntSet result = new IntOpenHashSet(bitSet.cardinality());
    SparseBitSets.complementIterator(bitSet, COMPLEMENT_SIZE)
        .forEachRemaining((IntConsumer) result::add);

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
  void testSparseForEach(IntCollection ints) {
    SparseBitSet bitSet = new SparseBitSet();
    ints.forEach((IntConsumer) bitSet::set);

    IntSet result = new IntOpenHashSet(bitSet.cardinality());
    SparseBitSets.forEach(bitSet, result::add);

    IntSet expected = new IntOpenHashSet(ints);

    assertThat(result, is(expected));
  }

  @ParameterizedTest
  @MethodSource("indices")
  void testSparseIterator(IntCollection ints) {
    SparseBitSet bitSet = new SparseBitSet();
    ints.forEach((IntConsumer) bitSet::set);

    IntSet result = new IntOpenHashSet(bitSet.cardinality());
    SparseBitSets.iterator(bitSet).forEachRemaining((IntConsumer) result::add);

    IntSet expected = new IntOpenHashSet(ints);

    assertThat(result, is(expected));
  }
}
