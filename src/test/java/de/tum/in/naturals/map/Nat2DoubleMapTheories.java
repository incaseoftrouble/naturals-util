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

package de.tum.in.naturals.map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import it.unimi.dsi.fastutil.ints.AbstractInt2DoubleMap.BasicEntry;
import it.unimi.dsi.fastutil.ints.Int2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({"MagicNumber", "StaticCollection", "NewClassNamingConvention"})
class Nat2DoubleMapTheories {
  private static final int MAXIMAL_KEY = 100;
  private static final int MAXIMAL_MODIFICATIONS = 2000;
  private static final int NUMBER_OF_LARGE_TESTS = 200;
  private static final int NUMBER_OF_SMALL_TESTS = 200;
  private static final List<List<Consumer<Int2DoubleMap>>> actions;
  private static final Random generator = new Random(10L);
  private static final List<Supplier<Int2DoubleMap>> implementations;

  static {
    // CSOFF: Indentation
    actions = Stream.concat(
        IntStream.range(0, NUMBER_OF_SMALL_TESTS)
            .map(k -> generator.nextInt(MAXIMAL_MODIFICATIONS))
            .mapToObj(length -> IntStream.range(0, length)
                .mapToObj(k -> generateAction(
                    () -> generator.nextInt(10), () -> (double) generator.nextInt(20)))
                .collect(Collectors.toList())),
        IntStream.range(0, NUMBER_OF_LARGE_TESTS)
            .map(k -> generator.nextInt(MAXIMAL_MODIFICATIONS))
            .mapToObj(length -> IntStream.range(0, length)
                .mapToObj(k -> generateAction(
                    () -> generator.nextInt(MAXIMAL_KEY), generator::nextDouble))
                .collect(Collectors.toList()))
    ).collect(Collectors.toList());
    // CSON

    implementations = Arrays.asList(Int2DoubleSortedArrayMap::new,
        () -> new Int2DoubleSortedArrayMap(MAXIMAL_KEY),
        () -> new Nat2DoubleDenseArrayMap(MAXIMAL_KEY));
  }

  @SuppressWarnings("MagicNumber")
  private static Consumer<Int2DoubleMap> generateAction(IntSupplier keySupplier, DoubleSupplier valueSupplier) {
    int actionId = generator.nextInt(84);
    if (actionId < 30) {
      return new Put(keySupplier.getAsInt(), valueSupplier.getAsDouble());
    }
    if (actionId < 40) {
      return new RemoveKey(keySupplier.getAsInt());
    }
    if (actionId < 45) {
      return new KeySetRemove(keySupplier.getAsInt());
    }
    if (actionId < 55) {
      int putAllSize = generator.nextInt(20);
      Int2DoubleMap putAll = new Int2DoubleOpenHashMap(putAllSize);
      for (int i = 0; i < putAllSize; i++) {
        putAll.put(keySupplier.getAsInt(), valueSupplier.getAsDouble());
      }
      return new PutAll(putAll);
    }
    if (actionId < 60) {
      return new ValuesRemove(valueSupplier.getAsDouble());
    }
    if (actionId < 70) {
      return new Remove(keySupplier.getAsInt(), valueSupplier.getAsDouble());
    }
    if (actionId < 80) {
      return new EntrySetRemove(keySupplier.getAsInt(), valueSupplier.getAsDouble());
    }
    if (actionId < 82) {
      return new DefaultReturnValue(valueSupplier.getAsDouble());
    }
    return new Clear();
  }

  public static Stream<Arguments> arguments() {
    return implementations.stream().flatMap(implementation ->
        actions.stream().map(action -> Arguments.of(implementation, action)));
  }

  @SuppressWarnings("MethodMayBeStatic")
  private void checkEquality(Int2DoubleMap actual, Int2DoubleMap expected) {
    assertThat(actual, is(expected));
    assertThat(actual.keySet(), is(expected.keySet()));
    assertThat(actual.values(), is(expected.values()));
    assertThat(actual.int2DoubleEntrySet(), is(expected.int2DoubleEntrySet()));

    for (int i = 0; i < 100; i++) {
      int key = generator.nextInt(MAXIMAL_KEY);
      assertThat(actual.get(key), is(expected.get(key)));
      assertThat(actual.containsKey(key), is(expected.containsKey(key)));
    }
  }

  @ParameterizedTest
  @MethodSource("arguments")
  void testImplementation(Supplier<Int2DoubleMap> implementation, Iterable<Consumer<Int2DoubleMap>> sequence) {
    Int2DoubleMap map = implementation.get();
    Int2DoubleMap reference = new Int2DoubleAVLTreeMap();

    for (Consumer<Int2DoubleMap> action : sequence) {
      action.accept(map);
      action.accept(reference);
      if (generator.nextInt(10) == 0) {
        checkEquality(map, reference);
      }
    }
    checkEquality(map, reference);
  }

  private static class Clear implements Consumer<Int2DoubleMap> {
    @Override
    public void accept(Int2DoubleMap map) {
      map.clear();
    }

    @Override
    public String toString() {
      return "clear";
    }
  }

  private static class DefaultReturnValue implements Consumer<Int2DoubleMap> {
    final double drv;

    public DefaultReturnValue(double drv) {
      this.drv = drv;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.defaultReturnValue(drv);
    }

    @Override
    public String toString() {
      return String.format("drv{%s}", drv);
    }
  }

  private static class EntrySetRemove implements Consumer<Int2DoubleMap> {
    final int key;
    final double value;

    public EntrySetRemove(int key, double value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.int2DoubleEntrySet().remove(new BasicEntry(key, value));
    }

    @Override
    public String toString() {
      return String.format("remove{%d,%s}", key, value);
    }
  }

  private static class KeySetRemove implements Consumer<Int2DoubleMap> {
    final int key;

    public KeySetRemove(int key) {
      this.key = key;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.keySet().remove(key);
    }

    @Override
    public String toString() {
      return String.format("keyRemove{%d}", key);
    }
  }

  private static class Put implements Consumer<Int2DoubleMap> {
    final int key;
    final double value;

    public Put(int key, double value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.put(key, value);
    }

    @Override
    public String toString() {
      return String.format("put{%d,%s}", key, value);
    }
  }

  private static class PutAll implements Consumer<Int2DoubleMap> {
    final Int2DoubleMap map;

    public PutAll(Int2DoubleMap map) {
      this.map = map;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.putAll(this.map);
    }

    @Override
    public String toString() {
      return String.format("putAll{%s}", map);
    }
  }

  private static class Remove implements Consumer<Int2DoubleMap> {
    final int key;
    final double value;

    public Remove(int key, double value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.remove(key, value);
    }

    @Override
    public String toString() {
      return String.format("remove{%d,%s}", key, value);
    }
  }

  private static class RemoveKey implements Consumer<Int2DoubleMap> {
    final int key;

    RemoveKey(int key) {
      this.key = key;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.remove(key);
    }

    @Override
    public String toString() {
      return String.format("remove{%d}", key);
    }
  }

  private static class ValuesRemove implements Consumer<Int2DoubleMap> {
    final double value;

    public ValuesRemove(double value) {
      this.value = value;
    }

    @Override
    public void accept(Int2DoubleMap map) {
      map.values().rem(value);
    }

    @Override
    public String toString() {
      return String.format("valueRemove{%s}", value);
    }
  }
}
