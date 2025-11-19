/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

public class PathUtilTest {

    @Test
    public void methods_output_expected_values() {
        final List<String> segments = Arrays.asList("path", "to", "foo");
        final String path = "path/to/foo";

        Assert.assertEquals("Unexpected result", path,
                PathUtil.getPath(segments));
        Assert.assertEquals("Unexpected result", "", PathUtil.getPath(null));
        Assert.assertEquals("Unexpected result", "",
                PathUtil.getPath(Collections.emptyList()));

        Assert.assertEquals("Unexpected result", "prefix/" + path,
                PathUtil.getPath("prefix", segments));
        Assert.assertEquals("Unexpected result", path,
                PathUtil.getPath("", segments));
        Assert.assertEquals("Unexpected result", path,
                PathUtil.getPath(null, segments));

        Assert.assertEquals("Unexpected result", segments,
                PathUtil.getSegmentsList(path));
        Assert.assertEquals("Unexpected result", segments,
                PathUtil.getSegmentsList(path + "/"));

        List<String> emptyStartSegment = new ArrayList<>();
        emptyStartSegment.add("");
        emptyStartSegment.addAll(segments);
        Assert.assertEquals("Unexpected result", emptyStartSegment,
                PathUtil.getSegmentsList("/" + path));
        Assert.assertEquals("Unexpected result", emptyStartSegment,
                PathUtil.getSegmentsList("/" + path + "/"));

        Assert.assertEquals("Unexpected result", path, PathUtil.trimPath(path));
        Assert.assertEquals("Unexpected result", path,
                PathUtil.trimPath("/" + path));
        Assert.assertEquals("Unexpected result", path,
                PathUtil.trimPath(path + "/"));
        Assert.assertEquals("Unexpected result", path,
                PathUtil.trimPath("/" + path + "/"));

    }

    @Test
    public void getSegmentsListWithDecoding_decodesEncodedSlashes() {
        // Test that %2F in a segment is decoded to / but doesn't split the segment
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("path%2Fwith%2Fslashes");
        Assert.assertEquals("Should have one segment", 1, segments.size());
        Assert.assertEquals("Should decode %2F to /", "path/with/slashes",
                segments.get(0));
    }

    @Test
    public void getSegmentsListWithDecoding_decodesSpaces() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("hello%20world");
        Assert.assertEquals("Should have one segment", 1, segments.size());
        Assert.assertEquals("Should decode %20 to space", "hello world",
                segments.get(0));
    }

    @Test
    public void getSegmentsListWithDecoding_decodesSpecialCharacters() {
        // Test various special characters
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("test%3Fquestion/value%26data");
        Assert.assertEquals("Should have two segments", 2, segments.size());
        Assert.assertEquals("Should decode %3F to ?", "test?question",
                segments.get(0));
        Assert.assertEquals("Should decode %26 to &", "value&data",
                segments.get(1));
    }

    @Test
    public void getSegmentsListWithDecoding_decodesPlus() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("a%2Bb/c%2Bd");
        Assert.assertEquals("Should have two segments", 2, segments.size());
        Assert.assertEquals("Should decode %2B to +", "a+b", segments.get(0));
        Assert.assertEquals("Should decode %2B to +", "c+d", segments.get(1));
    }

    @Test
    public void getSegmentsListWithDecoding_decodesHash() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("item%23123");
        Assert.assertEquals("Should have one segment", 1, segments.size());
        Assert.assertEquals("Should decode %23 to #", "item#123",
                segments.get(0));
    }

    @Test
    public void getSegmentsListWithDecoding_decodesPercent() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("50%25off");
        Assert.assertEquals("Should have one segment", 1, segments.size());
        Assert.assertEquals("Should decode %25 to %", "50%off",
                segments.get(0));
    }

    @Test
    public void getSegmentsListWithDecoding_handlesMultipleEncodedSegments() {
        List<String> segments = PathUtil.getSegmentsListWithDecoding(
                "path%2Fwith%2Fslashes/normal/another%2Fencoded");
        Assert.assertEquals("Should have three segments", 3, segments.size());
        Assert.assertEquals("First segment should be decoded",
                "path/with/slashes", segments.get(0));
        Assert.assertEquals("Second segment should be normal", "normal",
                segments.get(1));
        Assert.assertEquals("Third segment should be decoded",
                "another/encoded", segments.get(2));
    }

    @Test
    public void getSegmentsListWithDecoding_handlesEmptyPath() {
        List<String> segments = PathUtil.getSegmentsListWithDecoding("");
        Assert.assertTrue("Empty path should return empty list",
                segments.isEmpty());
    }

    @Test
    public void getSegmentsListWithDecoding_handlesNullPath() {
        List<String> segments = PathUtil.getSegmentsListWithDecoding(null);
        Assert.assertTrue("Null path should return empty list",
                segments.isEmpty());
    }

    @Test
    public void getSegmentsListWithDecoding_handlesLeadingSlash() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("/path%2Fencoded/normal");
        // Leading slash creates empty first segment
        Assert.assertEquals("Should have three segments", 3, segments.size());
        Assert.assertEquals("First segment should be empty", "",
                segments.get(0));
        Assert.assertEquals("Second segment should be decoded", "path/encoded",
                segments.get(1));
        Assert.assertEquals("Third segment should be normal", "normal",
                segments.get(2));
    }

    @Test
    public void getSegmentsListWithDecoding_handlesTrailingSlash() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("path%2Fencoded/");
        Assert.assertEquals("Should have one segment", 1, segments.size());
        Assert.assertEquals("Segment should be decoded", "path/encoded",
                segments.get(0));
    }

    @Test
    public void getSegmentsListWithDecoding_handlesUtf8Characters() {
        List<String> segments = PathUtil
                .getSegmentsListWithDecoding("hello%C3%A4%C3%B6%C3%BC");
        Assert.assertEquals("Should have one segment", 1, segments.size());
        Assert.assertEquals("Should decode UTF-8 characters", "helloäöü",
                segments.get(0));
    }

    @Test
    public void getSegmentsList_doesNotDecode() {
        // Verify existing behavior: getSegmentsList does NOT decode
        List<String> segments = PathUtil
                .getSegmentsList("path%2Fwith%2Fslashes");
        Assert.assertEquals("Should have one segment", 1, segments.size());
        Assert.assertEquals("Should NOT decode %2F", "path%2Fwith%2Fslashes",
                segments.get(0));
    }

}
