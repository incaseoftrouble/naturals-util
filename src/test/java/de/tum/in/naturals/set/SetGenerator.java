// SPDX-License-Identifier: Apache-2.0

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
