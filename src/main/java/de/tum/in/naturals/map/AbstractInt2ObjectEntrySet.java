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
