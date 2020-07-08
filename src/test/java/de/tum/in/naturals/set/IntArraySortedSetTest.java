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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

public class IntArraySortedSetTest {
  @Test
  public void testEmpty() {
    Consumer<IntSet> tester = set -> {
      assertThat(set, empty());
      assertThat(set.size(), is(0));
      assertThat(set, iterableWithSize(0));
    };

    IntSet defaultConstructor = new IntArraySortedSet();
    tester.accept(defaultConstructor);

    IntSet emptyArrayConstructor = new IntArraySortedSet(IntArrays.EMPTY_ARRAY);
    tester.accept(emptyArrayConstructor);

    IntSet emptyCollectionConstructor = new IntArraySortedSet(IntSets.EMPTY_SET);
    tester.accept(emptyCollectionConstructor);
  }

  @Test
  public void testModify() {
    IntSet set = new IntArraySortedSet();

    assertThat(set.add(1), is(true));
    assertThat(set.add(2), is(true));
    assertThat(set.add(0), is(true));

    assertThat(set, contains(0, 1, 2));

    assertThat(set.remove(1), is(true));
    assertThat(set.add(4), is(true));
    assertThat(set.remove(2), is(true));
    assertThat(set.remove(2), is(false));

    assertThat(set, contains(0, 4));

    set.clear();

    assertThat(set, empty());

    assertThat(set.add(Integer.MAX_VALUE), is(true));
    assertThat(set.add(Integer.MAX_VALUE), is(false));
    assertThat(set.add(Integer.MIN_VALUE), is(true));
    assertThat(set.add(Integer.MIN_VALUE), is(false));

    for (int i = 0; i < 400; i++) {
      assertThat(set.add(i), is(true));
    }

    assertThat(set, hasSize(402));

    for (int i = 399; i >= 0; i--) {
      assertThat(set.remove(i), is(true));
    }

    assertThat(set, hasSize(2));
  }
}
