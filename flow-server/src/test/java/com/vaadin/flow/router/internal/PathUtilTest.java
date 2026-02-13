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
package com.vaadin.flow.router.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathUtilTest {

    @Test
    public void methods_output_expected_values() {
        final List<String> segments = Arrays.asList("path", "to", "foo");
        final String path = "path/to/foo";

        assertEquals(path, PathUtil.getPath(segments), "Unexpected result");
        assertEquals("", PathUtil.getPath(null), "Unexpected result");
        assertEquals("", PathUtil.getPath(Collections.emptyList()),
                "Unexpected result");

        assertEquals("prefix/" + path, PathUtil.getPath("prefix", segments),
                "Unexpected result");
        assertEquals(path, PathUtil.getPath("", segments), "Unexpected result");
        assertEquals(path, PathUtil.getPath(null, segments),
                "Unexpected result");

        assertEquals(segments, PathUtil.getSegmentsList(path),
                "Unexpected result");
        assertEquals(segments, PathUtil.getSegmentsList(path + "/"),
                "Unexpected result");

        List<String> emptyStartSegment = new ArrayList<>();
        emptyStartSegment.add("");
        emptyStartSegment.addAll(segments);
        assertEquals(emptyStartSegment, PathUtil.getSegmentsList("/" + path),
                "Unexpected result");
        assertEquals(emptyStartSegment,
                PathUtil.getSegmentsList("/" + path + "/"),
                "Unexpected result");

        assertEquals(path, PathUtil.trimPath(path), "Unexpected result");
        assertEquals(path, PathUtil.trimPath("/" + path), "Unexpected result");
        assertEquals(path, PathUtil.trimPath(path + "/"), "Unexpected result");
        assertEquals(path, PathUtil.trimPath("/" + path + "/"),
                "Unexpected result");

    }

    @Test
    public void getSegmentsListWithDecoding_decodesEncodedSlashes() {
        // Test that %2F in a segment is decoded to / but doesn't split the
        // segment
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("path%2Fwith%2Fslashes");
        assertEquals(1, segments.size(), "Should have one segment");
        assertEquals("path/with/slashes", segments.get(0),
                "Should decode %2F to /");
    }

    @Test
    public void getSegmentsListWithDecoding_decodesSpaces() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("hello%20world");
        assertEquals(1, segments.size(), "Should have one segment");
        assertEquals("hello world", segments.get(0),
                "Should decode %20 to space");
    }

    @Test
    public void getSegmentsListWithDecoding_decodesSpecialCharacters() {
        // Test various special characters
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("test%3Fquestion/value%26data");
        assertEquals(2, segments.size(), "Should have two segments");
        assertEquals("test?question", segments.get(0),
                "Should decode %3F to ?");
        assertEquals("value&data", segments.get(1), "Should decode %26 to &");
    }

    @Test
    public void getSegmentsListWithDecoding_decodesPlus() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("a%2Bb/c%2Bd");
        assertEquals(2, segments.size(), "Should have two segments");
        assertEquals("a+b", segments.get(0), "Should decode %2B to +");
        assertEquals("c+d", segments.get(1), "Should decode %2B to +");
    }

    @Test
    public void getSegmentsListWithDecoding_decodesHash() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("item%23123");
        assertEquals(1, segments.size(), "Should have one segment");
        assertEquals("item#123", segments.get(0), "Should decode %23 to #");
    }

    @Test
    public void getSegmentsListWithDecoding_decodesPercent() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("50%25off");
        assertEquals(1, segments.size(), "Should have one segment");
        assertEquals("50%off", segments.get(0), "Should decode %25 to %");
    }

    @Test
    public void getSegmentsListWithDecoding_handlesMultipleEncodedSegments() {
        List<String> segments = PathUtil.getSegmentsListWithDecoding(
                "path%2Fwith%2Fslashes/normal/another%2Fencoded");
        assertEquals(3, segments.size(), "Should have three segments");
        assertEquals("path/with/slashes", segments.get(0),
                "First segment should be decoded");
        assertEquals("normal", segments.get(1),
                "Second segment should be normal");
        assertEquals("another/encoded", segments.get(2),
                "Third segment should be decoded");
    }

    @Test
    public void getSegmentsListWithDecoding_handlesEmptyPath() {
        List<String> segments = PathUtil.getSegmentsListWithDecoding("");
        assertTrue(segments.isEmpty(), "Empty path should return empty list");
    }

    @Test
    public void getSegmentsListWithDecoding_handlesNullPath() {
        List<String> segments = PathUtil.getSegmentsListWithDecoding(null);
        assertTrue(segments.isEmpty(), "Null path should return empty list");
    }

    @Test
    public void getSegmentsListWithDecoding_handlesLeadingSlash() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("/path%2Fencoded/normal");
        // Leading slash creates empty first segment
        assertEquals(3, segments.size(), "Should have three segments");
        assertEquals("", segments.get(0), "First segment should be empty");
        assertEquals("path/encoded", segments.get(1),
                "Second segment should be decoded");
        assertEquals("normal", segments.get(2),
                "Third segment should be normal");
    }

    @Test
    public void getSegmentsListWithDecoding_handlesTrailingSlash() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("path%2Fencoded/");
        assertEquals(1, segments.size(), "Should have one segment");
        assertEquals("path/encoded", segments.get(0),
                "Segment should be decoded");
    }

    @Test
    public void getSegmentsListWithDecoding_handlesUtf8Characters() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("hello%C3%A4%C3%B6%C3%BC");
        assertEquals(1, segments.size(), "Should have one segment");
        assertEquals("helloäöü", segments.get(0),
                "Should decode UTF-8 characters");
    }

    @Test
    public void getSegmentsList_doesNotDecode() {
        // Verify existing behavior: getSegmentsList does NOT decode
        List<String> segments = PathUtil
                .getSegmentsList("path%2Fwith%2Fslashes");
        assertEquals(1, segments.size(), "Should have one segment");
        assertEquals("path%2Fwith%2Fslashes", segments.get(0),
                "Should NOT decode %2F");
    }

}
