// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.map;

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.MapHashCodeTester;
import com.google.common.collect.testing.testers.SetHashCodeTester;
import java.util.Map;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@SuppressWarnings({"PMD.JUnit4SuitesShouldUseSuiteAnnotation", "PMD.UseUtilityClass"})
@RunWith(AllTests.class)
public class Nat2IntDenseMapGuavaTest {
    public static TestSuite suite() {
        return MapTestSuiteBuilder.using(new Nat2NatMapGenerator())
                .named("Nat2IntDenseMapTest")
                .withFeatures(
                        MapFeature.SUPPORTS_PUT,
                        MapFeature.SUPPORTS_REMOVE,
                        MapFeature.ALLOWS_ANY_NULL_QUERIES,
                        CollectionFeature.KNOWN_ORDER,
                        CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                        CollectionFeature.NON_STANDARD_TOSTRING,
                        CollectionSize.ANY)
                .suppressing(MapHashCodeTester.class.getMethods())
                .suppressing(SetHashCodeTester.class.getMethods())
                .createTestSuite();
    }

    private static final class Nat2NatMapGenerator extends AbstractNatMapGenerator<Double> {
        @Override
        Double[] sampleValues() {
            return new Double[] {1.123, 0.0, Double.MIN_NORMAL, Double.MAX_VALUE, -Math.PI};
        }

        @Override
        Map<Integer, Double> map() {
            return new Nat2DoubleDenseArrayMap(1);
        }
    }
}
