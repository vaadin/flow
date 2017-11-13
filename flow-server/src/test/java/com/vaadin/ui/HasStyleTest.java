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

import com.vaadin.ui.common.HasStyle;

public class HasStyleTest {

    @Tag("div")
    public static class HasStyleComponent extends Component
            implements HasStyle {

    }

    @Test
    public void addClassName() {
        HasStyleComponent component = new HasStyleComponent();
        component.addClassName("foo");
        assertClasses(component, "foo");
        component.addClassName("bar");
        assertClasses(component, "foo", "bar");

        // use ClassList

        component.getClassNames().add("baz");
        assertClasses(component, "foo", "bar", "baz");
    }

    @Test
    public void setClassName_useClassList() {
        HasStyleComponent component = new HasStyleComponent();
        component.setClassName("foo bar");

        component.getClassNames().set("bar", false);
        assertClasses(component, "foo");
    }

    @Test
    public void removeClassName() {
        HasStyleComponent component = new HasStyleComponent();
        component.setClassName("foo Bar baz");
        component.removeClassName("foo");
        assertClasses(component, "Bar", "baz");
        component.removeClassName("bar");
        assertClasses(component, "Bar", "baz");
        component.removeClassName("Bar");
        assertClasses(component, "baz");
        component.removeClassName("baz");
        assertClasses(component);

        // use ClassList
        component.setClassName("foo");

        component.getClassNames().remove("foo");
        assertClasses(component);
    }

    @Test
    public void setClassName() {
        HasStyleComponent component = new HasStyleComponent();
        component.setClassName("foo");
        assertClasses(component, "foo");
        component.setClassName("bar");
        assertClasses(component, "bar");
        component.setClassName("bar foo");
        assertClasses(component, "bar", "foo");
        component.setClassName(" ");
        assertClasses(component);
        component.setClassName("");
        assertClasses(component);
    }

    @Test
    public void getClassName() {
        HasStyleComponent component = new HasStyleComponent();
        component.setClassName("foo");
        Assert.assertEquals("foo", component.getClassName());
        component.setClassName(" ");
        Assert.assertNull(component.getClassName());
    }

    @Test
    public void setClassNameToggle() {
        HasStyleComponent component = new HasStyleComponent();
        component.setClassName("foo", false);
        assertClasses(component);
        component.setClassName("foo", true);
        assertClasses(component, "foo");
        component.setClassName("foo", false);
        assertClasses(component);
        component.setClassName("foo", true);
        component.setClassName("bar", true);
        component.setClassName("baz", true);
        assertClasses(component, "foo", "bar", "baz");
        component.setClassName("baz", false);
        assertClasses(component, "foo", "bar");

    }

    @Test
    public void hasClassName() {
        HasStyleComponent component = new HasStyleComponent();
        Assert.assertFalse(component.hasClassName("foo"));
        component.setClassName("foo");
        Assert.assertTrue(component.hasClassName("foo"));
        Assert.assertFalse(component.hasClassName("fo"));
        component.setClassName("foo bar");
        Assert.assertTrue(component.hasClassName("foo"));
        Assert.assertTrue(component.hasClassName("bar"));

    }

    @Test
    public void getClassList_elementClassList() {
        HasStyleComponent component = new HasStyleComponent();

        Assert.assertEquals(component.getElement().getClassList(),
                component.getClassNames());
    }

    private void assertClasses(HasStyleComponent c, String... expectedClasses) {
        Set<String> actual = c.getClassNames();
        HashSet<String> expected = new HashSet<String>(
                Arrays.asList(expectedClasses));
        Assert.assertEquals(expected, actual);
    }
}
