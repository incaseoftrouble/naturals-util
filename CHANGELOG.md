# 0.x

### 0.19.0 (2023-11-10)

 * Bump versions
 * Fixed performance issue of union find
 * Code is now spotless

### 0.18.0 (2022-09-15)

 * Bump versions
 * Add `guava-testlib` testing for most collections
 * Fix some iterator removals
 * Add `setValue` and `remove` support for some entries / iterators
 * Minor performance improvements
 
### 0.17.1 (2022-05-02)

 * Fix issue of Nat2XDenseMap

### 0.17.0 (2022-03-29)

 * Nat array maps now resize
 * Breaking change: `Indices#IndexConsumer` flipped argument order for compatibility with similar `forEachIndexed` methods in e.g. Guava.

### 0.16.0 (2022-02-14)

 * Version bumps, upgrade to Java 11, upgrade to Gradle Kotlin DSL 

### 0.15.1 (2020-07-08)

 * Version bumps, upgrade to JUnit 5

### 0.15.0 (2020-04-06)

 * Remove `size()` optimizations from `containsAll` checks, since `size()` is a linear time operation, too.
 * Bump version of `RoaringBitmap`, fixing a similar bug.
 * Performance improvements for Roaring-backed sets.

### 0.14.1 (2019-10-29)

 * Fixed some problems of static analysis tools

### 0.14.0 (2019-10-29)

 * Maintenance version bump

### 0.13.0 (2019-07-13)

 * Add CI
 * Bump versions of roaring bitmaps and fastutil
 * Improve performance of roaring bitmaps

### 0.12.0 (2018-11-09)

 * Extend support for bounded/unbounded pairs to all bulk operations
 * Unify source code ordering of sets
 * Add roaring bitmaps implementation

### 0.11.0 (2018-11-07)

 * Improve `containsAll` for bounded/unbounded pairs
 * Add `NatBitSetFactory` - Useful to transparently change used implementation
 * Extract some factory methods to `NatBitSetProvider`
 * Update some dependencies

### 0.10.0 (2018-02-15)

 * Fixed a visibility issue in `Indices`, removed superfluous method
 * Update build infrastructure, drop SpotBugs inspection 

### 0.9.0 (2018-02-07)

 * Some more utility in `Indices`, `BitSets`, and `SparseBitSets`
 * Added `Arrays2`, a (small) collection of array utilities

### 0.8.0 (2017-12-08)

 * Split `BitSets` in `SparseBitSets` and `BitSets`
 * Moved and renamed some factory methods
 * Update to fastutil 8.1.1 and SparseBitSet 1.1
 * Some cleanup

### 0.7.0 (2017-11-14)

 * Refactoring of some utilities
 * Made abstract implementations public
 * Added unmodifiable implementations
 * Fixed bug in `Nat2ObjectDenseArrayMap`
 * Change iteration order of power set to be lexicographic
 * Added efficient power set for the `{0, ..., n-1}` case

### 0.6.0 (2017-10-06)

 * Added `IntArraySortedSet`, which is not a sorted set yet but uses a sorted array as backing data structure, yielding `O(log n)` runtime for `contains`. Methods like `addAll`, `containsAll` etc. can be optimized for the case of sorted arguments.
 * Added `previousPresentIndex` and `previousAbsentIndex` to `NatBitSet`
 * Reworked testing theories
 * Fixed some more bugs
 * Upgraded gradle, findbugs (now spotbugs), PMD and checkstyle

### 0.5.0 (2017-08-31)

 * Added some support for boolean arrays, namely `PowerSetIterator` and `Indices.indexMap`.

### 0.4.1 (2017-08-31)

 * The `NatCartesianProductSet` and -iterator now copy the input array.

### 0.4.0 (2017-08-31)

 * Added `NatCartesianProductSet` and corresponding iterator

### 0.3.1 (2017-08-30)

 * Fix bug in `IntPreOrder`

### 0.3.0 (2017-08-28)

 * Added streams and `clearFrom` method
 * Fixed wrong behaviour of `LongNatBitSet`'s `clear` method
 * Added some more functionality to `BitSets` (e.g. `powerSet(BitSet)`) and `NatBitSets` (e.g. `powerSet(NatBitSet)` and `boundedFilledSet(int)`).
 * Fixed rare bug in `SparseBitSet`
 * Fixed bug in `NaturalsTransformer`

### 0.2.0 (2017-08-04)

 * Numerous fixes and improvements.
 * More test cases for `NatBitSet` implementations.
 * Added set variants based on a single `long`.
 * Several utility methods added to `NatBitSets`.

### 0.1.0 (2017-07-23)

 * Initial release.
