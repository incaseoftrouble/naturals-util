// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.map;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2IntMap.FastEntrySet;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import java.util.Map;

public abstract class AbstractInt2IntEntrySet<M extends Int2IntMap> extends AbstractObjectSet<Entry>
        implements FastEntrySet {
    protected final M map;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public AbstractInt2IntEntrySet(M map) {
        this.map = map;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AbstractInt2IntEntrySet<M> clone() throws CloneNotSupportedException {
        return this;
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

        Object key = e.getKey();
        if (!(key instanceof Integer)) {
            return false;
        }

        Object value = e.getValue();
        if (!(value instanceof Integer)) {
            return false;
        }

        int k = (Integer) key;
        int v = (Integer) value;
        return map.containsKey(k) && map.get(k) == v;
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }

        if (o instanceof Entry) {
            Entry e = (Entry) o;
            return map.remove(e.getIntKey(), e.getIntValue());
        }

        Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
        Object key = e.getKey();
        if (!(key instanceof Integer)) {
            return false;
        }

        Object value = e.getValue();
        if (!(value instanceof Integer)) {
            return false;
        }

        int k = (Integer) key;
        int v = (Integer) value;
        return map.remove(k, v);
    }

    @Override
    public int size() {
        return map.size();
    }
}
