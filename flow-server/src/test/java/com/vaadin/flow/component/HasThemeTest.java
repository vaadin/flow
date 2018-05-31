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
    public void addThemeProperty() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.addThemeProperty("foo");
        assertThemes(component, "foo");
        component.addThemeProperty("bar");
        assertThemes(component, "foo", "bar");

        // use ThemeList

        component.getThemeProperties().add("baz");
        assertThemes(component, "foo", "bar", "baz");
    }

    @Test
    public void setThemeProperty_useThemeList() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeProperty("foo bar");

        component.getThemeProperties().set("bar", false);
        assertThemes(component, "foo");
    }

    @Test
    public void removeThemeProperty() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeProperty("foo Bar baz");
        component.removeThemeProperty("foo");
        assertThemes(component, "Bar", "baz");
        component.removeThemeProperty("bar");
        assertThemes(component, "Bar", "baz");
        component.removeThemeProperty("Bar");
        assertThemes(component, "baz");
        component.removeThemeProperty("baz");
        assertThemes(component);

        // use ThemeList
        component.setThemeProperty("foo");

        component.getThemeProperties().remove("foo");
        assertThemes(component);
    }

    @Test
    public void setThemeProperty() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        Assert.assertNull(component.getThemeProperty());
        component.setThemeProperty("foo");
        assertThemes(component, "foo");
        component.setThemeProperty("bar");
        assertThemes(component, "bar");
        component.setThemeProperty("bar foo");
        assertThemes(component, "bar", "foo");
        component.setThemeProperty("");
        assertThemes(component);
        component.setThemeProperty("");
        assertThemes(component);
    }

    @Test
    public void getThemeProperty() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        Assert.assertNull(component.getThemeProperty());
        component.setThemeProperty("foo");
        Assert.assertEquals("foo", component.getThemeProperty());
        component.setThemeProperty("");
        Assert.assertEquals("", component.getThemeProperty());
    }

    @Test
    public void setThemePropertyToggle() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeProperty("foo", false);
        assertThemes(component);
        component.setThemeProperty("foo", true);
        assertThemes(component, "foo");
        component.setThemeProperty("foo", false);
        assertThemes(component);
        component.setThemeProperty("foo", true);
        component.setThemeProperty("bar", true);
        component.setThemeProperty("baz", true);
        assertThemes(component, "foo", "bar", "baz");
        component.setThemeProperty("baz", false);
        assertThemes(component, "foo", "bar");

    }

    @Test
    public void hasThemeProperty() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        Assert.assertFalse(component.hasThemeProperty("foo"));
        component.setThemeProperty("foo");
        Assert.assertTrue(component.hasThemeProperty("foo"));
        Assert.assertFalse(component.hasThemeProperty("fo"));
        component.setThemeProperty("foo bar");
        Assert.assertTrue(component.hasThemeProperty("foo"));
        Assert.assertTrue(component.hasThemeProperty("bar"));

    }

    @Test
    public void getThemeList_elementThemeList() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();

        Assert.assertEquals(component.getElement().getThemeList().isEmpty(),
                component.getThemeProperties().isEmpty());
    }

    @Test
    public void addThemeProperties() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.addThemeProperties();
        assertThemes(component);
        component.addThemeProperties("foo", "bar");
        assertThemes(component, "foo", "bar");
        component.addThemeProperties("baz1", "baz2");
        assertThemes(component, "foo", "bar", "baz1", "baz2");
    }

    @Test
    public void removeThemeProperties() {
        HasThemeTest.HasThemeComponent component = new HasThemeTest.HasThemeComponent();
        component.setThemeProperty("foo bar baz1 baz2 foo2 bar1");

        component.removeThemeProperties();
        assertThemes(component, "foo", "bar", "baz1", "baz2", "foo2", "bar1");

        component.removeThemeProperties("baz2");
        assertThemes(component, "foo", "bar", "baz1", "foo2", "bar1");

        component.removeThemeProperties("bar", "foo2", "foo");
        assertThemes(component, "baz1", "bar1");
    }

    private void assertThemes(HasThemeTest.HasThemeComponent c, String... expectedThemes) {
        Set<String> actual = c.getThemeProperties();
        Set<String> expected = new HashSet<>(
                Arrays.asList(expectedThemes));
        Assert.assertEquals(expected, actual);
    }

}
