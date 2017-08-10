// Obtained from: https://github.com/brettwooldridge/SparseBitSet (License: Apache License 2.0)
// Current version is not pushed to maven (yet)
// Note: To keep this file and the generated javadoc a bit smaller, all comments have been stripped.
// Refer to the above file for a documented version.

package de.tum.in.naturals.bitset;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressFBWarnings
@SuppressWarnings("all")
public class SparseBitSet implements Cloneable, Serializable {
  protected static final int INDEX_SIZE = Integer.SIZE - 1;
  protected static final int LENGTH4 = Long.SIZE;
  protected static final int LEVEL2 = 5;
  protected static final int LENGTH2 = 1 << LEVEL2;
  protected static final int LEVEL3 = 5;
  protected static final int LENGTH3 = 1 << LEVEL3;
  protected static final int LEVEL4 = 6;
  protected static final int LEVEL1 = INDEX_SIZE - LEVEL2 - LEVEL3 - LEVEL4;
  protected static final int MASK2 = LENGTH2 - 1;
  protected static final int MASK3 = LENGTH3 - 1;
  protected static final int MAX_LENGTH1 = 1 << LEVEL1;
  protected static final int SHIFT1 = LEVEL2 + LEVEL3;
  protected static final int SHIFT2 = LEVEL3;
  protected static final int SHIFT3 = LEVEL4;
  protected static final int UNIT = LENGTH2 * LENGTH3 * LENGTH4;
  protected static final transient AndNotStrategy andNotStrategy = new AndNotStrategy();
  protected static final transient AndStrategy andStrategy = new AndStrategy();
  protected static final transient ClearStrategy clearStrategy = new ClearStrategy();
  protected static final transient CopyStrategy copyStrategy = new CopyStrategy();
  protected static final transient FlipStrategy flipStrategy = new FlipStrategy();
  protected static final transient IntersectsStrategy intersectsStrategy = new IntersectsStrategy();
  protected static final transient OrStrategy orStrategy = new OrStrategy();
  protected static final transient SetStrategy setStrategy = new SetStrategy();
  protected static final transient XorStrategy xorStrategy = new XorStrategy();
  static final long[] ZERO_BLOCK = new long[LENGTH3];
  private static final long serialVersionUID = -6663013367427929992L;
  static int compactionCountDefault = 2;

  protected transient long[][][] bits;
  protected transient int bitsLength;
  protected transient Cache cache;
  protected transient int compactionCount;
  protected transient EqualsStrategy equalsStrategy;
  protected transient long[] spare;
  protected transient UpdateStrategy updateStrategy;

  protected SparseBitSet(int capacity, int compactionCount) throws NegativeArraySizeException {
    if (capacity < 0) {
      throw new NegativeArraySizeException("(requested capacity=" + capacity + ") < 0");
    }
    resize(capacity == 0 ? 0 : capacity - 1);
    this.compactionCount = compactionCount;
    constructorHelper();
    statisticsUpdate();
  }

  public SparseBitSet() {
    this(1, compactionCountDefault);
  }

  public SparseBitSet(int nbits) throws NegativeArraySizeException {
    this(nbits, compactionCountDefault);
  }

  public static SparseBitSet and(SparseBitSet a, SparseBitSet b) {
    final SparseBitSet result = a.clone();
    result.and(b);
    return result;
  }

  public static SparseBitSet andNot(SparseBitSet a, SparseBitSet b) {
    final SparseBitSet result = a.clone();
    result.andNot(b);
    return result;
  }

  public static SparseBitSet or(SparseBitSet a, SparseBitSet b) {
    final SparseBitSet result = a.clone();
    result.or(b);
    return result;
  }

  protected static void throwIndexOutOfBoundsException(int i, int j)
      throws IndexOutOfBoundsException {
    String s = "";
    if (i < 0) {
      s += "(i=" + i + ") < 0";
    }
    if (i == Integer.MAX_VALUE) {
      s += "(i=" + i + ")";
    }
    if (j < 0) {
      s += (s.isEmpty() ? "" : ", ") + "(j=" + j + ") < 0";
    }
    if (i > j) {
      s += (s.isEmpty() ? "" : ", ") + "(i=" + i + ") > (j=" + j + ")";
    }
    throw new IndexOutOfBoundsException(s);
  }

  public static SparseBitSet xor(SparseBitSet a, SparseBitSet b) {
    final SparseBitSet result = a.clone();
    result.xor(b);
    return result;
  }

  public void and(int i, boolean value) throws IndexOutOfBoundsException {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    if (!value) {
      clear(i);
    }
  }

  public void and(int i, int j, SparseBitSet b) throws IndexOutOfBoundsException {
    setScanner(i, j, b, andStrategy);
  }

  public void and(SparseBitSet b) {
    nullify(Math.min(bits.length, b.bits.length));
    setScanner(0, Math.min(bitsLength, b.bitsLength), b, andStrategy);
  }

  public void andNot(int i, boolean value) {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    if (value) {
      clear(i);
    }
  }

  public void andNot(int i, int j, SparseBitSet b)
      throws IndexOutOfBoundsException {
    setScanner(i, j, b, andNotStrategy);
  }

  public void andNot(SparseBitSet b) {
    setScanner(0, Math.min(bitsLength, b.bitsLength), b, andNotStrategy);
  }

  public int cardinality() {
    statisticsUpdate();
    return cache.cardinality;
  }

  public void clear(int i) {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    if (i >= bitsLength) {
      return;
    }
    final int w = i >> SHIFT3;
    long[][] a2;
    if ((a2 = bits[w >> SHIFT1]) == null) {
      return;
    }
    long[] a3;
    if ((a3 = a2[(w >> SHIFT2) & MASK2]) == null) {
      return;
    }
    a3[w & MASK3] &= ~(1L << i);
    cache.hash = 0;
  }

  public void clear(int i, int j) throws IndexOutOfBoundsException {
    setScanner(i, j, null, clearStrategy);
  }

  public void clear() {
    nullify(0);
  }

  @Override
  public SparseBitSet clone() {
    try {
      final SparseBitSet result = (SparseBitSet) super.clone();
      result.bits = null;
      result.resize(1);
      result.constructorHelper();
      result.equalsStrategy = null;
      result.setScanner(0, bitsLength, this, copyStrategy);
      return result;
    } catch (CloneNotSupportedException ex) {
      throw new InternalError(ex.getMessage());
    }
  }

  protected final void constructorHelper() {
    spare = new long[LENGTH3];
    cache = new Cache();
    updateStrategy = new UpdateStrategy();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SparseBitSet)) {
      return false;
    }
    final SparseBitSet b = (SparseBitSet) obj;
    if (this == b) {
      return true;
    }
    if (equalsStrategy == null) {
      equalsStrategy = new EqualsStrategy();
    }
    setScanner(0, Math.max(bitsLength, b.bitsLength), b, equalsStrategy);
    return equalsStrategy.result;
  }

  public void flip(int i) {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    final int w = i >> SHIFT3;
    final int w1 = w >> SHIFT1;
    final int w2 = (w >> SHIFT2) & MASK2;
    if (i >= bitsLength) {
      resize(i);
    }
    long[][] a2;
    if ((a2 = bits[w1]) == null) {
      a2 = bits[w1] = new long[LENGTH2][];
    }
    long[] a3;
    if ((a3 = a2[w2]) == null) {
      a3 = a2[w2] = new long[LENGTH3];
    }
    a3[w & MASK3] ^= 1L << i;
    cache.hash = 0;
  }

  public void flip(int i, int j) throws IndexOutOfBoundsException {
    setScanner(i, j, null, flipStrategy);
  }

  public boolean get(int i) {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    final int w = i >> SHIFT3;
    long[][] a2;
    long[] a3;
    return i < bitsLength && (a2 = bits[w >> SHIFT1]) != null
        && (a3 = a2[(w >> SHIFT2) & MASK2]) != null
        && ((a3[w & MASK3] & (1L << i)) != 0);
  }

  public SparseBitSet get(int i, int j) throws IndexOutOfBoundsException {
    final SparseBitSet result = new SparseBitSet(j, compactionCount);
    result.setScanner(i, j, this, copyStrategy);
    return result;
  }

  @Override
  public int hashCode() {
    statisticsUpdate();
    return cache.hash;
  }

  public boolean intersects(int i, int j, SparseBitSet b)
      throws IndexOutOfBoundsException {
    setScanner(i, j, b, intersectsStrategy);
    return intersectsStrategy.result;
  }

  public boolean intersects(SparseBitSet b) {
    setScanner(0, Math.max(bitsLength, b.bitsLength), b, intersectsStrategy);
    return intersectsStrategy.result;
  }

  public boolean isEmpty() {
    statisticsUpdate();
    return cache.cardinality == 0;
  }

  public int length() {
    statisticsUpdate();
    return cache.length;
  }

  public int nextClearBit(int i) {
    if (i < 0) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    int w = i >> SHIFT3;
    int w3 = w & MASK3;
    int w2 = (w >> SHIFT2) & MASK2;
    int w1 = w >> SHIFT1;
    long nword = ~0L << i;
    final int aLength = bits.length;
    long[][] a2;
    long[] a3;
    if (w1 < aLength && (a2 = bits[w1]) != null
        && (a3 = a2[w2]) != null
        && ((nword = ~a3[w3] & (~0L << i))) == 0L) {
      ++w;
      w3 = w & MASK3;
      w2 = (w >> SHIFT2) & MASK2;
      w1 = w >> SHIFT1;
      nword = ~0L;
      loop:
      for (; w1 != aLength; ++w1) {
        if ((a2 = bits[w1]) == null) {
          break;
        }
        for (; w2 != LENGTH2; ++w2) {
          if ((a3 = a2[w2]) == null) {
            break loop;
          }
          for (; w3 != LENGTH3; ++w3) {
            if ((nword = ~a3[w3]) != 0) {
              break loop;
            }
          }
          w3 = 0;
        }
        w2 = w3 = 0;
      }
    }
    final int result = (((w1 << SHIFT1) + (w2 << SHIFT2) + w3) << SHIFT3)
        + Long.numberOfTrailingZeros(nword);
    return (result == Integer.MAX_VALUE ? -1 : result);
  }

  public int nextSetBit(int i) {
    if (i < 0) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    int w = i >> SHIFT3;
    int w3 = w & MASK3;
    int w2 = (w >> SHIFT2) & MASK2;
    int w1 = w >> SHIFT1;
    long word = 0L;
    final int aLength = bits.length;
    long[][] a2;
    long[] a3;
    if (w1 < aLength && ((a2 = bits[w1]) == null
        || (a3 = a2[w2]) == null
        || ((word = a3[w3] & (~0L << i)) == 0L))) {
      ++w;
      w3 = w & MASK3;
      w2 = (w >> SHIFT2) & MASK2;
      w1 = w >> SHIFT1;
      major:
      for (; w1 != aLength; ++w1) {
        if ((a2 = bits[w1]) != null) {
          for (; w2 != LENGTH2; ++w2) {
            if ((a3 = a2[w2]) != null) {
              for (; w3 != LENGTH3; ++w3) {
                if ((word = a3[w3]) != 0) {
                  break major;
                }
              }
            }
            w3 = 0;
          }
        }
        w2 = w3 = 0;
      }
    }
    return (w1 >= aLength ? -1
        : (((w1 << SHIFT1) + (w2 << SHIFT2) + w3) << SHIFT3)
            + Long.numberOfTrailingZeros(word));
  }

  protected final void nullify(int start) {
    final int aLength = bits.length;
    if (start < aLength) {
      for (int w = start; w != aLength; ++w) {
        bits[w] = null;
      }
      cache.hash = 0;
    }
  }

  public void or(int i, boolean value) {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    if (value) {
      set(i);
    }
  }

  public void or(int i, int j, SparseBitSet b) throws IndexOutOfBoundsException {
    setScanner(i, j, b, orStrategy);
  }

  public void or(SparseBitSet b) {
    setScanner(0, b.bitsLength, b, orStrategy);
  }

  private void readObject(ObjectInputStream s) throws IOException,
      ClassNotFoundException {
    s.defaultReadObject();
    compactionCount = s.readInt();
    final int aLength = s.readInt();
    resize(aLength);
    final int count = s.readInt();
    long[][] a2;
    long[] a3;
    for (int n = 0; n != count; ++n) {
      final int w = s.readInt();
      final int w3 = w & MASK3;
      final int w2 = (w >> SHIFT2) & MASK2;
      final int w1 = w >> SHIFT1;
      final long word = s.readLong();
      if ((a2 = bits[w1]) == null) {
        a2 = bits[w1] = new long[LENGTH2][];
      }
      if ((a3 = a2[w2]) == null) {
        a3 = a2[w2] = new long[LENGTH3];
      }
      a3[w3] = word;
    }
    constructorHelper();
    statisticsUpdate();
    if (count != cache.count) {
      throw new InternalError("count of entries not consistent");
    }
    final int hash = s.readInt();
    if (hash != cache.hash) {
      throw new IOException("deserialized hashCode mis-match");
    }
  }

  protected final void resize(int index) {
    final int w1 = (index >> SHIFT3) >> SHIFT1;
    int newSize = Integer.highestOneBit(w1);
    if (newSize == 0) {
      newSize = 1;
    }
    if (w1 >= newSize) {
      newSize <<= 1;
    }
    if (newSize > MAX_LENGTH1) {
      newSize = MAX_LENGTH1;
    }
    final int aLength1 = (bits != null ? bits.length : 0);
    if (newSize != aLength1) {
      final long[][][] temp = new long[newSize][][];
      if (aLength1 != 0) {
        System.arraycopy(bits, 0, temp, 0, Math.min(aLength1, newSize));
        nullify(0);
      }
      bits = temp;
      bitsLength =
          (newSize == MAX_LENGTH1 ? Integer.MAX_VALUE : newSize * UNIT);
    }
  }

  public void set(int i) {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    final int w = i >> SHIFT3;
    final int w1 = w >> SHIFT1;
    final int w2 = (w >> SHIFT2) & MASK2;
    if (i >= bitsLength) {
      resize(i);
    }
    long[][] a2;
    if ((a2 = bits[w1]) == null) {
      a2 = bits[w1] = new long[LENGTH2][];
    }
    long[] a3;
    if ((a3 = a2[w2]) == null) {
      a3 = a2[w2] = new long[LENGTH3];
    }
    a3[w & MASK3] |= 1L << i;
    cache.hash = 0;
  }

  public void set(int i, boolean value) {
    if (value) {
      set(i);
    } else {
      clear(i);
    }
  }

  public void set(int i, int j) throws IndexOutOfBoundsException {
    setScanner(i, j, null, setStrategy);
  }

  public void set(int i, int j, boolean value) {
    if (value) {
      set(i, j);
    } else {
      clear(i, j);
    }
  }

  protected final void setScanner(int i, int j, SparseBitSet b,
      AbstractStrategy op) throws IndexOutOfBoundsException {
    if (op.start(b)) {
      cache.hash = 0;
    }
    if (j < i || (i + 1) < 1) {
      throwIndexOutOfBoundsException(i, j);
    }
    if (i == j) {
      return;
    }
    final int properties = op.properties();
    final boolean f_op_f_eq_f = (properties & AbstractStrategy.F_OP_F_EQ_F) != 0;
    final boolean f_op_x_eq_f = (properties & AbstractStrategy.F_OP_X_EQ_F) != 0;
    final boolean x_op_f_eq_f = (properties & AbstractStrategy.X_OP_F_EQ_F) != 0;
    final boolean x_op_f_eq_x = (properties & AbstractStrategy.X_OP_F_EQ_X) != 0;
    int u = i >> SHIFT3;
    final long um = ~0L << i;
    final int v = (j - 1) >> SHIFT3;
    final long vm = ~0L >>> -j;
    long[][][] a1 = bits;
    int aLength1 = bits.length;
    final long[][][] b1 = (b != null ? b.bits : null);
    final int bLength1 = (b1 != null ? b.bits.length : 0);
    int u1 = u >> SHIFT1;
    int u2 = (u >> SHIFT2) & MASK2;
    int u3 = u & MASK3;
    final int v1 = v >> SHIFT1;
    final int v2 = (v >> SHIFT2) & MASK2;
    final int v3 = v & MASK3;
    final int lastA3Block = (v1 << LEVEL2) + v2;
    int a2CountLocal = 0;
    int a3CountLocal = 0;
    boolean notFirstBlock = u == 0 && um == ~0L;
    boolean a2IsEmpty = u2 == 0;
    while (i < j) {
      long[][] a2 = null;
      boolean haveA2 = u1 < aLength1 && (a2 = a1[u1]) != null;
      long[][] b2 = null;
      final boolean haveB2 = u1 < bLength1
          && b1 != null && (b2 = b1[u1]) != null;
      if ((!haveA2 && !haveB2 && f_op_f_eq_f
          || !haveA2 && f_op_x_eq_f || !haveB2 && x_op_f_eq_f)
          && notFirstBlock && u1 != v1) {
        if (u1 < aLength1) {
          a1[u1] = null;
        }
      } else {
        final int limit2 = (u1 == v1 ? v2 + 1 : LENGTH2);
        while (u2 != limit2) {
          long[] a3 = null;
          final boolean haveA3 = haveA2 && (a3 = a2[u2]) != null;
          long[] b3 = null;
          final boolean haveB3 = haveB2 && (b3 = b2[u2]) != null;
          final int a3Block = (u1 << LEVEL2) + u2;
          final boolean notLastBlock = lastA3Block != a3Block;
          if ((!haveA3 && !haveB3 && f_op_f_eq_f
              || !haveA3 && f_op_x_eq_f || !haveB3 && x_op_f_eq_f)
              && notFirstBlock && notLastBlock) {
            if (haveA2) {
              a2[u2] = null;
            }
          } else {
            final int base3 = a3Block << SHIFT2;
            final int limit3 = (notLastBlock ? LENGTH3 : v3);
            if (!haveA3) {
              a3 = spare;
            }
            if (!haveB3) {
              b3 = ZERO_BLOCK;
            }
            boolean isZero;
            if (notFirstBlock && notLastBlock) {
              if (x_op_f_eq_x && !haveB3) {
                isZero = op.isZeroBlock(a3);
              } else {
                isZero = op.block(base3, 0, LENGTH3, a3, b3);
              }
            } else {
              if (notFirstBlock) {
                isZero = op.block(base3, 0, limit3, a3, b3);
                isZero &= op.word(base3, limit3, a3, b3, vm);
              } else {
                if (u == v) {
                  isZero = op.word(base3, u3, a3, b3, um & vm);
                } else {
                  isZero = op.word(base3, u3, a3, b3, um);
                  isZero &=
                      op.block(base3, u3 + 1, limit3, a3, b3);
                  if (limit3 != LENGTH3) {
                    isZero &= op.word(base3, limit3, a3, b3, vm);
                  }
                }
                notFirstBlock = true;
              }
              if (isZero) {
                isZero = op.isZeroBlock(a3);
              }
            }
            if (isZero) {
              if (haveA2) {
                a2[u2] = null;
              }
            } else {
              if (a3 == spare) {
                if (i >= bitsLength) {
                  resize(i);
                  a1 = bits;
                  aLength1 = a1.length;
                }
                if (a2 == null) {
                  a1[u1] = a2 = new long[LENGTH2][];
                  haveA2 = true;
                }
                a2[u2] = a3;
                spare = new long[LENGTH3];
              }
              ++a3CountLocal;
            }
            a2IsEmpty &= !(haveA2 && a2[u2] != null);
          }
          ++u2;
          u3 = 0;
        }
        if (u2 == LENGTH2 && a2IsEmpty && u1 < aLength1) {
          a1[u1] = null;
        } else {
          ++a2CountLocal;
        }
      }
      i = (u = (++u1 << SHIFT1)) << SHIFT3;
      u2 = 0;
      if (i < 0) {
        i = Integer.MAX_VALUE;
      }
    }
    op.finish(a2CountLocal, a3CountLocal);
  }

  public int size() {
    statisticsUpdate();
    return cache.size;
  }

  public String statistics() {
    return statistics(null);
  }

  public String statistics(String[] values) {
    statisticsUpdate();
    String[] v = new String[Statistics.values().length];
    v[Statistics.Size.ordinal()] = Integer.toString(size());
    v[Statistics.Length.ordinal()] = Integer.toString(length());
    v[Statistics.Cardinality.ordinal()] = Integer.toString(cardinality());
    v[Statistics.Total_words.ordinal()] = Integer.toString(cache.count);
    v[Statistics.Set_array_length.ordinal()] = Integer.toString(bits.length);
    v[Statistics.Set_array_max_length.ordinal()] =
        Integer.toString(MAX_LENGTH1);
    v[Statistics.Level2_areas.ordinal()] = Integer.toString(cache.a2Count);
    v[Statistics.Level2_area_length.ordinal()] = Integer.toString(LENGTH2);
    v[Statistics.Level3_blocks.ordinal()] = Integer.toString(cache.a3Count);
    v[Statistics.Level3_block_length.ordinal()] = Integer.toString(LENGTH3);
    v[Statistics.Compaction_count_value.ordinal()] =
        Integer.toString(compactionCount);
    int longestLabel = 0;
    for (Statistics s : Statistics.values()) {
      longestLabel =
          Math.max(longestLabel, s.name().length());
    }
    final StringBuilder result = new StringBuilder();
    for (Statistics s : Statistics.values()) {
      result.append(s.name());
      for (int i = 0; i != longestLabel - s.name().length(); ++i) {
        result.append(' ');
      }
      result.append(" = ");
      result.append(v[s.ordinal()]);
      result.append('\n');
    }
    for (int i = 0; i != result.length(); ++i) {
      if (result.charAt(i) == '_') {
        result.setCharAt(i, ' ');
      }
    }
    if (values != null) {
      final int len = Math.min(values.length, v.length);
      System.arraycopy(v, 0, values, 0, len);
    }
    return result.toString();
  }

  protected final void statisticsUpdate() {
    if (cache.hash != 0) {
      return;
    }
    setScanner(0, bitsLength, null, updateStrategy);
  }

  @Override
  public String toString() {
    final StringBuilder p = new StringBuilder(200);
    p.append('{');
    int i = nextSetBit(0);
    while (i >= 0) {
      p.append(i);
      int j = nextSetBit(i + 1);
      if (compactionCount > 0) {
        if (j < 0) {
          break;
        }
        int last = nextClearBit(i);
        last = (last < 0 ? Integer.MAX_VALUE : last);
        if (i + compactionCount < last) {
          p.append("..").append(last - 1);
          j = nextSetBit(last);
        }
      }
      if (j >= 0) {
        p.append(", ");
      }
      i = j;
    }
    p.append('}');
    return p.toString();
  }

  public void toStringCompaction(int count) {
    compactionCount = count;
  }

  public void toStringCompaction(boolean change) {
    if (change) {
      compactionCountDefault = compactionCount;
    }
  }

  private void writeObject(ObjectOutputStream s) throws IOException, InternalError {
    statisticsUpdate();
    s.defaultWriteObject();
    s.writeInt(compactionCount);
    s.writeInt(cache.length);
    int count = cache.count;
    s.writeInt(count);
    final long[][][] a1 = bits;
    final int aLength1 = a1.length;
    long[][] a2;
    long[] a3;
    long word;
    for (int w1 = 0; w1 != aLength1; ++w1) {
      if ((a2 = a1[w1]) != null) {
        for (int w2 = 0; w2 != LENGTH2; ++w2) {
          if ((a3 = a2[w2]) != null) {
            final int base = (w1 << SHIFT1) + (w2 << SHIFT2);
            for (int w3 = 0; w3 != LENGTH3; ++w3) {
              if ((word = a3[w3]) != 0) {
                s.writeInt(base + w3);
                s.writeLong(word);
                --count;
              }
            }
          }
        }
      }
    }
    if (count != 0) {
      throw new InternalError("count of entries not consistent");
    }
    s.writeInt(cache.hash);
  }

  public void xor(int i, boolean value) {
    if ((i + 1) < 1) {
      throw new IndexOutOfBoundsException("i=" + i);
    }
    if (value) {
      flip(i);
    }
  }

  public void xor(int i, int j, SparseBitSet b) throws IndexOutOfBoundsException {
    setScanner(i, j, b, xorStrategy);
  }

  public void xor(SparseBitSet b) {
    setScanner(0, b.bitsLength, b, xorStrategy);
  }

  public enum Statistics {
    Size,
    Length,
    Cardinality,
    Total_words,
    Set_array_length,
    Set_array_max_length,
    Level2_areas,
    Level2_area_length,
    Level3_blocks,
    Level3_block_length,
    Compaction_count_value
  }

  protected abstract static class AbstractStrategy {
    static final int F_OP_F_EQ_F = 0x1;
    static final int F_OP_X_EQ_F = 0x2;
    static final int X_OP_F_EQ_F = 0x4;
    static final int X_OP_F_EQ_X = 0x8;

    protected abstract boolean block(int base, int u3, int v3, long[] a3, long[] b3);

    protected void finish(int a2Count, int a3Count) {
    }

    protected final boolean isZeroBlock(long[] a3) {
      for (long word : a3) {
        if (word != 0L) {
          return false;
        }
      }
      return true;
    }

    protected abstract int properties();

    protected abstract boolean start(SparseBitSet b);

    protected abstract boolean word(int base, int u3, long[] a3, long[] b3, long mask);
  }

  protected static class AndNotStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        isZero &= (a3[w3] &= ~b3[w3]) == 0L;
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + F_OP_X_EQ_F + X_OP_F_EQ_X;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      if (b == null) {
        throw new NullPointerException();
      }
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      return (a3[u3] &= ~(b3[u3] & mask)) == 0L;
    }
  }

  protected static class AndStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        isZero &= ((a3[w3] &= b3[w3]) == 0L);
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + F_OP_X_EQ_F + X_OP_F_EQ_F;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      if (b == null) {
        throw new NullPointerException();
      }
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      return (a3[u3] &= b3[u3] | ~mask) == 0L;
    }
  }

  protected static class ClearStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      if (u3 != 0 || v3 != LENGTH3) {
        for (int w3 = u3; w3 != v3; ++w3) {
          a3[w3] = 0L;
        }
      }
      return true;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + F_OP_X_EQ_F;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      return (a3[u3] &= ~mask) == 0L;
    }
  }

  protected static class CopyStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        isZero &= (a3[w3] = b3[w3]) == 0L;
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + X_OP_F_EQ_F;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      return (a3[u3] = b3[u3] & mask) == 0L;
    }
  }

  protected static class EqualsStrategy extends AbstractStrategy {
    boolean result;

    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        final long word = a3[w3];
        result &= word == b3[w3];
        isZero &= word == 0L;
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      if (b == null) {
        throw new NullPointerException();
      }
      result = true;
      return false;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      final long word = a3[u3];
      result &= (word & mask) == (b3[u3] & mask);
      return word == 0L;
    }
  }

  protected static class FlipStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        isZero &= (a3[w3] ^= ~0L) == 0L;
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return 0;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      return (a3[u3] ^= mask) == 0L;
    }
  }

  protected static class IntersectsStrategy extends AbstractStrategy {
    protected boolean result;

    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        final long word = a3[w3];
        result |= (word & b3[w3]) != 0L;
        isZero &= word == 0L;
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + F_OP_X_EQ_F;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      if (b == null) {
        throw new NullPointerException();
      }
      result = false;
      return false;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      final long word = a3[u3];
      result |= (word & b3[u3] & mask) != 0L;
      return word == 0L;
    }
  }

  protected static class OrStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        isZero &= (a3[w3] |= b3[w3]) == 0L;
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + X_OP_F_EQ_X;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      if (b == null) {
        throw new NullPointerException();
      }
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      return (a3[u3] |= b3[u3] & mask) == 0L;
    }
  }

  protected static class SetStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      for (int w3 = u3; w3 != v3; ++w3) {
        a3[w3] = ~0L;
      }
      return false;
    }

    @Override
    protected int properties() {
      return 0;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      a3[u3] |= mask;
      return false;
    }
  }

  protected static class XorStrategy extends AbstractStrategy {
    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = u3; w3 != v3; ++w3) {
        isZero &= (a3[w3] ^= b3[w3]) == 0;
      }
      return isZero;
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + X_OP_F_EQ_X;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      if (b == null) {
        throw new NullPointerException();
      }
      return true;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      return (a3[u3] ^= b3[u3] & mask) == 0;
    }
  }

  protected class Cache {
    protected transient int a2Count;
    protected transient int a3Count;
    protected transient int cardinality;
    protected transient int count;
    protected transient int hash;
    protected transient int length;
    protected transient int size;
  }

  protected class UpdateStrategy extends AbstractStrategy {
    protected transient int cardinality;
    protected transient int count;
    protected transient long hash;
    protected transient int wMax;
    protected transient int wMin;
    protected transient long wordMax;
    protected transient long wordMin;

    @Override
    protected boolean block(int base, int u3, int v3, long[] a3, long[] b3) {
      boolean isZero = true;
      for (int w3 = 0; w3 != v3; ++w3) {
        final long word = a3[w3];
        if (word != 0) {
          isZero = false;
          compute(base + w3, word);
        }
      }
      return isZero;
    }

    private void compute(final int index, final long word) {
      ++count;
      hash ^= word * (long) (index + 1);
      if (wMin < 0) {
        wMin = index;
        wordMin = word;
      }
      wMax = index;
      wordMax = word;
      cardinality += Long.bitCount(word);
    }

    @Override
    protected void finish(int a2Count, int a3Count) {
      cache.a2Count = a2Count;
      cache.a3Count = a3Count;
      cache.count = count;
      cache.cardinality = cardinality;
      cache.length = (wMax + 1) * LENGTH4 - Long.numberOfLeadingZeros(wordMax);
      cache.size = cache.length - wMin * LENGTH4
          - Long.numberOfTrailingZeros(wordMin);
      cache.hash = (int) ((hash >> Integer.SIZE) ^ hash);
    }

    @Override
    protected int properties() {
      return F_OP_F_EQ_F + F_OP_X_EQ_F;
    }

    @Override
    protected boolean start(SparseBitSet b) {
      hash = 1234L;
      wMin = -1;
      wordMin = 0L;
      wMax = 0;
      wordMax = 0L;
      count = 0;
      cardinality = 0;
      return false;
    }

    @Override
    protected boolean word(int base, int u3, long[] a3, long[] b3, long mask) {
      final long word = a3[u3];
      final long word1 = word & mask;
      if (word1 != 0L) {
        compute(base + u3, word1);
      }
      return word == 0L;
    }
  }
}
