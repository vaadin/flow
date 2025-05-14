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
package com.vaadin.flow.dom.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.vaadin.flow.dom.Element;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ThemeListImplTest {

    private static class MockElement extends Element {
        private final Map<String, String> attributesMap = new HashMap<>();

        private MockElement(String... themeNames) {
            super("mock-element");
            if (themeNames.length > 0) {
                attributesMap.put(ThemeListImpl.THEME_ATTRIBUTE_NAME,
                        Stream.of(themeNames).collect(Collectors.joining(" ")));
            }
        }

        @Override
        public Element setAttribute(String attribute, String value) {
            attributesMap.put(attribute, value);
            return this;
        }

        @Override
        public String getAttribute(String attribute) {
            return attributesMap.get(attribute);
        }

        @Override
        public Element removeAttribute(String attribute) {
            attributesMap.remove(attribute);
            return this;
        }
    }

    @Test
    public void themeListCreatedWithNoThemes() {
        ThemeListImpl emptyList = new ThemeListImpl(new MockElement());

        assertTrue(
                "ThemeList created from the element without themes should be empty",
                emptyList.isEmpty());
        assertEquals(
                "ThemeList created from the element without themes should be empty",
                emptyList.size(), 0);
    }

    @Test
    public void themeListCreatedWithOneThemes() {
        String themeName = "theme1";

        ThemeListImpl elementWithOneTheme = new ThemeListImpl(
                new MockElement(themeName));

        assertEquals(
                "ThemeList created from the element with one theme should contain single element",
                elementWithOneTheme.size(), 1);
        assertTrue(
                "ThemeList created from the element with one theme should contain this theme as its single element",
                elementWithOneTheme.contains(themeName));
    }

    @Test
    public void themeListCreatedWithMultipleThemes() {
        String[] themeNames = { "theme1", "theme2" };
        ThemeListImpl elementWithMultipleThemes = new ThemeListImpl(
                new MockElement(themeNames));

        assertFalse(
                "ThemeList created from the element with multiple distinct themes should not be empty",
                elementWithMultipleThemes.isEmpty());
        assertEquals(
                "ThemeList created from the element with multiple distinct themes should be of the same size as number of the themes",
                elementWithMultipleThemes.size(), themeNames.length);
        assertTrue(
                "ThemeList created from the element with multiple distinct themes should contain them all and oly them",
                elementWithMultipleThemes
                        .containsAll(Arrays.asList(themeNames)));
    }

    @Test
    public void themeListCreatedWithDuplicateThemes() {
        String[] themeNames = { "theme1", "theme1", "theme1" };
        ThemeListImpl elementWithMultipleThemes = new ThemeListImpl(
                new MockElement(themeNames));

        assertEquals(
                "ThemeList created from the element with multiple themes should be of the same size as number of unique themes",
                elementWithMultipleThemes.size(), 1);
        assertTrue(
                "ThemeList created from the element with multiple themes should be contain all unique themes",
                elementWithMultipleThemes.contains(themeNames[0]));
    }

    @Test
    public void clear() {
        MockElement element = new MockElement("theme1", "theme2");
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.clear();

        assertTrue("ThemeList should be empty after it's cleared",
                themeList.isEmpty());
        assertNull(
                "If corresponding ThemeList is cleared, no themes should be preset in the corresponding element",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME));
    }

    @Test
    public void remove() {
        String themeToRemove = "theme2";
        String themeToLeave = "theme1";
        MockElement element = new MockElement(themeToLeave, themeToRemove);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.remove(themeToRemove);

        assertEquals(
                "Only one theme should be present in ThemeList after removal",
                themeList.size(), 1);
        assertTrue("ThemeList should contain theme that was not removed",
                themeList.contains(themeToLeave));
        assertEquals(
                "Corresponding element should contain only the theme that was not removed",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeToLeave);
    }

    @Test
    public void removeAll() {
        String themeToRemove1 = "theme3";
        String themeToRemove2 = "theme2";
        String themeToLeave = "theme1";
        MockElement element = new MockElement(themeToLeave, themeToRemove1,
                themeToRemove2);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.removeAll(Arrays.asList(themeToRemove1, themeToRemove2));

        assertEquals(
                "Only one theme should be present in ThemeList after removal",
                themeList.size(), 1);
        assertTrue("ThemeList should contain theme that was not removed",
                themeList.contains(themeToLeave));
        assertEquals(
                "Corresponding element should contain only the theme that was not removed",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeToLeave);
    }

    @Test
    public void removeAllThemes() {
        String[] themeNames = { "theme1", "theme2" };
        MockElement element = new MockElement(themeNames);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.removeAll(Arrays.asList(themeNames));

        assertTrue(
                "ThemeList should be empty after all it's themes are removed",
                themeList.isEmpty());
        assertNull(
                "If corresponding ThemeList is cleared, no themes should be preset in the corresponding element",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME));
    }

    @Test
    public void retainAll() {
        List<String> elementsToRetain = Arrays.asList("retained",
                "notRetained");
        String[] themeNames = { elementsToRetain.get(0), "theme2", "theme3" };
        MockElement element = new MockElement(themeNames);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.retainAll(elementsToRetain);

        assertEquals("ThemeList should contain one retained theme",
                themeList.size(), 1);
        assertEquals(
                "Corresponding element should contain the only element present in ThemeList",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeList.iterator().next());
    }

    @Test
    public void add() {
        String themeToAdd = "theme";
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.add(themeToAdd);

        assertEquals("ThemeList should not be empty after adding a theme",
                themeList.size(), 1);
        assertTrue("ThemeList should contain theme added",
                themeList.contains(themeToAdd));
        assertEquals("Corresponding element should contain the theme added",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeToAdd);
    }

    @Test
    public void addAll() {
        List<String> themesToAdd = Arrays.asList("theme1", "theme2", "theme3");
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.addAll(themesToAdd);

        assertFalse("ThemeList should not be empty after adding themes",
                themeList.isEmpty());
        assertEquals(
                "ThemeList size should be equal to number of distinct themes added if it was empty before",
                themeList.size(), themesToAdd.size());
        assertTrue("ThemeList should contain all distinct themes added",
                themeList.containsAll(themesToAdd));
        themesToAdd.forEach(themeName -> assertTrue(
                "Each distinct theme added to ThemeList should be present in correspondent element's 'theme' attribute",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME)
                        .contains(themeName)));
    }

    @Test
    public void addDuplicates() {
        List<String> themesToAdd = Arrays.asList("theme1", "theme1", "theme1");
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.addAll(themesToAdd);

        assertEquals(
                "ThemeList should not be empty after themes have been added",
                themeList.size(), 1);
        assertTrue("ThemeList should contain all distinct themes added",
                themeList.contains(themesToAdd.get(0)));
        assertEquals(
                "Corresponding element should have all distinct themes added",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themesToAdd.get(0));
    }

    @Test
    public void iteratorRemoval() {
        String[] themeNames = { "theme1", "theme2", "theme3" };
        List<String> originalThemes = Arrays.asList(themeNames);
        MockElement element = new MockElement(themeNames);
        ThemeListImpl themeList = new ThemeListImpl(element);

        Set<String> removedElements = new HashSet<>();
        Iterator<String> iterator = themeList.iterator();
        removedElements.add(iterator.next());
        iterator.remove();
        removedElements.add(iterator.next());
        iterator.remove();

        assertEquals("ThemeList should have one element left after removal",
                themeList.size(), 1);
        String notRemovedTheme = themeList.iterator().next();
        assertTrue(
                "Themes returned by ThemeList's iterator.next() should be present in the original list of themes",
                originalThemes.containsAll(removedElements));
        assertTrue(
                "Theme left in ThemeList after removal should be present in the original list of themes",
                originalThemes.contains(notRemovedTheme));
        assertFalse(
                "Removed themes should not contain theme left in the ThemeList",
                removedElements.contains(notRemovedTheme));
        assertEquals(
                "Theme left in ThemeList after removal should be the only theme preset in the corresponding element",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                notRemovedTheme);
    }
}
