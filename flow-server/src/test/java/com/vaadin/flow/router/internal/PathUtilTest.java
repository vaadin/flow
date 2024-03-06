/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

}
