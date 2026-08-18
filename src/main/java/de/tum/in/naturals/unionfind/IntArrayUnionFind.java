// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.unionfind;

import java.util.Arrays;

public class IntArrayUnionFind implements IntUnionFind {
    private int componentCount;
    private int[] parent;
    private int[] size;

    public IntArrayUnionFind() {
        this(32);
    }

    public IntArrayUnionFind(int initialSize) {
        parent = new int[initialSize];
        this.size = new int[initialSize];
        componentCount = initialSize;
        for (int i = 0; i < initialSize; i++) {
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
        for (int i = currentSize; i < newSize; i++) {
            parent[i] = i;
        }
        size = Arrays.copyOf(size, newSize);
        Arrays.fill(size, currentSize, newSize, 1);
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
