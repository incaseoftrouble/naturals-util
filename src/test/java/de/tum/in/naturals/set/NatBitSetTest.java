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
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;


@SuppressWarnings("MagicNumber")
class NatBitSetTest {
  @Test
  void testCreateBoundedLongSet() {
    BoundedNatBitSet ints = NatBitSets.boundedLongSet(10);
    assertThat(ints.domainSize(), is(10));
    assertThat(NatBitSets.isModifiable(ints, 10), is(true));
    assertThat(NatBitSets.isModifiable(ints, 11), is(false));
    assertThat(NatBitSets.isModifiable(ints.complement(), 10), is(true));
    assertThat(NatBitSets.isModifiable(ints.complement(), 11), is(false));
    ints.set(0, 10);
  }

  @Test
  void testCreateBoundedSet() {
    BoundedNatBitSet ints = NatBitSets.boundedSet(10);
    assertThat(ints.domainSize(), is(10));
    assertThat(NatBitSets.isModifiable(ints, 10), is(true));
    assertThat(NatBitSets.isModifiable(ints, 11), is(false));
    assertThat(NatBitSets.isModifiable(ints.complement(), 10), is(true));
    assertThat(NatBitSets.isModifiable(ints.complement(), 11), is(false));
    ints.set(0, 10);
  }

  @Test
  void testCreateFullSet() {
    BoundedNatBitSet ints = NatBitSets.boundedFullSet(500);
    assertThat(ints, instanceOf(FixedSizeNatBitSet.class));
    assertThat(NatBitSets.isModifiable(ints, 500), is(false));
    assertThat(NatBitSets.isModifiable(ints.complement(), 500), is(false));
    assertThat(ints.domainSize(), is(500));

    assertThat(ints.firstInt(), is(0));
    assertThat(ints.lastInt(), is(499));

    assertThat(ints.complement().isEmpty(), is(true));
  }

  @Test
  void testCreateSimpleSet() {
    NatBitSet ints = NatBitSets.simpleSet();
    assertThat(ints, instanceOf(SimpleNatBitSet.class));
    assertThat(NatBitSets.isModifiable(ints), is(true));
    ints.set(0, 10);
  }

  @Test
  void testCreateSimpleSetWithExpectedSize() {
    NatBitSet ints = NatBitSets.simpleSet(10);
    assertThat(ints, instanceOf(SimpleNatBitSet.class));
    assertThat(NatBitSets.isModifiable(ints), is(true));
    ints.set(0, 10);
  }

  @Test
  void testCreateSingleton() {
    NatBitSet singleton = NatBitSets.singleton(1);
    assertThat(singleton, instanceOf(MutableSingletonNatBitSet.class));
    assertThat(NatBitSets.isModifiable(singleton), is(false));
    assertThat(NatBitSets.isModifiable(singleton, 1), is(false));

    assertThat(singleton.size(), is(1));
    assertThat(singleton, contains(1));
    assertThat(singleton.firstInt(), is(1));
    assertThat(singleton.lastInt(), is(1));
  }

  @Test
  void testCreateSparseSet() {
    NatBitSet ints = NatBitSets.sparseSet();
    assertThat(ints, instanceOf(SparseNatBitSet.class));
    assertThat(NatBitSets.isModifiable(ints), is(true));
    ints.set(0, 10);
  }

  @Test
  void testCreateSparseSetWithExpectedSize() {
    NatBitSet ints = NatBitSets.sparseSet(10);
    assertThat(ints, instanceOf(SparseNatBitSet.class));
    assertThat(NatBitSets.isModifiable(ints), is(true));
    ints.set(0, 10);
  }

  @Test
  void testSingleton() {
    NatBitSet singleton = NatBitSets.singleton(1);
    assertThat(singleton.size(), is(1));
    assertThat(singleton, contains(1));

    singleton.clear(0, 10);
    assertThat(singleton.isEmpty(), is(true));
    singleton.set(0);
    assertThat(singleton.size(), is(1));
    assertThat(singleton, contains(0));

    singleton.flip(0);
    singleton.flip(1, 2);
    assertThat(singleton, contains(1));

    assertThat(singleton.nextPresentIndex(0), is(1));
    assertThat(singleton.nextAbsentIndex(0), is(0));
    assertThat(singleton.nextPresentIndex(1), is(1));
    assertThat(singleton.nextAbsentIndex(1), is(2));
    assertThat(singleton.nextPresentIndex(2), is(-1));
    assertThat(singleton.nextAbsentIndex(2), is(2));

    singleton.clear();
    assertThat(singleton.nextPresentIndex(0), is(-1));
  }
}
