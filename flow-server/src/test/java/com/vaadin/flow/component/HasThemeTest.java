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
package com.vaadin.flow.component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class HasThemeTest {

    @Tag("div")
    public static class HasThemeComponent extends Component
            implements HasTheme {

    }

    @Test
    public void addThemeName() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.addThemeName("foo");
        assertThemes(component, "foo");
        component.addThemeName("bar");
        assertThemes(component, "foo", "bar");

        // use ThemeList

        component.getThemeNames().add("baz");
        assertThemes(component, "foo", "bar", "baz");
    }

    @Test
    public void setThemeName_useThemeList() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeName("foo bar");

        component.getThemeNames().set("bar", false);
        assertThemes(component, "foo");
    }

    @Test
    public void removeThemeName() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeName("foo Bar baz");
        component.removeThemeName("foo");
        assertThemes(component, "Bar", "baz");
        component.removeThemeName("bar");
        assertThemes(component, "Bar", "baz");
        component.removeThemeName("Bar");
        assertThemes(component, "baz");
        component.removeThemeName("baz");
        assertThemes(component);

        // use ThemeList
        component.setThemeName("foo");

        component.getThemeNames().remove("foo");
        assertThemes(component);
    }

    @Test
    public void setThemeName() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        Assert.assertNull(component.getThemeName());
        component.setThemeName("foo");
        assertThemes(component, "foo");
        component.setThemeName("bar");
        assertThemes(component, "bar");
        component.setThemeName("bar foo");
        assertThemes(component, "bar", "foo");
        component.setThemeName("");
        assertThemes(component);
        component.setThemeName("");
        assertThemes(component);
    }

    @Test
    public void getThemeName() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        Assert.assertNull(component.getThemeName());
        component.setThemeName("foo");
        Assert.assertEquals("foo", component.getThemeName());
        component.setThemeName("");
        Assert.assertEquals("", component.getThemeName());
    }

    @Test
    public void setThemeNameToggle() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeName("foo", false);
        assertThemes(component);
        component.setThemeName("foo", true);
        assertThemes(component, "foo");
        component.setThemeName("foo", false);
        assertThemes(component);
        component.setThemeName("foo", true);
        component.setThemeName("bar", true);
        component.setThemeName("baz", true);
        assertThemes(component, "foo", "bar", "baz");
        component.setThemeName("baz", false);
        assertThemes(component, "foo", "bar");

    }

    @Test
    public void hasThemeName() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        Assert.assertFalse(component.hasThemeName("foo"));
        component.setThemeName("foo");
        Assert.assertTrue(component.hasThemeName("foo"));
        Assert.assertFalse(component.hasThemeName("fo"));
        component.setThemeName("foo bar");
        Assert.assertTrue(component.hasThemeName("foo"));
        Assert.assertTrue(component.hasThemeName("bar"));

    }

    @Test
    public void getThemeList_elementThemeList() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();

        Assert.assertEquals(component.getElement().getThemeList().isEmpty(),
                component.getThemeNames().isEmpty());
    }

    @Test
    public void testAddThemeNames() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.addThemeNames();
        assertThemes(component);
        component.addThemeNames("foo", "bar");
        assertThemes(component, "foo", "bar");
        component.addThemeNames("baz1", "baz2");
        assertThemes(component, "foo", "bar", "baz1", "baz2");
    }

    @Test
    public void testRemoveThemeNames() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeName("foo bar baz1 baz2 foo2 bar1");

        component.removeThemeNames();
        assertThemes(component, "foo", "bar", "baz1", "baz2", "foo2", "bar1");

        component.removeThemeNames("baz2");
        assertThemes(component, "foo", "bar", "baz1", "foo2", "bar1");

        component.removeThemeNames("bar", "foo2", "foo");
        assertThemes(component, "baz1", "bar1");
    }

    private void assertThemes(HasThemeTest.HasThemeComponent c,
            String... expectedThemes) {
        Set<String> actual = c.getThemeNames();
        Set<String> expected = new HashSet<>(Arrays.asList(expectedThemes));
        Assert.assertEquals(expected, actual);
    }

}
