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

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import java.util.Map;

public abstract class BasicInt2IntEntry implements Int2IntMap.Entry {
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Map.Entry)) {
      return false;
    }
    if (o instanceof Int2IntMap.Entry) {
      Int2IntMap.Entry other = (Int2IntMap.Entry) o;
      return getIntKey() == other.getIntKey() && getIntValue() == other.getIntValue();
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
    return getIntKey() == k && getIntValue() == v;
  }

  @Deprecated
  @Override
  public Integer getKey() {
    return getIntKey();
  }

  @Deprecated
  @Override
  public Integer getValue() {
    return getIntValue();
  }

  @Override
  public int hashCode() {
    return HashCommon.mix(getIntKey()) ^ HashCommon.mix(getIntValue());
  }

  @Deprecated
  @Override
  public Integer setValue(Integer value) {
    return setValue(value.intValue());
  }

  @Override
  public String toString() {
    return String.format("%d->%d", getIntKey(), getIntValue());
  }
}
