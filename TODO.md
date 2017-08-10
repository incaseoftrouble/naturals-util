
 * Add support for Roaring Bitmaps with soft dependency.
 * Improve implementations for the iterators ("block optimization").
 * Add more tests.
 * Let the implementations dynamically switch which reference currently is the "complement" and which the "true" view - useful for, e.g., xor computation.
 * Add efficient bulk operations between bounded and unbounded sets.
 * Avoid cloning the bit sets by employing multiple "bitwise" operations (if reasonable)
 * Extend the set theories by splitting the data points into pre-filled sets and implementations, allowing to, e.g., test `simpleSet.addAll(singletonComplementSet)`.
 * Power-set for `BoundedNatBitSet`