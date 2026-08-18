// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.map;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.FastEntrySet;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import java.util.Map;

public abstract class AbstractInt2ObjectEntrySet<V, M extends Int2ObjectMap<V>>
        extends AbstractObjectSet<Int2ObjectMap.Entry<V>> implements FastEntrySet<V> {
    protected final M map;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public AbstractInt2ObjectEntrySet(M map) {
        this.map = map;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AbstractInt2ObjectEntrySet<V, M> clone() throws CloneNotSupportedException {
        return this;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
        Object value = e.getValue();
        if (value == null) {
            return false;
        }

        Object key = e.getKey();
        if (!(key instanceof Integer)) {
            return false;
        }

        int k = (Integer) key;
        V v = map.get(k);
        return value.equals(v);
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        if (o instanceof Entry) {
            Entry<?> e = (Entry<?>) o;
            return map.remove(e.getIntKey(), e.getValue());
        }

        Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
        Object key = e.getKey();
        if (!(key instanceof Integer)) {
            return false;
        }

        int k = (Integer) key;
        return map.remove(k, e.getValue());
    }

    @Override
    public int size() {
        return map.size();
    }
}
