// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import java.util.Set;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

@SuppressWarnings({"PMD.JUnit4SuitesShouldUseSuiteAnnotation", "PMD.UseUtilityClass"})
@RunWith(AllTests.class)
public class HybridNatBitSetGuavaTest {
    public static TestSuite suite() {
        return GuavaSetTest.createNatSet(HybridNatBitSet::new, "HybridNatBitSetGuavaTest", Set.of());
    }
}
