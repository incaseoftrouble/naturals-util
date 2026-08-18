// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Compares plain linear search, {@link Arrays#binarySearch(int[], int)} and a
 * {@link Arrays2#hybridBinarySearch}-style hybrid search with a configurable window.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 250, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 250, timeUnit = TimeUnit.MILLISECONDS)
public class ArraySearchBenchmark {
    private static final long SEED = 0x5EED;

    private static int[] buildArray(int size) {
        Random random = new Random(SEED);
        int[] array = new int[size];
        do {
            for (int i = 0; i < size; i++) {
                array[i] = random.nextInt();
            }
        } while (Arrays.stream(array).distinct().count() != size);
        Arrays.sort(array);
        return array;
    }

    private static int[] buildQueries(int[] array) {
        Random random = new Random(SEED);
        int size = array.length;
        int[] queries = new int[2 * size];
        for (int i = 0; i < size; i++) {
            queries[2 * i] = array[i];
            int query;
            do {
                query = random.nextInt();
            } while (linearSearch(array, query) >= 0);
            queries[2 * i + 1] = query;
        }
        for (int i = queries.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = queries[i];
            queries[i] = queries[j];
            queries[j] = tmp;
        }
        return queries;
    }

    private static int linearSearch(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            int current = array[i];
            if (current >= value) {
                return current == value ? i : -(i + 1);
            }
        }
        return -(array.length + 1);
    }

    // Same algorithm as Arrays2.hybridBinarySearch, but with the linear-search window as a parameter
    // instead of a fixed constant, so the sweep can search for the best window on this hardware.
    private static int hybridSearch(int[] array, int value, int window) {
        int low = 0;
        int high = array.length;
        while (high - low > window) {
            int mid = (low + high) >>> 1;
            if (array[mid] < value) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        for (int i = low; i < high; i++) {
            int current = array[i];
            if (current >= value) {
                return current == value ? i : -(i + 1);
            }
        }
        return high < array.length && array[high] == value ? high : -(high + 1);
    }

    @State(Scope.Thread)
    public static class SearchData {
        @Param({"4", "8", "16", "32", "48", "64", "128", "256", "512"})
        public int size;

        int[] array = {};
        int[] queries = {};
        int index;

        @Setup(Level.Trial)
        public void setup() {
            array = buildArray(size);
            queries = buildQueries(array);
            index = 0;
        }

        int nextQuery() {
            int value = queries[index];
            index = (index + 1) % queries.length;
            return value;
        }
    }

    @State(Scope.Thread)
    public static class HybridSearchData extends SearchData {
        @Param({"8", "16", "24", "32", "48", "64"})
        public int window;
    }

    @Benchmark
    public int linearSearch(SearchData data) {
        return linearSearch(data.array, data.nextQuery());
    }

    @Benchmark
    public int arraysBinarySearch(SearchData data) {
        return Arrays.binarySearch(data.array, data.nextQuery());
    }

    @Benchmark
    public int hybridSearch(HybridSearchData data) {
        return hybridSearch(data.array, data.nextQuery(), data.window);
    }
}
