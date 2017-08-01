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
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import java.util.Map;

public abstract class BasicInt2DoubleEntry implements Int2DoubleMap.Entry {
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Map.Entry)) {
      return false;
    }
    if (o instanceof Int2DoubleMap.Entry) {
      Int2DoubleMap.Entry other = (Int2DoubleMap.Entry) o;
      return getIntKey() == other.getIntKey() && Double.doubleToRawLongBits(getDoubleValue())
          == Double.doubleToRawLongBits(other.getDoubleValue());
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
    return getIntKey() == k
        && Double.doubleToRawLongBits(getDoubleValue()) == Double.doubleToRawLongBits(v);
  }

  @Deprecated
  @Override
  public Integer getKey() {
    return getIntKey();
  }

  @Deprecated
  @Override
  public Double getValue() {
    return getDoubleValue();
  }

  @Override
  public int hashCode() {
    return HashCommon.mix(getIntKey()) ^ HashCommon.double2int(getDoubleValue());
  }

  @Deprecated
  @Override
  public Double setValue(Double value) {
    return setValue(value.doubleValue());
  }

  @Override
  public String toString() {
    return String.format("%d->%s", getIntKey(), getDoubleValue());
  }
}
