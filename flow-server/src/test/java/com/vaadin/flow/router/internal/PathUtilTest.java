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

}
