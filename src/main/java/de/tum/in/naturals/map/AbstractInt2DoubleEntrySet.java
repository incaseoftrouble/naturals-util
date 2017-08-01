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

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.FastEntrySet;
import it.unimi.dsi.fastutil.objects.AbstractObjectSet;
import java.util.Map;

public abstract class AbstractInt2DoubleEntrySet<M extends Int2DoubleMap>
    extends AbstractObjectSet<Entry> implements FastEntrySet {
  protected final M map;

  public AbstractInt2DoubleEntrySet(M map) {
    this.map = map;
  }

  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @Override
  public AbstractInt2DoubleEntrySet<M> clone() throws CloneNotSupportedException {
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
    if (!(value instanceof Double)) {
      return false;
    }

    int k = (Integer) key;
    double v = (Double) value;
    return map.containsKey(k)
        && Double.doubleToLongBits(map.get(k)) == Double.doubleToLongBits(v);
  }

  @Override
  public boolean remove(Object o) {
    if (!(o instanceof Map.Entry)) {
      return false;
    }

    if (o instanceof Int2DoubleMap.Entry) {
      Entry e = (Entry) o;
      return map.remove(e.getIntKey(), e.getDoubleValue());
    }

    Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;

    Object key = e.getKey();
    if (!(key instanceof Integer)) {
      return false;
    }

    Object value = e.getValue();
    if (!(value instanceof Double)) {
      return false;
    }

    int k = (Integer) key;
    double v = (Double) value;
    return map.remove(k, v);
  }

  @Override
  public int size() {
    return map.size();
  }
}
