// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Collection;
import javax.annotation.Nonnegative;

public abstract class AbstractBoundedNatBitSet extends AbstractNatBitSet implements BoundedNatBitSet {
    @Nonnegative
    private final int domainSize;

    protected AbstractBoundedNatBitSet(@Nonnegative int domainSize) {
        this.domainSize = domainSize;
    }

    @Override
    @Nonnegative
    public int domainSize() {
        return domainSize;
    }

    @Override
    public boolean add(int index) {
        checkInDomain(index);
        return super.add(index);
    }

    @Override
    public boolean addAll(Collection<? extends Integer> c) {
        return super.addAll(c);
    }

    @Override
    public void clearFrom(int from) {
        if (from >= domainSize) {
            return;
        }
        clear(Math.max(0, from), domainSize);
    }

    @Override
    public void orNot(IntCollection indices) {
        if (indices.isEmpty()) {
            set(0, domainSize);
        } else {
            IntSet reference;
            if (indices instanceof IntSet) {
                reference = (IntSet) indices;
            } else {
                reference = new IntOpenHashSet();
                indices.forEach(i -> {
                    if (0 <= i && i < domainSize) {
                        reference.add(i);
                    }
                });
            }
            for (int i = 0; i < domainSize(); i++) {
                if (!reference.contains(i)) {
                    set(i);
                }
            }
        }
    }

    @Override
    public boolean intersects(Collection<Integer> indices) {
        if (isEmpty() || indices.isEmpty()) {
            return false;
        }
        if (indices instanceof IntSortedSet || indices instanceof NatBitSet) {
            IntIterator iterator = ((IntSet) indices).intIterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (contains(index)) {
                    return true;
                }
                if (index >= domainSize()) {
                    return false;
                }
            }
            return false;
        }
        return super.intersects(indices);
    }

    @Override
    public AbstractBoundedNatBitSet clone() {
        return (AbstractBoundedNatBitSet) super.clone();
    }

    @Override
    public String toString() {
        return domainSize + super.toString();
    }

    protected boolean inDomain(int index) {
        return 0 <= index && index < domainSize;
    }

    protected void checkInDomain(int from, int to) {
        NatBitSetsUtil.checkInDomain(domainSize, from, to);
    }

    protected void checkInDomain(int index) {
        NatBitSetsUtil.checkInDomain(domainSize, index);
    }
}
