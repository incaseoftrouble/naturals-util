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

package de.tum.in.naturals;

import it.unimi.dsi.fastutil.doubles.Double2IntFunction;
import it.unimi.dsi.fastutil.doubles.Double2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.PrimitiveIterator;
import java.util.function.DoubleToIntFunction;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nonnegative;

@SuppressWarnings("TypeMayBeWeakened")
public final class Indices {
  private Indices() {}

  public static IntUnaryOperator elementToIndexMap(IntCollection ints) {
    return elementToIndexMap(ints.iterator());
  }

  public static IntUnaryOperator elementToIndexMap(PrimitiveIterator.OfInt iterator) {
    Int2IntFunction indexMap = new Int2IntLinkedOpenHashMap();
    indexMap.defaultReturnValue(-1);
    int index = 0;
    while (iterator.hasNext()) {
      int next = iterator.nextInt();
      assert !indexMap.containsKey(next);
      indexMap.put(next, index);
      index += 1;
    }
    return element -> {
      assert indexMap.containsKey(element);
      return indexMap.get(element);
    };
  }

  public static DoubleToIntFunction elementToIndexMap(DoubleCollection doubles) {
    return elementToIndexMap(doubles.iterator());
  }

  public static DoubleToIntFunction elementToIndexMap(PrimitiveIterator.OfDouble iterator) {
    Double2IntFunction indexMap = new Double2IntLinkedOpenHashMap();
    indexMap.defaultReturnValue(-1);
    int index = 0;
    while (iterator.hasNext()) {
      double next = iterator.nextDouble();
      assert !indexMap.containsKey(next);
      indexMap.put(next, index);
      index += 1;
    }
    return element -> {
      assert indexMap.containsKey(element);
      return indexMap.get(element);
    };
  }

  /**
   * Returns an array containing the indices of all {@code true} entries in the {@code elements}
   * array. For example, given {@code [true, false, false, true, true, false]}, the method returns
   * the array {@code [0, 3, 4]}.
   */
  public static int[] indexMap(boolean[] elements) {
    int elementCount = 0;
    for (boolean element : elements) {
      if (element) {
        elementCount += 1;
      }
    }

    if (elementCount == 0) {
      //noinspection AssignmentOrReturnOfFieldWithMutableType
      return IntArrays.EMPTY_ARRAY;
    }
    int[] indexArray = new int[elementCount];
    int index = 0;
    for (int globalIndex = 0; globalIndex < elements.length; globalIndex++) {
      if (elements[globalIndex]) {
        indexArray[index] = globalIndex;
        index += 1;
      }
    }
    return indexArray;
  }

  public static <E> void forEachIndexed(Iterable<E> elements, IndexConsumer<E> action) {
    int index = 0;
    for (E element : elements) {
      action.accept(index, element);
      index += 1;
    }
  }

  public static void forEachIndexed(IntIterable elements, IntIndexConsumer action) {
    int index = 0;
    IntIterator iterator = elements.iterator();
    while (iterator.hasNext()) {
      action.accept(index, iterator.nextInt());
      index += 1;
    }
  }

  @FunctionalInterface
  public interface IndexConsumer<E> {
    void accept(@Nonnegative int index, E element);
  }

  @FunctionalInterface
  public interface IntIndexConsumer {
    void accept(@Nonnegative int index, int element);
  }
}
