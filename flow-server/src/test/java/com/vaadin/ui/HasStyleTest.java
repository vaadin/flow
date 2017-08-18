/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.Tag;

public class HasStyleTest {

    @Tag("div")
    public static class HasStyleComponent extends Component
            implements HasStyle {

    }

    @Test
    public void addClassName() {
        HasStyleComponent c = new HasStyleComponent();
        c.addClassName("foo");
        assertClasses(c, "foo");
        c.addClassName("bar");
        assertClasses(c, "foo", "bar");
    }

    private void assertClasses(HasStyleComponent c, String... expectedClasses) {
        Set<String> actual = c.getClassNames();
        HashSet<String> expected = new HashSet<String>(
                Arrays.asList(expectedClasses));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void removeClassName() {
        HasStyleComponent c = new HasStyleComponent();
        c.setClassName("foo Bar baz");
        c.removeClassName("foo");
        assertClasses(c, "Bar", "baz");
        c.removeClassName("bar");
        assertClasses(c, "Bar", "baz");
        c.removeClassName("Bar");
        assertClasses(c, "baz");
        c.removeClassName("baz");
        assertClasses(c);
    }

    @Test
    public void setClassName() {
        HasStyleComponent c = new HasStyleComponent();
        c.setClassName("foo");
        assertClasses(c, "foo");
        c.setClassName("bar");
        assertClasses(c, "bar");
        c.setClassName("bar foo");
        assertClasses(c, "bar", "foo");
        c.setClassName(" ");
        assertClasses(c);
        c.setClassName("");
        assertClasses(c);
    }

    @Test
    public void getClassName() {
        HasStyleComponent c = new HasStyleComponent();
        c.setClassName("foo");
        Assert.assertEquals("foo", c.getClassName());
        c.setClassName(" ");
        Assert.assertNull(c.getClassName());
    }

    @Test
    public void setClassNameToggle() {
        HasStyleComponent c = new HasStyleComponent();
        c.setClassName("foo", false);
        assertClasses(c);
        c.setClassName("foo", true);
        assertClasses(c, "foo");
        c.setClassName("foo", false);
        assertClasses(c);
        c.setClassName("foo", true);
        c.setClassName("bar", true);
        c.setClassName("baz", true);
        assertClasses(c, "foo", "bar", "baz");
        c.setClassName("baz", false);
        assertClasses(c, "foo", "bar");

    }

    @Test
    public void hasClassName() {
        HasStyleComponent c = new HasStyleComponent();
        Assert.assertFalse(c.hasClassName("foo"));
        c.setClassName("foo");
        Assert.assertTrue(c.hasClassName("foo"));
        Assert.assertFalse(c.hasClassName("fo"));
        c.setClassName("foo bar");
        Assert.assertTrue(c.hasClassName("foo"));
        Assert.assertTrue(c.hasClassName("bar"));

    }
}
