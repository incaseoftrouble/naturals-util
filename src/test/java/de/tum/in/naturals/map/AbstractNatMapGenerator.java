// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.map;

import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestMapGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractNatMapGenerator<V> implements TestMapGenerator<Integer, V> {
    abstract V[] sampleValues();

    abstract Map<Integer, V> map();

    @Override
    public SampleElements<Map.Entry<Integer, V>> samples() {
        V[] samples = sampleValues();
        assert samples.length == 5;
        return new SampleElements<>(
                Map.entry(1, samples[0]),
                Map.entry(5, samples[1]),
                Map.entry(123, samples[2]),
                Map.entry(2, samples[3]),
                Map.entry(12, samples[4]));
    }

    @Override
    public Map<Integer, V> create(Object... elements) {
        Map<Integer, V> map = map();
        for (Object o : elements) {
            @SuppressWarnings("unchecked")
            Map.Entry<Integer, V> e = (Map.Entry<Integer, V>) o;
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    @Override
    public Iterable<Map.Entry<Integer, V>> order(List<Map.Entry<Integer, V>> insertionOrder) {
        List<Map.Entry<Integer, V>> list = new ArrayList<>(insertionOrder);
        list.sort(Map.Entry.comparingByKey());
        return list;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<Integer, V>[] createArray(int length) {
        return new Map.Entry[length];
    }

    @Override
    public Integer[] createKeyArray(int length) {
        return new Integer[length];
    }

    @SuppressWarnings("unchecked")
    @Override
    public V[] createValueArray(int length) {
        return (V[]) new Object[length];
    }
}
