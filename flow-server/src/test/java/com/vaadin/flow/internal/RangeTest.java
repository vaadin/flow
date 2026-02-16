/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("static-method")
class RangeTest {

    @Test
    public void startAfterEndTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            Range.between(10, 9);
        });
    }

    @Test
    public void negativeLengthTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            Range.withLength(10, -1);
        });
    }

    @Test
    public void constructorEquivalenceTest() {
        assertEquals(Range.withOnly(10), Range.between(10, 11),
                "10 == [10,11[");
        assertEquals(Range.between(10, 20), Range.withLength(10, 10),
                "[10,20[ == 10, length 10");
        assertEquals(Range.withOnly(10), Range.withLength(10, 1),
                "10 == 10, length 1");
    }

    @Test
    public void boundsTest() {
        {
            final Range range = Range.between(0, 10);
            assertEquals(0, range.getStart(), "between(0, 10) start");
            assertEquals(10, range.getEnd(), "between(0, 10) end");
        }

        {
            final Range single = Range.withOnly(10);
            assertEquals(10, single.getStart(), "withOnly(10) start");
            assertEquals(11, single.getEnd(), "withOnly(10) end");
        }

        {
            final Range length = Range.withLength(10, 5);
            assertEquals(10, length.getStart(), "withLength(10, 5) start");
            assertEquals(15, length.getEnd(), "withLength(10, 5) end");
        }
    }

    @Test
    @SuppressWarnings("boxing")
    public void equalsTest() {
        final Range range1 = Range.between(0, 10);
        final Range range2 = Range.withLength(0, 11);

        assertTrue(!range1.equals(null), "null");
        assertTrue(range1.equals(range1), "reflexive");
        assertEquals(range1.equals(range2), range2.equals(range1), "symmetric");
    }

    @Test
    public void containsTest() {
        final int start = 0;
        final int end = 10;
        final Range range = Range.between(start, end);

        assertTrue(range.contains(start), "start should be contained");
        assertTrue(!range.contains(start - 1),
                "start-1 should not be contained");
        assertTrue(!range.contains(end), "end should not be contained");
        assertTrue(range.contains(end - 1), "end-1 should be contained");

        assertTrue(Range.between(0, 10).contains(5), "[0..10[ contains 5");
        assertTrue(!Range.between(5, 5).contains(5),
                "empty range does not contain 5");
    }

    @Test
    public void emptyTest() {
        assertTrue(Range.between(0, 0).isEmpty(), "[0..0[ should be empty");
        assertTrue(Range.withLength(0, 0).isEmpty(),
                "Range of length 0 should be empty");

        assertTrue(!Range.between(0, 1).isEmpty(),
                "[0..1[ should not be empty");
        assertTrue(!Range.withLength(0, 1).isEmpty(),
                "Range of length 1 should not be empty");
    }

    @Test
    public void splitTest() {
        final Range startRange = Range.between(0, 10);
        final Range[] splitRanges = startRange.splitAt(5);
        assertEquals(Range.between(0, 5), splitRanges[0],
                "[0..10[ split at 5, lower");
        assertEquals(Range.between(5, 10), splitRanges[1],
                "[0..10[ split at 5, upper");
    }

    @Test
    public void split_valueBefore() {
        Range range = Range.between(10, 20);
        Range[] splitRanges = range.splitAt(5);

        assertEquals(Range.between(10, 10), splitRanges[0]);
        assertEquals(range, splitRanges[1]);
    }

    @Test
    public void split_valueAfter() {
        Range range = Range.between(10, 20);
        Range[] splitRanges = range.splitAt(25);

        assertEquals(range, splitRanges[0]);
        assertEquals(Range.between(20, 20), splitRanges[1]);
    }

    @Test
    public void emptySplitTest() {
        final Range range = Range.between(5, 10);
        final Range[] split1 = range.splitAt(0);
        assertTrue(split1[0].isEmpty(), "split1, [0]");
        assertEquals(range, split1[1], "split1, [1]");

        final Range[] split2 = range.splitAt(15);
        assertEquals(range, split2[0], "split2, [0]");
        assertTrue(split2[1].isEmpty(), "split2, [1]");
    }

    @Test
    public void lengthTest() {
        assertEquals(5, Range.withLength(10, 5).length(), "withLength length");
        assertEquals(5, Range.between(10, 15).length(), "between length");
        assertEquals(1, Range.withOnly(10).length(), "withOnly 10 length");
    }

    @Test
    public void intersectsTest() {
        assertTrue(Range.between(0, 10).intersects(Range.between(5, 15)),
                "[0..10[ intersects [5..15[");
        assertTrue(!Range.between(0, 10).intersects(Range.between(10, 20)),
                "[0..10[ does not intersect [10..20[");
    }

    @Test
    public void intersects_emptyInside() {
        assertTrue(Range.between(5, 5).intersects(Range.between(0, 10)),
                "[5..5[ does intersect with [0..10[");
        assertTrue(Range.between(0, 10).intersects(Range.between(5, 5)),
                "[0..10[ does intersect with [5..5[");
    }

    @Test
    public void intersects_emptyOutside() {
        assertTrue(!Range.between(15, 15).intersects(Range.between(0, 10)),
                "[15..15[ does not intersect with [0..10[");
        assertTrue(!Range.between(0, 10).intersects(Range.between(15, 15)),
                "[0..10[ does not intersect with [15..15[");
    }

    @Test
    public void subsetTest() {
        assertTrue(Range.between(5, 10).isSubsetOf(Range.between(0, 20)),
                "[5..10[ is subset of [0..20[");

        final Range range = Range.between(0, 10);
        assertTrue(range.isSubsetOf(range), "range is subset of self");

        assertTrue(!Range.between(0, 10).isSubsetOf(Range.between(5, 15)),
                "[0..10[ is not subset of [5..15[");
    }

    @Test
    public void offsetTest() {
        assertEquals(Range.between(5, 15), Range.between(0, 10).offsetBy(5));
    }

    @Test
    public void rangeStartsBeforeTest() {
        final Range former = Range.between(0, 5);
        final Range latter = Range.between(1, 5);
        assertTrue(former.startsBefore(latter),
                "former should starts before latter");
        assertTrue(!latter.startsBefore(former),
                "latter shouldn't start before latter");

        assertTrue(!Range.between(0, 5).startsBefore(Range.between(0, 10)),
                "no overlap allowed");
    }

    @Test
    public void rangeStartsAfterTest() {
        final Range former = Range.between(0, 5);
        final Range latter = Range.between(5, 10);
        assertTrue(latter.startsAfter(former),
                "latter should start after former");
        assertTrue(!former.startsAfter(latter),
                "former shouldn't start after latter");

        assertTrue(!Range.between(5, 10).startsAfter(Range.between(0, 6)),
                "no overlap allowed");
    }

    @Test
    public void rangeEndsBeforeTest() {
        final Range former = Range.between(0, 5);
        final Range latter = Range.between(5, 10);
        assertTrue(former.endsBefore(latter),
                "latter should end before former");
        assertTrue(!latter.endsBefore(former),
                "former shouldn't end before latter");

        assertTrue(!Range.between(5, 10).endsBefore(Range.between(9, 15)),
                "no overlap allowed");
    }

    @Test
    public void rangeEndsAfterTest() {
        final Range former = Range.between(1, 5);
        final Range latter = Range.between(1, 6);
        assertTrue(latter.endsAfter(former), "latter should end after former");
        assertTrue(!former.endsAfter(latter),
                "former shouldn't end after latter");

        assertTrue(!Range.between(0, 10).endsAfter(Range.between(5, 10)),
                "no overlap allowed");
    }

    @Test
    public void combine_notOverlappingFirstSmaller() {
        Range r1 = Range.between(0, 10);
        Range r2 = Range.between(11, 20);
        assertThrows(IllegalArgumentException.class, () -> r1.combineWith(r2));
    }

    @Test
    public void combine_notOverlappingSecondLarger() {
        Range r1 = Range.between(11, 20);
        Range r2 = Range.between(0, 10);
        assertThrows(IllegalArgumentException.class, () -> r1.combineWith(r2));
    }

    @Test
    public void combine_firstEmptyNotOverlapping() {
        Range r1 = Range.between(15, 15);
        Range r2 = Range.between(0, 10);
        assertThrows(IllegalArgumentException.class, () -> r1.combineWith(r2));
    }

    @Test
    public void combine_secondEmptyNotOverlapping() {
        Range r1 = Range.between(0, 10);
        Range r2 = Range.between(15, 15);
        assertThrows(IllegalArgumentException.class, () -> r1.combineWith(r2));
    }

    @Test
    public void combine_barelyOverlapping() {
        Range r1 = Range.between(0, 10);
        Range r2 = Range.between(10, 20);

        // Test both ways, should give the same result
        Range combined1 = r1.combineWith(r2);
        Range combined2 = r2.combineWith(r1);
        assertEquals(combined1, combined2);

        assertEquals(0, combined1.getStart());
        assertEquals(20, combined1.getEnd());
    }

    @Test
    public void combine_subRange() {
        Range r1 = Range.between(0, 10);
        Range r2 = Range.between(2, 8);

        // Test both ways, should give the same result
        Range combined1 = r1.combineWith(r2);
        Range combined2 = r2.combineWith(r1);
        assertEquals(combined1, combined2);

        assertEquals(r1, combined1);
    }

    @Test
    public void combine_intersecting() {
        Range r1 = Range.between(0, 10);
        Range r2 = Range.between(5, 15);

        // Test both ways, should give the same result
        Range combined1 = r1.combineWith(r2);
        Range combined2 = r2.combineWith(r1);
        assertEquals(combined1, combined2);

        assertEquals(0, combined1.getStart());
        assertEquals(15, combined1.getEnd());

    }

    @Test
    public void combine_emptyInside() {
        Range r1 = Range.between(0, 10);
        Range r2 = Range.between(5, 5);

        // Test both ways, should give the same result
        Range combined1 = r1.combineWith(r2);
        Range combined2 = r2.combineWith(r1);
        assertEquals(combined1, combined2);

        assertEquals(r1, combined1);
    }

    @Test
    public void expand_basic() {
        Range r1 = Range.between(5, 10);
        Range r2 = r1.expand(2, 3);

        assertEquals(Range.between(3, 13), r2);
    }

    @Test
    public void expand_negativeLegal() {
        Range r1 = Range.between(5, 10);

        Range r2 = r1.expand(-2, -2);
        assertEquals(Range.between(7, 8), r2);

        Range r3 = r1.expand(-3, -2);
        assertEquals(Range.between(8, 8), r3);

        Range r4 = r1.expand(3, -8);
        assertEquals(Range.between(2, 2), r4);
    }

    @Test
    public void expand_negativeIllegal1() {
        Range r1 = Range.between(5, 10);
        // Should throw because the start would contract beyond the end
        assertThrows(IllegalArgumentException.class, () -> r1.expand(-3, -3));
    }

    @Test
    public void expand_negativeIllegal2() {
        Range r1 = Range.between(5, 10);
        // Should throw because the end would contract beyond the start
        assertThrows(IllegalArgumentException.class, () -> r1.expand(3, -9));
    }

    @Test
    public void restrictTo_fullyInside() {
        Range r1 = Range.between(5, 10);
        Range r2 = Range.between(4, 11);

        Range r3 = r1.restrictTo(r2);
        assertTrue(r1 == r3);
    }

    @Test
    public void restrictTo_fullyOutside() {
        Range r1 = Range.between(4, 11);
        Range r2 = Range.between(5, 10);

        Range r3 = r1.restrictTo(r2);
        assertTrue(r2 == r3);
    }

    @Test
    public void restrictTo_notInterstecting() {
        Range r1 = Range.between(5, 10);
        Range r2 = Range.between(15, 20);

        Range r3 = r1.restrictTo(r2);
        assertTrue(r3.isEmpty(),
                "Non-intersecting ranges should produce an empty result");

        Range r4 = r2.restrictTo(r1);
        assertTrue(r4.isEmpty(),
                "Non-intersecting ranges should produce an empty result");
    }

    @Test
    public void restrictTo_startOutside() {
        Range r1 = Range.between(5, 10);
        Range r2 = Range.between(7, 15);

        Range r3 = r1.restrictTo(r2);

        assertEquals(Range.between(7, 10), r3);

        assertEquals(r2.restrictTo(r1), r3);
    }

    @Test
    public void restrictTo_endOutside() {
        Range r1 = Range.between(5, 10);
        Range r2 = Range.between(4, 7);

        Range r3 = r1.restrictTo(r2);

        assertEquals(Range.between(5, 7), r3);

        assertEquals(r2.restrictTo(r1), r3);
    }

    @Test
    public void restrictTo_empty() {
        Range r1 = Range.between(5, 10);
        Range r2 = Range.between(0, 0);

        Range r3 = r1.restrictTo(r2);
        assertTrue(r3.isEmpty());

        Range r4 = r2.restrictTo(r1);
        assertTrue(r4.isEmpty());
    }

}
