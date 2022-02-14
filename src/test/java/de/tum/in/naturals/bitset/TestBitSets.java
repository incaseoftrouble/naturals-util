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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import java.util.BitSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TestBitSets {
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
}
