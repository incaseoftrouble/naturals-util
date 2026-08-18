// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import static de.tum.in.naturals.set.NatBitSetsUtil.checkNonNegative;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkOrdered;
import static de.tum.in.naturals.set.NatBitSetsUtil.checkRange;

import de.tum.in.naturals.Arrays2;
import de.tum.in.naturals.bitset.BitSets;
import de.tum.in.naturals.bitset.RoaringBitmaps;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import org.roaringbitmap.PeekableIntIterator;
import org.roaringbitmap.RoaringBitmap;

class HybridNatBitSet extends AbstractNatBitSet {
    /**
     * Up to this many elements an array wins on both axes that matter: it iterates roughly an order of
     * magnitude faster than a bitmap of the same content, and - with the search below - probes faster
     * too, while staying within a few hundred bytes of the smallest alternative.
     */
    static final int MAXIMAL_ARRAY_SIZE = 64;

    /**
     * How much larger than the smallest option a representation may be before its speed stops being
     * worth it. Speed comes first here, but not at any price.
     */
    private static final int MEMORY_SLACK = 4;

    private static final int SIZE_UNKNOWN = -1;

    private static final long INFINITY = Integer.MAX_VALUE + 1L;
    private static final int[] EMPTY_ARRAY = {};
    private static final int ROARING_FLOOR = 150;
    private static final int ROARING_CONTAINER = 40;
    private static final int MINIMAL_RECLAIMED_WORDS = 8;

    private enum Mode {
        ARRAY,
        BIT_SET,
        ROARING
    }

    // Sorted int[], BitSet, or RoaringBitmap
    private Object store;
    // Sometimes lazy (== SIZE_UNKNOWN)
    private int size;

    HybridNatBitSet() {
        this.store = EMPTY_ARRAY;
        this.size = 0;
    }

    HybridNatBitSet(int[] elements, int size) {
        assert 0 <= size && size <= elements.length && isSorted(elements, size);
        this.store = elements;
        this.size = size;
    }

    HybridNatBitSet(BitSet bitSet) {
        this(bitSet, SIZE_UNKNOWN);
    }

    HybridNatBitSet(BitSet bitSet, int size) {
        assert size == SIZE_UNKNOWN || bitSet.cardinality() == size;
        this.store = bitSet;
        this.size = size;
    }

    HybridNatBitSet(RoaringBitmap bitmap) {
        this(bitmap, SIZE_UNKNOWN);
    }

    HybridNatBitSet(RoaringBitmap bitmap, int size) {
        assert size == SIZE_UNKNOWN || bitmap.getCardinality() == size;
        this.store = bitmap;
        this.size = size;
    }

    HybridNatBitSet(HybridNatBitSet other) {
        Object otherStore = other.store;
        if (otherStore instanceof int[]) {
            this.store = ((int[]) otherStore).clone();
        } else if (otherStore instanceof BitSet) {
            this.store = ((BitSet) otherStore).clone();
        } else {
            this.store = ((RoaringBitmap) otherStore).clone();
        }
        this.size = other.size;
    }

    static HybridNatBitSet forShape(int expectedCardinality, int expectedLast) {
        switch (idealModeEstimate(expectedCardinality, expectedLast)) {
            case ARRAY:
                return new HybridNatBitSet(new int[expectedCardinality], 0);
            case BIT_SET:
                // The hint may sit at the very top of the range, where one past it no longer fits
                return new HybridNatBitSet(
                        new BitSet(expectedLast == Integer.MAX_VALUE ? expectedLast : expectedLast + 1), 0);
            default:
                return new HybridNatBitSet(new RoaringBitmap(), 0);
        }
    }

    private static boolean isSorted(int[] elements, int size) {
        for (int i = 1; i < size; i++) {
            if (elements[i - 1] >= elements[i]) {
                return false;
            }
        }
        return size == 0 || elements[0] >= 0;
    }

    static long arrayBytes(int cardinality) {
        return 4L * cardinality;
    }

    static long bitSetBytes(int last) {
        // Words are allocated whole, so anything present costs at least one of them
        return 8L * ((last >>> 6) + 1L);
    }

    static long roaringBytes(int cardinality, int last, long runs) {
        long containers = Math.min(cardinality, ((long) last >>> 16) + 1L);
        return ROARING_FLOOR + ROARING_CONTAINER * containers + Math.min(2L * cardinality, 4L * runs);
    }

    private static long runBound(int cardinality, int last) {
        // The cardinality may be an over-estimate that exceeds the span, and a negative run count would
        // make a bitmap look cheaper than an empty one - anything present has at least one run
        return Math.max(1L, last + 2L - cardinality);
    }

    private static boolean preferRoaringOverBitSet(int cardinality, int last, long runs) {
        return MEMORY_SLACK * roaringBytes(cardinality, last, runs) < bitSetBytes(last);
    }

    private static boolean preferRoaringOverBitSet(int cardinality, int last) {
        return preferRoaringOverBitSet(cardinality, last, runBound(cardinality, last));
    }

    private static boolean preferRoaringOverWords(BitSet bitSet, int cardinality, int last) {
        long containers = Math.min(cardinality, ((long) last >>> 16) + 1L);
        long budget = bitSetBytes(last) / MEMORY_SLACK - (ROARING_FLOOR + ROARING_CONTAINER * containers);
        if (budget <= 0) {
            return false; // The words are cheaper than a bitmap holding nothing at all
        }
        // Array containers alone may already fit, in which case runs cannot make the bitmap lose. Whatever
        // they do not cover has to come out of the run term, at 4 bytes each.
        return 2L * cardinality < budget || BitSets.hasAtMostRuns(bitSet, (int) ((budget - 1) / 4));
    }

    private static Mode idealModeEstimate(int cardinality, int last) {
        return idealMode(cardinality, last, runBound(cardinality, last));
    }

    private static Mode idealMode(int cardinality, int last, long runs) {
        if (cardinality == 0) {
            return Mode.ARRAY;
        }
        if (cardinality <= MAXIMAL_ARRAY_SIZE) {
            // The same call optimize() makes: below the cap an array wins on speed, and the byte
            // difference against a handful of words is never large enough to argue with that.
            return Mode.ARRAY;
        }
        // A BitSet's size depends only on the span (last), not on how many bits are actually set; Roaring's
        // size tracks the content (cardinality) instead, since it stores only what's present. So we compare
        // density: is the set dense enough over its span for BitSet's cheap word-at-a-time scan to be worth
        // its larger footprint?
        // MEMORY_SLACK biases that test toward BitSet, letting it cost up to that many times as much memory,
        // because its scan still beats walking Roaring's containers even then. The resulting threshold lands
        // near one set bit per word - unsurprising since that is also where scanning word-at-a-time overtakes
        // visiting one element at a time.
        return bitSetBytes(last) <= MEMORY_SLACK * roaringBytes(cardinality, last, runs) ? Mode.BIT_SET : Mode.ROARING;
    }

    // Accessors

    private Mode mode() {
        Object store = this.store;
        if (store instanceof int[]) {
            return Mode.ARRAY;
        }
        return store instanceof BitSet ? Mode.BIT_SET : Mode.ROARING;
    }

    @Override
    public boolean isEmpty() {
        Object store = this.store;
        if (store instanceof int[]) {
            return size == 0;
        }
        return store instanceof BitSet ? ((BitSet) store).isEmpty() : ((RoaringBitmap) store).isEmpty();
    }

    @Override
    public int size() {
        int size = this.size;
        if (size != SIZE_UNKNOWN) {
            return size;
        }
        Object store = this.store;
        // Array always tracks its size
        assert !(store instanceof int[]);
        int cardinality =
                store instanceof BitSet ? ((BitSet) store).cardinality() : ((RoaringBitmap) store).getCardinality();
        this.size = cardinality;
        return cardinality;
    }

    @Override
    public boolean contains(int index) {
        if (index < 0) {
            return false;
        }
        Object store = this.store;
        if (store instanceof int[]) {
            return Arrays2.hybridBinarySearch((int[]) store, size, index) >= 0;
        }
        return store instanceof BitSet ? ((BitSet) store).get(index) : ((RoaringBitmap) store).contains(index);
    }

    @Override
    public int firstInt() {
        Object store = this.store;
        if (store instanceof int[]) {
            if (size == 0) {
                throw new NoSuchElementException();
            }
            return ((int[]) store)[0];
        }
        if (store instanceof BitSet) {
            int first = ((BitSet) store).nextSetBit(0);
            if (first == -1) {
                throw new NoSuchElementException();
            }
            return first;
        }
        return ((RoaringBitmap) store).first();
    }

    @Override
    public int lastInt() {
        Object store = this.store;
        if (store instanceof int[]) {
            if (size == 0) {
                throw new NoSuchElementException();
            }
            return ((int[]) store)[size - 1];
        }
        if (store instanceof BitSet) {
            int last = ((BitSet) store).length() - 1;
            if (last == -1) {
                throw new NoSuchElementException();
            }
            return last;
        }
        return ((RoaringBitmap) store).last();
    }

    @Override
    public int nextPresentIndex(int index) {
        checkNonNegative(index);
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int position = Arrays2.hybridBinarySearch(array, size, index);
            if (position >= 0) {
                return array[position];
            }
            int insertion = -(position + 1);
            return insertion == size ? -1 : array[insertion];
        }
        if (store instanceof BitSet) {
            return ((BitSet) store).nextSetBit(index);
        }
        return Math.toIntExact(((RoaringBitmap) store).nextValue(index));
    }

    @Override
    public int nextAbsentIndex(int index) {
        checkNonNegative(index);
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int position = Arrays2.hybridBinarySearch(array, size, index);
            if (position < 0) {
                // index is not in array, ergo it is absent
                return index;
            }
            int candidate = index;
            while (position < size && array[position] == candidate) {
                candidate += 1;
                position += 1;
            }
            return candidate;
        }
        if (store instanceof BitSet) {
            return ((BitSet) store).nextClearBit(index);
        }
        // Roaring answers over the unsigned range and can report 2^32; BitSet.nextClearBit stays in
        // int and never fails, so saturate rather than let toIntExact throw
        long absent = ((RoaringBitmap) store).nextAbsentValue(index);
        return absent > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) absent;
    }

    @Override
    public int previousPresentIndex(int index) {
        checkNonNegative(index);
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int position = Arrays2.hybridBinarySearch(array, size, index);
            if (position >= 0) {
                return index;
            }
            int insertion = -(position + 1);
            return insertion == 0 ? -1 : array[insertion - 1];
        }
        if (store instanceof BitSet) {
            return ((BitSet) store).previousSetBit(index);
        }
        return Math.toIntExact(((RoaringBitmap) store).previousValue(index));
    }

    @Override
    public int previousAbsentIndex(int index) {
        checkNonNegative(index);
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int position = Arrays2.hybridBinarySearch(array, size, index);
            if (position < 0) {
                return index;
            }
            int candidate = index;
            while (position >= 0 && array[position] == candidate) {
                candidate -= 1;
                position -= 1;
            }
            return candidate;
        }
        if (store instanceof BitSet) {
            return ((BitSet) store).previousClearBit(index);
        }
        return Math.toIntExact(((RoaringBitmap) store).previousAbsentValue(index));
    }

    @Override
    public IntIterator iterator() {
        Object store = this.store;
        if (store instanceof int[]) {
            return new ArrayIterator(this);
        }
        return store instanceof BitSet
                ? new RemovingIterator(this, BitSets.iterator((BitSet) store))
                : RoaringBitmaps.iterator((RoaringBitmap) store);
    }

    @Override
    public void forEach(IntConsumer consumer) {
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            for (int i = 0; i < size; i++) {
                consumer.accept(array[i]);
            }
        } else if (store instanceof BitSet) {
            BitSets.forEach((BitSet) store, consumer);
        } else {
            ((RoaringBitmap) store).forEach((org.roaringbitmap.IntConsumer) consumer::accept);
        }
    }

    // Representation changes

    private void prepareFor(int extra, int newLast) {
        Object store = this.store;

        if (store instanceof RoaringBitmap) {
            // Don't "downgrade" from roaring to avoid thrashing
            return;
        }

        if (store instanceof BitSet) {
            // Writing past the allocated words is the growth point - the only place worth re-deciding.
            // Note: .size() is not .cardinality(); bitSet.size() - 1 indicates the largest element
            //   that fits in the currently allocated array.
            BitSet bitSet = (BitSet) store;
            // If we don't know last, be pessimistic
            int last = newLast == NatBitSetsUtil.UNKNOWN_LAST
                    ? (int) Math.min(Integer.MAX_VALUE, 2L * bitSet.length())
                    : Math.max(newLast, bitSet.length() - 1);
            if (last >= bitSet.size() && preferRoaringOverBitSet(size() + extra, last)) {
                this.store = toBitmap();
            }
            return;
        }
        int expectedCardinality = size + extra;
        if (expectedCardinality <= MAXIMAL_ARRAY_SIZE) {
            return;
        }
        // An empty array has no last element to extrapolate from
        int currentLast = size == 0 ? 0 : lastInt();
        int last = newLast == NatBitSetsUtil.UNKNOWN_LAST
                ? (int) Math.min(Integer.MAX_VALUE, 2L * currentLast)
                : Math.max(newLast, currentLast);
        this.store = preferRoaringOverBitSet(expectedCardinality, last) ? toBitmap() : toBitSet(last);
    }

    private BitSet toBitSet(int last) {
        Object store = this.store;
        if (store instanceof RoaringBitmap) {
            return BitSets.of((RoaringBitmap) store, last + 1);
        }
        BitSet bitSet = new BitSet(last + 1);
        int[] array = (int[]) store;
        // Only the first size entries are live - the tail holds stale values (zeroes after growth)
        for (int i = 0; i < size; i++) {
            bitSet.set(array[i]);
        }
        return bitSet;
    }

    private RoaringBitmap toBitmap() {
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            return RoaringBitmap.bitmapOf(size == array.length ? array : Arrays.copyOf(array, size));
        }
        return RoaringBitmaps.of((BitSet) store);
    }

    private int[] toArray(int cardinality) {
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            return array.length == cardinality ? array : Arrays.copyOf(array, cardinality);
        }
        int[] array = new int[cardinality];
        if (store instanceof BitSet) {
            BitSet bitSet = (BitSet) store;
            int i = 0;
            // We only go here when the cardinality is small; so we do not need run optimization
            for (int value = bitSet.nextSetBit(0); value != -1; value = bitSet.nextSetBit(value + 1)) {
                array[i] = value;
                i += 1;
            }
            assert i == cardinality;
            return array;
        }
        RoaringBitmap bitmap = (RoaringBitmap) store;
        int i = 0;
        for (int value = (int) bitmap.nextValue(0); value != -1; value = (int) bitmap.nextValue(value + 1)) {
            array[i] = value;
            i += 1;
        }
        assert i == cardinality;
        return array;
    }

    @Override
    public boolean optimize() {
        Object store = this.store;
        if (isEmpty()) {
            boolean exact = store instanceof int[] && ((int[]) store).length == 0;
            this.store = EMPTY_ARRAY;
            this.size = 0;
            return !exact;
        }
        boolean changed = store instanceof RoaringBitmap && ((RoaringBitmap) store).runOptimize();

        int cardinality = size();
        int last = lastInt();
        // Unlike the growth path this works with real numbers rather than the run bound - but it still pays
        // for them only where they can change the outcome.
        long bitSetCost = bitSetBytes(last);
        Mode mode;
        if (cardinality <= MAXIMAL_ARRAY_SIZE) {
            // At this size, Roaring has too much base overhead anyway. Only BitSet remains. It could be
            // smaller for small domains, but arrays are very fast.
            // We could compare with arrayBytes(cardinality) if needed.
            mode = Mode.ARRAY;
        } else if (store instanceof RoaringBitmap) {
            // Roaring reports its actual footprint after runOptimize, so nothing has to be estimated.
            long roaringCost = ((RoaringBitmap) store).getSizeInBytes();
            mode = bitSetCost <= MEMORY_SLACK * roaringCost ? Mode.BIT_SET : Mode.ROARING;
        } else if (store instanceof BitSet) {
            // Only words hold more than the array cap, so this is the one case that needs a run count.
            mode = preferRoaringOverWords((BitSet) store, cardinality, last) ? Mode.ROARING : Mode.BIT_SET;
        } else {
            // We are an array but over the maximal array size -- this can happen when we get wrapped
            mode = preferRoaringOverBitSet(cardinality, last) ? Mode.ROARING : Mode.BIT_SET;
        }
        if (mode == mode()) {
            if (store instanceof int[] && ((int[]) store).length > cardinality) {
                this.store = Arrays.copyOf((int[]) store, cardinality);
                return true;
            }
            if (store instanceof BitSet) {
                long neededWords = (last >>> 6) + 1L;
                long allocatedWords = ((BitSet) store).size() / Long.SIZE;
                if (allocatedWords > 2L * neededWords && allocatedWords - neededWords >= MINIMAL_RECLAIMED_WORDS) {
                    // Trim if the underlying store is way too large
                    this.store = BitSets.trimmedCopy((BitSet) store);
                    return true;
                }
            }
            return changed;
        }

        Object replacement;
        if (mode == Mode.ARRAY) {
            replacement = toArray(cardinality);
        } else if (mode == Mode.BIT_SET) {
            replacement = toBitSet(last);
        } else {
            RoaringBitmap bitmap = toBitmap();
            bitmap.runOptimize();
            replacement = bitmap;
        }
        this.store = replacement;
        this.size = cardinality;
        return true;
    }

    // Mutators

    private void setInArray(int index) {
        int[] array = (int[]) store;
        if (size == 0) {
            if (array.length == 0) {
                this.store = new int[] {index};
            } else {
                array[0] = index;
            }
            this.size = 1;
            return;
        }
        int position = Arrays2.hybridBinarySearch(array, size, index);
        if (position >= 0) {
            return;
        }
        if (size == MAXIMAL_ARRAY_SIZE) {
            int last = Math.max(array[size - 1], index);
            this.store = preferRoaringOverBitSet(size + 1, last) ? toBitmap() : toBitSet(last);
            assert size != SIZE_UNKNOWN;
            // The conversion copied the existing elements only; the recursive call accounts for the new one
            set(index);
            return;
        }

        int insertion = -(position + 1);
        int[] target = array;
        if (size == array.length) {
            target = Arrays.copyOf(array, Math.min(MAXIMAL_ARRAY_SIZE, 2 * array.length));
            this.store = target;
        }
        System.arraycopy(target, insertion, target, insertion + 1, size - insertion);
        target[insertion] = index;
        size += 1;
    }

    private void setInBitSet(int index) {
        BitSet bitSet = (BitSet) store;
        if (index >= bitSet.size()
                && preferRoaringOverBitSet(
                        size() + 1, index)) { // TODO This computes size() eagerly. Should we do this?
            RoaringBitmap bitmap = toBitmap();
            bitmap.add(index);
            this.store = bitmap;
            if (size != SIZE_UNKNOWN) {
                this.size = size + 1;
            }
            return;
        }
        if (size != SIZE_UNKNOWN && !bitSet.get(index)) {
            size += 1;
        }
        bitSet.set(index);
    }

    private void setInBitMap(int index) {
        RoaringBitmap bitmap = (RoaringBitmap) store;
        if (size == SIZE_UNKNOWN) {
            bitmap.add(index);
        } else if (bitmap.checkedAdd(index)) {
            size += 1;
        }
    }

    @Override
    public void set(int index) {
        checkNonNegative(index);
        Object store = this.store;
        if (store instanceof int[]) {
            setInArray(index);
        } else if (store instanceof BitSet) {
            setInBitSet(index);
        } else {
            setInBitMap(index);
        }
    }

    @Override
    public void set(int index, boolean value) {
        if (value) {
            set(index);
        } else {
            clear(index);
        }
    }

    @Override
    public void set(int from, int to) {
        checkRange(from, to);
        if (from == to) {
            return;
        }
        prepareFor(to - from, to - 1);

        Object store = this.store;
        if (store instanceof int[]) {
            // prepareFor already ran, so the array only survives when the whole range fits within the cap
            for (int i = from; i < to; i++) {
                set(i);
            }
            return;
        }
        if (store instanceof BitSet) {
            ((BitSet) store).set(from, to);
        } else {
            ((RoaringBitmap) store).add(from, (long) to);
        }
        this.size = SIZE_UNKNOWN;
    }

    @Override
    public void clear() {
        this.store = EMPTY_ARRAY;
        this.size = 0;
    }

    @Override
    public void clear(int index) {
        if (index < 0) {
            return;
        }
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int position = Arrays2.hybridBinarySearch(array, size, index);
            if (position >= 0) {
                System.arraycopy(array, position + 1, array, position, size - position - 1);
                size -= 1;
            }
        } else if (store instanceof BitSet) {
            BitSet bitSet = (BitSet) store;
            if (size != SIZE_UNKNOWN && bitSet.get(index)) {
                size -= 1;
            }
            bitSet.clear(index);
        } else {
            RoaringBitmap bitmap = (RoaringBitmap) store;
            if (size == SIZE_UNKNOWN) {
                bitmap.remove(index);
            } else if (bitmap.checkedRemove(index)) {
                size -= 1;
            }
        }
    }

    @Override
    public void clear(int from, int to) {
        checkOrdered(from, to);
        int start = Math.max(0, from);
        int end = Math.max(0, to);
        if (start == end || isEmpty()) {
            return;
        }
        // Both ends are cheap on every store, and a range that misses the set entirely must not cost a
        // cardinality: dropping the cached size is what makes the next size() walk the whole thing.
        if (end <= firstInt() || lastInt() < start) {
            return;
        }
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int retained = 0;
            for (int i = 0; i < size; i++) {
                int value = array[i];
                if (value < start || end <= value) {
                    array[retained] = value;
                    retained += 1;
                }
            }
            this.size = retained;
            return;
        }
        if (store instanceof BitSet) {
            ((BitSet) store).clear(start, end);
        } else {
            ((RoaringBitmap) store).remove(start, (long) end);
        }
        this.size = SIZE_UNKNOWN;
    }

    @Override
    public void clearFrom(int from) {
        if (isEmpty()) {
            return;
        }
        if (from <= 0 || from <= firstInt()) {
            clear();
            return;
        }
        if (from > lastInt()) {
            return;
        }

        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int position = Arrays2.hybridBinarySearch(array, size, from);
            this.size = position >= 0 ? position : -(position + 1);
            return;
        }
        if (store instanceof BitSet) {
            ((BitSet) store).clear(from, Integer.MAX_VALUE);
        } else {
            ((RoaringBitmap) store).remove(from, INFINITY);
        }
        this.size = SIZE_UNKNOWN;
    }

    @Override
    public void flip(int index) {
        checkNonNegative(index);
        if (contains(index)) {
            clear(index);
        } else {
            set(index);
        }
    }

    @Override
    public void flip(int from, int to) {
        checkRange(from, to);
        if (from == to) {
            return;
        }
        prepareFor(to - from, to - 1);

        Object store = this.store;
        if (store instanceof int[]) {
            for (int i = from; i < to; i++) {
                flip(i);
            }
            return;
        }
        if (store instanceof BitSet) {
            ((BitSet) store).flip(from, to);
        } else {
            ((RoaringBitmap) store).flip(from, (long) to);
        }
        this.size = SIZE_UNKNOWN;
    }

    private static IntPredicate probe(Collection<Integer> indices) {
        BitSet words = NatBitSetsUtil.words(indices);
        if (words != null) {
            return words::get;
        }
        RoaringBitmap bitmap = NatBitSetsUtil.bitmap(indices);
        if (bitmap != null) {
            return bitmap::contains;
        }
        if (indices instanceof IntSet) {
            return ((IntSet) indices)::contains;
        }
        if (indices instanceof Set<?>) {
            return indices::contains;
        }
        return new IntOpenHashSet(indices)::contains;
    }

    private IntPredicate probeThis() {
        Object store = this.store;
        if (store instanceof int[]) {
            // Hoisted deliberately - this predicate is driven once per element of the operand, and the
            // callers only read, so pinning the store costs nothing and saves a field load and a cast
            int[] array = (int[]) store;
            int size = this.size;
            return i -> Arrays2.hybridBinarySearch(array, size, i) >= 0;
        }
        if (store instanceof BitSet) {
            return ((BitSet) store)::get;
        }
        return ((RoaringBitmap) store)::contains;
    }

    /** Whether any element satisfies the given predicate. */
    private boolean anyMatch(IntPredicate predicate) {
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            for (int i = 0; i < size; i++) {
                if (predicate.test(array[i])) {
                    return true;
                }
            }
            return false;
        }
        if (store instanceof BitSet) {
            BitSet bitSet = (BitSet) store;
            for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
                if (predicate.test(i)) {
                    return true;
                }
            }
            return false;
        }
        PeekableIntIterator iterator = ((RoaringBitmap) store).getIntIterator();
        while (iterator.hasNext()) {
            if (predicate.test(iterator.next())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Estimate the cost of a linear pass over this set, in units of "one element probed".
     */
    private long scanCost() {
        Object store = this.store;
        if (store instanceof int[]) {
            return size;
        }
        if (store instanceof BitSet) {
            // Words are scanned whether they carry bits or not; length() is O(1)
            long words = ((BitSet) store).length() / Long.SIZE;
            return size == SIZE_UNKNOWN ? words : words + size;
        }
        return size == SIZE_UNKNOWN ? ((RoaringBitmap) store).getCardinality() : size;
    }

    @Override
    public boolean intersects(Collection<Integer> indices) {
        if (isEmpty() || indices.isEmpty()) {
            return false;
        }
        Object store = this.store;
        if (store instanceof BitSet) {
            BitSet otherWords = NatBitSetsUtil.words(indices);
            if (otherWords != null) {
                return ((BitSet) store).intersects(otherWords);
            }
        } else if (store instanceof RoaringBitmap) {
            RoaringBitmap otherBitmap = NatBitSetsUtil.bitmap(indices);
            if (otherBitmap != null) {
                return RoaringBitmap.intersects((RoaringBitmap) store, otherBitmap);
            }
        }

        if (store instanceof int[] || (indices instanceof IntSet && indices.size() > scanCost())) {
            return anyMatch(probe(indices));
        }
        if (indices instanceof IntCollection) {
            return IntIterators.any(((IntCollection) indices).iterator(), probeThis());
        }
        return anyMatch(probe(indices));
    }

    @Override
    public boolean containsAll(IntCollection indices) {
        if (indices.isEmpty()) {
            return true;
        }
        if (isEmpty()) {
            return false;
        }
        // Where the operand ends is O(1) on both sides, and it settles the common case of an operand that
        // reaches past this set before anything counts or walks
        int otherLast = NatBitSetsUtil.lastOf(indices);
        if (otherLast != NatBitSetsUtil.UNKNOWN_LAST && lastInt() < otherLast) {
            return false;
        }
        Object store = this.store;
        if (store instanceof BitSet) {
            BitSet otherWords = NatBitSetsUtil.words(indices);
            if (otherWords != null) {
                return BitSets.isSubset(otherWords, (BitSet) store);
            }
        } else if (store instanceof RoaringBitmap) {
            RoaringBitmap otherBitmap = NatBitSetsUtil.bitmap(indices);
            if (otherBitmap != null) {
                // Roaring answers this container by container and stops at the first miss, where an
                // intersection cardinality would visit all of both
                return ((RoaringBitmap) store).contains(otherBitmap);
            }
        }
        if (indices instanceof IntSet && size != SIZE_UNKNOWN && indices.size() > size) {
            return false;
        }
        return !IntIterators.any(indices.iterator(), probeThis().negate());
    }

    @Override
    public boolean isSubsetOf(Collection<Integer> indices) {
        if (isEmpty()) {
            return true;
        }
        if (indices.isEmpty()) {
            return false;
        }
        Object store = this.store;
        if (store instanceof BitSet) {
            BitSet otherWords = NatBitSetsUtil.words(indices);
            if (otherWords != null) {
                return BitSets.isSubset((BitSet) store, otherWords);
            }
        } else if (store instanceof RoaringBitmap) {
            RoaringBitmap otherBitmap = NatBitSetsUtil.bitmap(indices);
            if (otherBitmap != null) {
                return otherBitmap.contains((RoaringBitmap) store);
            }
        }

        // TODO Can we do this better?
        return !anyMatch(probe(indices).negate());
    }

    @Override
    public void and(IntCollection indices) {
        if (indices.isEmpty()) {
            clear();
            return;
        }
        if (indices == this) { // NOPMD - identity is the point
            return;
        }
        Object store = this.store;
        if (store instanceof int[]) {
            // An intersection can only shrink, so array mode always survives it.
            int[] array = (int[]) store;
            int retained = 0;
            for (int i = 0; i < size; i++) {
                int value = array[i];
                if (indices.contains(value)) {
                    array[retained] = value;
                    retained += 1;
                }
            }
            this.size = retained;
            return;
        }
        BitSet otherWords = NatBitSetsUtil.words(indices);
        RoaringBitmap otherBitmap = NatBitSetsUtil.bitmap(indices);

        // Direct fast-path
        if (store instanceof BitSet) {
            BitSet bitSet = (BitSet) store;
            if (otherWords != null) {
                if (otherWords != bitSet) { // NOPMD - identity is the point
                    bitSet.and(otherWords);
                    this.size = SIZE_UNKNOWN;
                }
                return;
            }
        }
        if (store instanceof RoaringBitmap) {
            RoaringBitmap bitmap = (RoaringBitmap) store;
            if (otherBitmap != null) {
                if (otherBitmap != bitmap) { // NOPMD - identity is the point
                    bitmap.and(otherBitmap);
                    this.size = SIZE_UNKNOWN;
                }
                return;
            }
        }

        // If indices is significantly smaller than it takes to go over this set
        // build the intersection from scratch
        long ourScanCost = scanCost();
        int otherSize = indices.size();
        if (indices instanceof IntSet && 4L * otherSize < ourScanCost) {
            int[] retained = new int[otherSize];
            int count = 0;
            int last = 0;
            IntIterator iterator = indices.iterator();
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (contains(index)) {
                    retained[count] = index;
                    count += 1;
                    if (index > last) {
                        last = index;
                    }
                }
            }
            Mode mode = idealModeEstimate(count, last);
            if (mode == Mode.ARRAY) {
                int[] newStore;
                if (count < retained.length) {
                    newStore = Arrays.copyOf(retained, count);
                } else {
                    newStore = retained;
                }
                Arrays.sort(newStore);
                this.store = newStore;
            } else if (mode == Mode.BIT_SET) {
                BitSet newStore;
                if (store instanceof BitSet) {
                    newStore = (BitSet) store;
                    newStore.clear();
                } else {
                    newStore = new BitSet(last + 1);
                    this.store = newStore;
                }
                for (int i = 0; i < count; i++) {
                    newStore.set(retained[i]);
                }
            } else {
                RoaringBitmap newStore;
                if (store instanceof RoaringBitmap) {
                    newStore = (RoaringBitmap) store;
                    newStore.clear();
                } else {
                    newStore = new RoaringBitmap();
                    this.store = newStore;
                }
                for (int i = 0; i < count; i++) {
                    newStore.add(retained[i]);
                }
            }
            this.size = count;
            return;
        }

        // Neither store can be handed to the other, so walk our own elements and drop the ones the
        // operand does not have. Copying the operand into our shape instead would cost its whole span -
        // a sparse bitmap-backed operand reaching far out dwarfs everything this set holds - and
        // promoting ourselves to meet it would give up the representation on a merely-read operand.
        removeIf(probe(indices).negate());
    }

    private boolean removeEachFromContainers(IntCollection indices, boolean trackRemove) {
        boolean track = trackRemove || size != SIZE_UNKNOWN;
        int removed = 0;
        Object store = this.store;
        IntIterator iterator = indices.iterator();
        if (store instanceof BitSet) {
            BitSet bitSet = (BitSet) store;
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (index < 0) {
                    continue;
                }
                if (track) {
                    if (bitSet.get(index)) {
                        bitSet.clear(index);
                        removed += 1;
                    }
                } else {
                    bitSet.clear(index);
                }
            }
        } else {
            RoaringBitmap bitmap = (RoaringBitmap) store;
            while (iterator.hasNext()) {
                int index = iterator.nextInt();
                if (index < 0) {
                    continue;
                }
                if (track) {
                    if (bitmap.checkedRemove(index)) {
                        removed += 1;
                    }
                } else {
                    bitmap.remove(index);
                }
            }
        }
        if (size != SIZE_UNKNOWN) {
            size -= removed;
        }
        return removed > 0;
    }

    @Override
    public void andNot(IntCollection indices) {
        if (isEmpty() || indices.isEmpty()) {
            return;
        }
        if (indices == this) { // NOPMD - identity is the point
            clear();
            return;
        }
        Object store = this.store;

        if (store instanceof int[]) {
            int[] array = (int[]) store;
            if (indices instanceof IntSet || indices.size() < size) {
                int retained = 0;
                for (int i = 0; i < size; i++) {
                    int value = array[i];
                    if (!indices.contains(value)) {
                        array[retained] = value;
                        retained += 1;
                    }
                }
                this.size = retained;
            } else {
                // indices.contains is more costly than checking this array
                indices.forEach(this::clear);
            }
            return;
        }

        if (store instanceof BitSet) {
            BitSet bitSet = (BitSet) store;
            BitSet otherWords = NatBitSetsUtil.words(indices);
            if (otherWords != null) {
                if (otherWords == bitSet) { // NOPMD - identity is the point
                    clear();
                    return;
                }
                bitSet.andNot(otherWords);
                size = SIZE_UNKNOWN;
            } else if (2L * indices.size() < scanCost() || !(indices instanceof IntSet)) {
                // Option 1: Indices is very small, so iterate over it instead of us
                // Option 2: Indices is large and has costly contains()
                // Adapts size
                removeEachFromContainers(indices, false);
            } else {
                // Indices is a large set, scan over ours.
                IntIterator iterator = BitSets.iterator(bitSet);
                int removed = 0;
                while (iterator.hasNext()) {
                    if (indices.contains(iterator.nextInt())) {
                        iterator.remove();
                        removed += 1;
                    }
                }
                if (size != SIZE_UNKNOWN) {
                    size -= removed;
                }
            }
            return;
        }

        RoaringBitmap bitmap = (RoaringBitmap) store;
        RoaringBitmap other = NatBitSetsUtil.bitmap(indices);
        if (other != null) {
            if (other == bitmap) { // NOPMD - identity is the point
                clear();
                return;
            }
            bitmap.andNot(other);
            this.size = SIZE_UNKNOWN;
        } else if (2L * indices.size() < scanCost() || !(indices instanceof IntSet)) {
            // Option 1: Indices is very small, so iterate over it instead of us
            // Option 2: Indices is large and has costly contains()
            // Adapts size
            removeEachFromContainers(indices, false);
        } else {
            // Indices is a large set, so gather those we want to remove -- roaring iterator cannot remove
            // TODO Can we do better?
            RoaringBitmap bitmapToRemove = new RoaringBitmap();
            PeekableIntIterator iterator = bitmap.getIntIterator();
            int removed = 0;
            while (iterator.hasNext()) {
                int next = iterator.next();
                if (indices.contains(next)) {
                    bitmapToRemove.add(next);
                    // NB: indices is a set, so uniqueness guaranteed
                    removed += 1;
                }
            }
            bitmap.andNot(bitmapToRemove);
            if (size != SIZE_UNKNOWN) {
                this.size -= removed;
            }
        }
    }

    @Override
    public void or(IntCollection indices) {
        if (indices.isEmpty() || indices == this) { // NOPMD - identity is the point
            return;
        }
        Object store = this.store;

        RoaringBitmap otherBitmap = NatBitSetsUtil.bitmap(indices);
        if (otherBitmap != null) {
            if (otherBitmap == store) { // NOPMD - identity is the point
                return;
            }
            // The other already gave up on words; follow it up the ladder rather than densifying.
            if (!(store instanceof RoaringBitmap)) {
                this.store = toBitmap();
            }
            ((RoaringBitmap) this.store).or(otherBitmap);
            this.size = SIZE_UNKNOWN;
            return;
        }

        BitSet otherWords = NatBitSetsUtil.words(indices);
        if (otherWords != null) {
            if (store instanceof BitSet) {
                BitSet bitSet = (BitSet) store;
                if (otherWords == bitSet) { // NOPMD - identity is the point
                    return;
                }
                int otherLast = otherWords.length() - 1;
                // bitSet.length() - 1 is lastInt(), and answers -1 rather than throwing when empty
                if (otherLast >= bitSet.length() - 1 && preferRoaringOverBitSet(size() + indices.size(), otherLast)) {
                    RoaringBitmap bitmap = RoaringBitmaps.of(bitSet);
                    RoaringBitmaps.add(bitmap, otherWords);
                    this.store = bitmap;
                } else {
                    bitSet.or(otherWords);
                }
                this.size = SIZE_UNKNOWN;
                return;
            }
            if (store instanceof RoaringBitmap) {
                RoaringBitmaps.add((RoaringBitmap) store, otherWords);
                this.size = SIZE_UNKNOWN;
                return;
            }
            if (size + indices.size() > MAXIMAL_ARRAY_SIZE) {
                int otherLast = otherWords.length() - 1;
                BitSet newStore = toBitSet(Math.max(size == 0 ? -1 : lastInt(), otherLast));
                newStore.or(otherWords);
                this.store = newStore;
                this.size = SIZE_UNKNOWN;
                return;
            }
            // Case: Array mode + small other
            indices.forEach(this::setInArray);
            return;
        }

        prepareFor(indices.size(), NatBitSetsUtil.lastOf(indices));
        indices.forEach(this::set);
    }

    @Override
    public void xor(IntCollection indices) {
        if (indices.isEmpty()) {
            return;
        }
        if (indices == this) { // NOPMD - identity is the point
            clear();
            return;
        }
        Object store = this.store;

        RoaringBitmap otherBitmap = NatBitSetsUtil.bitmap(indices);
        if (otherBitmap != null) {
            if (otherBitmap == store) { // NOPMD - identity is the point
                clear();
                return;
            }
            // As in or: the operand already gave up on words, so follow it rather than densifying
            if (!(store instanceof RoaringBitmap)) {
                this.store = toBitmap();
            }
            ((RoaringBitmap) this.store).xor(otherBitmap);
            this.size = SIZE_UNKNOWN;
            return;
        }

        prepareFor(indices.size(), NatBitSetsUtil.lastOf(indices));

        BitSet otherWords = NatBitSetsUtil.words(indices);
        if (otherWords != null) {
            if (this.store instanceof BitSet) {
                ((BitSet) this.store).xor(otherWords);
                this.size = SIZE_UNKNOWN;
                return;
            }
            if (this.store instanceof RoaringBitmap) {
                ((RoaringBitmap) this.store).xor(RoaringBitmaps.of(otherWords));
                this.size = SIZE_UNKNOWN;
                return;
            }
            // Array and other is small
            BitSets.forEach(otherWords, this::flip);
            return;
        }

        IntSet set = indices instanceof IntSet ? (IntSet) indices : new IntOpenHashSet(indices);
        set.forEach((IntConsumer) this::flip);
    }

    @Override
    public boolean retainAll(Collection<?> indices) {
        if (isEmpty()) {
            return false;
        }
        if (indices.isEmpty()) {
            clear();
            return true;
        }
        int size = size();
        // TODO This can be massively improved -- but on the other hand it shouldn't be called anyway
        NatBitSet retain = NatBitSets.of();
        for (Object index : indices) {
            if (index instanceof Integer) {
                int value = (int) index;
                if (contains(value)) {
                    retain.set(value);
                }
            }
        }
        and(retain);
        return size() < size;
    }

    @Override
    public boolean removeAll(Collection<?> indices) {
        if (isEmpty() || indices.isEmpty()) {
            return false;
        }
        int size = size();
        // TODO This can be massively improved -- but on the other hand it shouldn't be called anyway
        NatBitSet remove = NatBitSets.of();
        for (Object index : indices) {
            if (index instanceof Integer) {
                int value = (int) index;
                if (contains(value)) {
                    remove.set(value);
                }
            }
        }
        andNot(remove);
        return size() < size;
    }

    @Override
    public boolean removeAll(IntCollection indices) {
        if (isEmpty() || indices.isEmpty()) {
            return false;
        }
        if (indices == this) { // NOPMD - identity is the point
            clear();
            return true;
        }
        // Taking a few elements out one at a time says on its own whether any of them was there, where the
        // bulk paths have to bracket the operation with two cardinalities to find out
        if (!(store instanceof int[]) && indices.size() < scanCost()) {
            return removeEachFromContainers(indices, true);
        }
        int size = size();
        andNot(indices);
        return size() < size;
    }

    @Override
    public boolean removeIf(IntPredicate filter) {
        Object store = this.store;
        if (store instanceof int[]) {
            int[] array = (int[]) store;
            int retained = 0;
            for (int i = 0; i < size; i++) {
                int value = array[i];
                if (!filter.test(value)) {
                    array[retained] = value;
                    retained += 1;
                }
            }
            boolean modified = retained < size;
            this.size = retained;
            return modified;
        }
        if (store instanceof BitSet) {
            BitSet bitSet = (BitSet) store;
            int removed = 0;
            IntIterator iterator = BitSets.iterator(bitSet);
            while (iterator.hasNext()) {
                int next = iterator.nextInt();
                if (filter.test(next)) {
                    iterator.remove();
                    removed += 1;
                }
            }
            if (removed > 0 && size != SIZE_UNKNOWN) {
                this.size -= removed;
            }
            return removed > 0;
        }
        // Roaring cannot filter in place; gather the victims as a bitmap and take them out in one go
        RoaringBitmap bitmap = (RoaringBitmap) store;
        RoaringBitmap remove = new RoaringBitmap();
        bitmap.forEach((org.roaringbitmap.IntConsumer) value -> {
            if (filter.test(value)) {
                remove.add(value);
            }
        });
        if (remove.isEmpty()) {
            return false;
        }
        bitmap.andNot(remove);
        this.size = size == SIZE_UNKNOWN ? SIZE_UNKNOWN : size - remove.getCardinality();
        return true;
    }

    @Override
    public IntStream intStream() {
        if (store instanceof int[]) {
            return Arrays.stream((int[]) store, 0, size);
        }
        if (store instanceof BitSet) {
            return ((BitSet) store).stream();
        }
        return ((RoaringBitmap) store).stream();
    }

    static NatBitSet union(Collection<? extends IntCollection> operands) {
        // Collected rather than re-walked: the caller may hand us a lazily computed view, and the shape
        // scan and the fold below would otherwise evaluate it twice
        List<IntCollection> present = new ArrayList<>(operands.size());
        int maximalLast = -1;
        boolean spanKnown = true;
        long totalCardinality = 0L;
        boolean anyBitmap = false;

        for (IntCollection operand : operands) {
            if (operand.isEmpty()) {
                continue;
            }
            present.add(operand);
            int last = NatBitSetsUtil.lastOf(operand);
            if (last == NatBitSetsUtil.UNKNOWN_LAST) {
                spanKnown = false;
            } else {
                maximalLast = Math.max(maximalLast, last);
            }
            totalCardinality += operand.size();
            anyBitmap |= NatBitSetsUtil.bitmap(operand) != null;
        }

        if (present.isEmpty()) {
            return new HybridNatBitSet();
        }
        Mode mode;
        if (spanKnown) {
            // The union holds at most every element of every operand, and at most the whole span
            int cardinality = (int) Math.min(totalCardinality, maximalLast + 1L);
            mode = idealModeEstimate(cardinality, maximalLast);
        } else {
            // Without a bound on the span, words could be arbitrarily wasteful and Roaring cannot be
            mode = Mode.ROARING;
        }

        // An operand that has already given up on words is evidence the union will want to as well -
        // the same call or() and xor() make when a single operand arrives as a bitmap
        if (mode == Mode.ROARING || anyBitmap) {
            return new HybridNatBitSet(unionAsBitmap(present));
        }

        if (mode == Mode.ARRAY) {
            HybridNatBitSet result = new HybridNatBitSet();
            for (IntCollection operand : present) {
                result.or(operand);
            }
            return result;
        }

        BitSet bitSet = new BitSet(maximalLast + 1);
        for (IntCollection operand : present) {
            BitSet words = NatBitSetsUtil.words(operand);
            if (words == null) {
                operand.forEach((IntConsumer) bitSet::set);
            } else {
                bitSet.or(words);
            }
        }
        return new HybridNatBitSet(bitSet);
    }

    /**
     * The union of non-empty operands as a bitmap.
     *
     * <p>Operands that already are bitmaps go into the n-ary or as they are - it reads them without
     * copying and without handing any of them back as the result. The rest are folded in afterwards,
     * word backed ones run by run, so that a dense operand costs a call per run rather than one per
     * element and never has to be converted into a bitmap of its own first.</p>
     */
    private static RoaringBitmap unionAsBitmap(List<IntCollection> operands) {
        List<RoaringBitmap> bitmaps = new ArrayList<>(operands.size());
        for (IntCollection operand : operands) {
            RoaringBitmap bitmap = NatBitSetsUtil.bitmap(operand);
            if (bitmap != null) {
                bitmaps.add(bitmap);
            }
        }
        RoaringBitmap union = bitmaps.isEmpty() ? new RoaringBitmap() : RoaringBitmap.or(bitmaps.iterator());
        for (IntCollection operand : operands) {
            if (NatBitSetsUtil.bitmap(operand) != null) {
                continue;
            }
            BitSet words = NatBitSetsUtil.words(operand);
            if (words == null) {
                operand.forEach((IntConsumer) union::add);
            } else {
                RoaringBitmaps.add(union, words);
            }
        }
        union.runOptimize();
        return union;
    }

    // Clone and equality

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public HybridNatBitSet clone() {
        return new HybridNatBitSet(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        // Two sets that end in different places are different, and both ends are O(1) - the inherited
        // comparison starts by taking both cardinalities, which is not
        if (o instanceof IntCollection) {
            IntCollection other = (IntCollection) o;
            if (other.isEmpty() || isEmpty()) {
                return other.isEmpty() && isEmpty();
            }
            int otherLast = NatBitSetsUtil.lastOf(other);
            if (otherLast != NatBitSetsUtil.UNKNOWN_LAST && lastInt() != otherLast) {
                return false;
            }
        }
        Object store = this.store;
        if (store instanceof int[]) {
            if (o instanceof HybridNatBitSet && ((HybridNatBitSet) o).store instanceof int[]) {
                HybridNatBitSet other = (HybridNatBitSet) o;
                return size == other.size && Arrays.equals((int[]) store, 0, size, (int[]) other.store, 0, size);
            }
        } else if (store instanceof BitSet) {
            BitSet otherWords = NatBitSetsUtil.words(o);
            if (otherWords != null) {
                return store.equals(otherWords);
            }
        } else {
            RoaringBitmap otherBitmap = NatBitSetsUtil.bitmap(o);
            if (otherBitmap != null) {
                return store.equals(otherBitmap);
            }
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    boolean isWordBacked() {
        return store instanceof BitSet;
    }

    BitSet words() {
        return (BitSet) store;
    }

    boolean isBitmapBacked() {
        return store instanceof RoaringBitmap;
    }

    RoaringBitmap bitmap() {
        return (RoaringBitmap) store;
    }

    Object store() {
        return store;
    }

    /** Delegates removal to the set so that the cached size stays correct. */
    private static final class RemovingIterator implements IntIterator {
        private final HybridNatBitSet set;
        private final IntIterator iterator;
        private int last = -1;

        RemovingIterator(HybridNatBitSet set, IntIterator iterator) {
            this.set = set;
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public int nextInt() {
            int value = iterator.nextInt();
            last = value;
            return value;
        }

        @Override
        public void remove() {
            if (last < 0) {
                throw new IllegalStateException();
            }
            set.clear(last);
            last = -1;
        }
    }

    private static final class ArrayIterator implements IntIterator {
        private final HybridNatBitSet set;
        private int next;
        private boolean removable;

        ArrayIterator(HybridNatBitSet set) {
            this.set = set;
        }

        @Override
        public boolean hasNext() {
            return next < set.size;
        }

        @Override
        public int nextInt() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            removable = true;
            int value = ((int[]) set.store)[next];
            next += 1;
            return value;
        }

        @Override
        public void remove() {
            if (!removable) {
                throw new IllegalStateException();
            }
            removable = false;
            next -= 1;
            int[] array = (int[]) set.store;
            System.arraycopy(array, next + 1, array, next, set.size - next - 1);
            set.size -= 1;
        }
    }
}
