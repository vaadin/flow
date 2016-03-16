/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public class LocationTest {
    @Test
    public void parseLocation() {
        Location location = new Location("foo/bar/baz");

        Assert.assertEquals(Arrays.asList("foo", "bar", "baz"),
                location.getSegments());
        Assert.assertEquals("foo/bar/baz", location.getPath());
    }

    @Test
    public void parseLocationWithEndingSlash() {
        Location location = new Location("foo/bar/");

        Assert.assertEquals(Arrays.asList("foo", "bar", ""),
                location.getSegments());
    }

    @Test(expected = AssertionError.class)
    public void parseLocationStartingWithSlash() {
        new Location("/foo/bar");
    }

    @Test(expected = AssertionError.class)
    public void parseLocationWithQueryString() {
        new Location("path?query");
    }

    @Test
    public void locationFromSegments() {
        Location location = new Location(Arrays.asList("one", "two"));
        Assert.assertEquals(Arrays.asList("one", "two"),
                location.getSegments());
        Assert.assertEquals("one/two", location.getPath());
    }

    @Test
    public void subLocation() {
        Location location = new Location(Arrays.asList("one", "two", "three"));

        Assert.assertEquals("one", location.getFirstSegment());

        Location subLocation = location.getSubLocation();
        Assert.assertEquals(Arrays.asList("two", "three"),
                subLocation.getSegments());
        Assert.assertEquals("two/three", subLocation.getPath());

    }

    @Test
    public void emptyLocation() {
        Location location = new Location(Collections.emptyList());

        Assert.assertNull(location.getFirstSegment());
        Assert.assertEquals("", location.getPath());
    }

    @Test(expected = IllegalStateException.class)
    public void emptyLocation_subLocation_throws() {
        Location location = new Location(Collections.emptyList());
        location.getSubLocation();
    }

}
