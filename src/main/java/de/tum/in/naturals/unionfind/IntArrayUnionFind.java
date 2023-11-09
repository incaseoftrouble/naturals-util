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

package de.tum.in.naturals.unionfind;

import java.util.Arrays;

public class IntArrayUnionFind implements IntUnionFind {
  private int componentCount;
  private int[] parent;
  private int[] size;

  public IntArrayUnionFind(int size) {
    parent = new int[size];
    this.size = new int[size];
    componentCount = size;
    for (int i = 0; i < size; i++) {
      parent[i] = i;
    }
    Arrays.fill(this.size, 1);
  }

  @Override
  public void add(int num) {
    componentCount += num;

    int currentSize = parent.length;
    int newSize = currentSize + num;
    parent = Arrays.copyOf(parent, newSize);
    size = Arrays.copyOf(size, newSize);
    for (int i = currentSize; i < newSize; i++) {
      parent[i] = i;
      this.size[i] = 1;
    }
  }

  @Override
  public int componentCount() {
    return componentCount;
  }

  @Override
  public int find(int p) {
    assert isValid(p);

    // Search parent
    int root = p;
    while (root != parent[root]) {
      root = parent[root];
    }

    // Compress path
    int current = p;
    while (current != root) {
      int newParent = parent[current];
      parent[current] = root;
      current = newParent;
    }
    return root;
  }

  private boolean isValid(int p) {
    return 0 <= p && p < size();
  }

  @Override
  public int size() {
    return parent.length;
  }

  @Override
  public void union(int p, int q) {
    int rootP = find(p);
    int rootQ = find(q);
    if (rootP == rootQ) {
      return;
    }

    // make smaller root point to larger one
    int sizeP = size[rootP];
    int sizeQ = size[rootQ];
    if (sizeP < sizeQ) {
      parent[rootP] = rootQ;
      size[rootQ] = sizeP + sizeQ;
    } else {
      parent[rootQ] = rootP;
      size[rootP] = sizeP + sizeQ;
    }
    componentCount--;
  }
}


