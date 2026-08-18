// SPDX-License-Identifier: Apache-2.0

package de.tum.in.naturals.set;

import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.Feature;
import com.google.common.collect.testing.testers.CollectionRetainAllTester;
import com.google.common.collect.testing.testers.CollectionSpliteratorTester;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;
import junit.framework.TestSuite;

public final class GuavaSetTest {
    private static final Collection<Method> suppression;

    static {
        try {
            suppression = Arrays.asList(
                    CollectionRetainAllTester.class.getMethod("testRetainAll_nullSingletonPreviouslyNonEmpty"),
                    CollectionSpliteratorTester.class.getMethod("testSpliteratorKnownOrder"));
        } catch (NoSuchMethodException e) {
            //noinspection ProhibitedExceptionThrown
            throw new RuntimeException(e); // NOPMD
        }
    }

    public static TestSuite createDefault(Supplier<? extends IntSet> supplier, String name) {
        return SetTestSuiteBuilder.using(new SetGenerator(supplier))
                .named(name)
                .withFeatures(
                        CollectionFeature.GENERAL_PURPOSE,
                        CollectionFeature.KNOWN_ORDER,
                        CollectionFeature.NON_STANDARD_TOSTRING,
                        CollectionSize.ANY)
                .suppressing(suppression)
                .createTestSuite();
    }

    public static TestSuite createNatSet(
            Supplier<? extends NatBitSet> supplier, String name, Collection<? extends Feature<?>> removeFeatures) {
        Collection<Feature<?>> features = new HashSet<>(Arrays.asList(
                CollectionFeature.SUPPORTS_ADD,
                CollectionFeature.SUPPORTS_REMOVE,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                CollectionFeature.KNOWN_ORDER,
                CollectionFeature.NON_STANDARD_TOSTRING,
                CollectionSize.ANY));
        features.removeAll(removeFeatures);

        return SetTestSuiteBuilder.using(new SetGenerator(supplier))
                .named(name)
                .withFeatures(features)
                .suppressing(suppression)
                .createTestSuite();
    }

    private GuavaSetTest() {}
}
