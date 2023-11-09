/*
 * Copyright (C) 2022 Tobias Meggendorfer
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

import com.google.common.collect.testing.TestIntegerSetGenerator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class SetGenerator extends TestIntegerSetGenerator {
    private final Supplier<? extends IntSet> constructor;

    public SetGenerator(Supplier<? extends IntSet> constructor) {
        this.constructor = constructor;
    }

    @Override
    protected Set<Integer> create(Integer[] elements) {
        IntSet set = constructor.get();
        set.addAll(Arrays.asList(elements));
        return set;
    }

    @Override
    public List<Integer> order(List<Integer> insertionOrder) {
        return insertionOrder.stream().sorted().collect(Collectors.toList());
    }
}
