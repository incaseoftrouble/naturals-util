// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.unionfind;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class IntMapUnionFind implements IntUnionFind {
    private int elements;
    private final Int2IntMap parent = new Int2IntOpenHashMap();
    private final Int2IntMap size = new Int2IntOpenHashMap();

    public IntMapUnionFind() {
        this(0);
    }

    public IntMapUnionFind(int size) {
        elements = size;
        parent.defaultReturnValue(-1);
        this.size.defaultReturnValue(1);
    }

    @Override
    public void add(int num) {
        elements += num;
    }

    @Override
    public int componentCount() {
        // Only non-roots ever get a parent entry, so their count is exactly the number of elements
        // that have been merged into some other root.
        return elements - parent.size();
    }

    @Override
    public int find(int p) {
        assert isValid(p);

        // Search parent
        int root = p;
        while (true) {
            int next = parent.get(root);
            if (next == -1) {
                break;
            }
            root = next;
        }

        // Compress path
        int current = p;
        while (current != root) {
            current = parent.put(current, root);
        }
        return root;
    }

    private boolean isValid(int p) {
        return 0 <= p && p < size();
    }

    @Override
    public int size() {
        return elements;
    }

    @Override
    public void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);
        if (rootP == rootQ) {
            return;
        }

        // make smaller root point to larger one
        int sizeP = size.get(rootP);
        int sizeQ = size.get(rootQ);
        if (sizeP < sizeQ) {
            parent.put(rootP, rootQ);
            size.put(rootQ, sizeP + sizeQ);
            if (sizeP > 1) {
                assert size.containsKey(rootP);
                size.remove(rootP);
            }
        } else {
            parent.put(rootQ, rootP);
            size.put(rootP, sizeP + sizeQ);
            if (sizeQ > 1) {
                assert size.containsKey(rootQ);
                size.remove(rootQ);
            }
        }
        assert parent.int2IntEntrySet().stream().noneMatch(e -> e.getIntKey() == e.getIntValue());
    }
}
