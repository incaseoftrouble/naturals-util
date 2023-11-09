/*
 * Copyright (C) 2022 Tobias Meggendorfer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tum.in.naturals.set;

import com.google.common.collect.testing.features.CollectionFeature;
import java.util.Set;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.roaringbitmap.RoaringBitmap;

@SuppressWarnings({"PMD.JUnit4SuitesShouldUseSuiteAnnotation", "PMD.UseUtilityClass"})
@RunWith(AllTests.class)
public class RoaringNatBitSetGuavaTest {
    public static TestSuite suite() {
        return GuavaSetTest.createNatSet(
                () -> new RoaringNatBitSet(new RoaringBitmap()),
                "RoaringNatBitSetGuavaTest",
                Set.of(CollectionFeature.SUPPORTS_ITERATOR_REMOVE));
    }
}
