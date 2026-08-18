// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import com.google.common.collect.testing.features.CollectionFeature;
import java.util.Set;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.roaringbitmap.RoaringBitmap;

/**
 * Guava suite for a hybrid set started in Roaring mode.
 */
@SuppressWarnings({"PMD.JUnit4SuitesShouldUseSuiteAnnotation", "PMD.UseUtilityClass"})
@RunWith(AllTests.class)
public class HybridRoaringModeGuavaTest {
    public static TestSuite suite() {
        return GuavaSetTest.createNatSet(
                () -> new HybridNatBitSet(new RoaringBitmap()),
                "HybridRoaringModeGuavaTest",
                Set.of(CollectionFeature.SUPPORTS_ITERATOR_REMOVE));
    }
}
