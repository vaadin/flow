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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
class ThemeListImplTest {

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
    void themeListCreatedWithNoThemes() {
        ThemeListImpl emptyList = new ThemeListImpl(new MockElement());

        assertTrue(emptyList.isEmpty(),
                "ThemeList created from the element without themes should be empty");
        assertEquals(emptyList.size(), 0,
                "ThemeList created from the element without themes should be empty");
    }

    @Test
    void themeListCreatedWithOneThemes() {
        String themeName = "theme1";

        ThemeListImpl elementWithOneTheme = new ThemeListImpl(
                new MockElement(themeName));

        assertEquals(elementWithOneTheme.size(), 1,
                "ThemeList created from the element with one theme should contain single element");
        assertTrue(elementWithOneTheme.contains(themeName),
                "ThemeList created from the element with one theme should contain this theme as its single element");
    }

    @Test
    void themeListCreatedWithMultipleThemes() {
        String[] themeNames = { "theme1", "theme2" };
        ThemeListImpl elementWithMultipleThemes = new ThemeListImpl(
                new MockElement(themeNames));

        assertFalse(elementWithMultipleThemes.isEmpty(),
                "ThemeList created from the element with multiple distinct themes should not be empty");
        assertEquals(elementWithMultipleThemes.size(), themeNames.length,
                "ThemeList created from the element with multiple distinct themes should be of the same size as number of the themes");
        assertTrue(
                elementWithMultipleThemes
                        .containsAll(Arrays.asList(themeNames)),
                "ThemeList created from the element with multiple distinct themes should contain them all and oly them");
    }

    @Test
    void themeListCreatedWithDuplicateThemes() {
        String[] themeNames = { "theme1", "theme1", "theme1" };
        ThemeListImpl elementWithMultipleThemes = new ThemeListImpl(
                new MockElement(themeNames));

        assertEquals(elementWithMultipleThemes.size(), 1,
                "ThemeList created from the element with multiple themes should be of the same size as number of unique themes");
        assertTrue(elementWithMultipleThemes.contains(themeNames[0]),
                "ThemeList created from the element with multiple themes should be contain all unique themes");
    }

    @Test
    void clear() {
        MockElement element = new MockElement("theme1", "theme2");
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.clear();

        assertTrue(themeList.isEmpty(),
                "ThemeList should be empty after it's cleared");
        assertNull(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                "If corresponding ThemeList is cleared, no themes should be preset in the corresponding element");
    }

    @Test
    void remove() {
        String themeToRemove = "theme2";
        String themeToLeave = "theme1";
        MockElement element = new MockElement(themeToLeave, themeToRemove);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.remove(themeToRemove);

        assertEquals(themeList.size(), 1,
                "Only one theme should be present in ThemeList after removal");
        assertTrue(themeList.contains(themeToLeave),
                "ThemeList should contain theme that was not removed");
        assertEquals(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeToLeave,
                "Corresponding element should contain only the theme that was not removed");
    }

    @Test
    void removeAll() {
        String themeToRemove1 = "theme3";
        String themeToRemove2 = "theme2";
        String themeToLeave = "theme1";
        MockElement element = new MockElement(themeToLeave, themeToRemove1,
                themeToRemove2);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.removeAll(Arrays.asList(themeToRemove1, themeToRemove2));

        assertEquals(themeList.size(), 1,
                "Only one theme should be present in ThemeList after removal");
        assertTrue(themeList.contains(themeToLeave),
                "ThemeList should contain theme that was not removed");
        assertEquals(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeToLeave,
                "Corresponding element should contain only the theme that was not removed");
    }

    @Test
    void removeAllThemes() {
        String[] themeNames = { "theme1", "theme2" };
        MockElement element = new MockElement(themeNames);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.removeAll(Arrays.asList(themeNames));

        assertTrue(themeList.isEmpty(),
                "ThemeList should be empty after all it's themes are removed");
        assertNull(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                "If corresponding ThemeList is cleared, no themes should be preset in the corresponding element");
    }

    @Test
    void retainAll() {
        List<String> elementsToRetain = Arrays.asList("retained",
                "notRetained");
        String[] themeNames = { elementsToRetain.get(0), "theme2", "theme3" };
        MockElement element = new MockElement(themeNames);
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.retainAll(elementsToRetain);

        assertEquals(themeList.size(), 1,
                "ThemeList should contain one retained theme");
        assertEquals(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeList.iterator().next(),
                "Corresponding element should contain the only element present in ThemeList");
    }

    @Test
    void add() {
        String themeToAdd = "theme";
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.add(themeToAdd);

        assertEquals(themeList.size(), 1,
                "ThemeList should not be empty after adding a theme");
        assertTrue(themeList.contains(themeToAdd),
                "ThemeList should contain theme added");
        assertEquals(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themeToAdd,
                "Corresponding element should contain the theme added");
    }

    @Test
    void addAll() {
        List<String> themesToAdd = Arrays.asList("theme1", "theme2", "theme3");
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.addAll(themesToAdd);

        assertFalse(themeList.isEmpty(),
                "ThemeList should not be empty after adding themes");
        assertEquals(themeList.size(), themesToAdd.size(),
                "ThemeList size should be equal to number of distinct themes added if it was empty before");
        assertTrue(themeList.containsAll(themesToAdd),
                "ThemeList should contain all distinct themes added");
        themesToAdd.forEach(themeName -> assertTrue(
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME)
                        .contains(themeName),
                "Each distinct theme added to ThemeList should be present in correspondent element's 'theme' attribute"));
    }

    @Test
    void addDuplicates() {
        List<String> themesToAdd = Arrays.asList("theme1", "theme1", "theme1");
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.addAll(themesToAdd);

        assertEquals(themeList.size(), 1,
                "ThemeList should not be empty after themes have been added");
        assertTrue(themeList.contains(themesToAdd.get(0)),
                "ThemeList should contain all distinct themes added");
        assertEquals(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                themesToAdd.get(0),
                "Corresponding element should have all distinct themes added");
    }

    @Test
    void iteratorRemoval() {
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

        assertEquals(themeList.size(), 1,
                "ThemeList should have one element left after removal");
        String notRemovedTheme = themeList.iterator().next();
        assertTrue(originalThemes.containsAll(removedElements),
                "Themes returned by ThemeList's iterator.next() should be present in the original list of themes");
        assertTrue(originalThemes.contains(notRemovedTheme),
                "Theme left in ThemeList after removal should be present in the original list of themes");
        assertFalse(removedElements.contains(notRemovedTheme),
                "Removed themes should not contain theme left in the ThemeList");
        assertEquals(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                notRemovedTheme,
                "Theme left in ThemeList after removal should be the only theme preset in the corresponding element");
    }

    @Test
    void themeListReflectsAttributeSetAfterCreation() {
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        element.setAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME, "dark");

        assertFalse(themeList.isEmpty(),
                "ThemeList should reflect the theme attribute set after the list was obtained");
        assertEquals(1, themeList.size(),
                "ThemeList should reflect the theme attribute set after the list was obtained");
        assertTrue(themeList.contains("dark"),
                "ThemeList should reflect the theme attribute set after the list was obtained");
        assertEquals("dark", themeList.iterator().next(),
                "ThemeList iterator should reflect the theme attribute set after the list was obtained");
    }

    @Test
    void themeListReflectsAttributeRemovedAfterCreation() {
        MockElement element = new MockElement("dark");
        ThemeListImpl themeList = new ThemeListImpl(element);

        element.removeAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME);

        assertTrue(themeList.isEmpty(),
                "ThemeList should reflect the theme attribute removed after the list was obtained");
        assertFalse(themeList.contains("dark"),
                "ThemeList should reflect the theme attribute removed after the list was obtained");
    }

    @Test
    void modificationKeepsThemesAddedThroughAnotherInstance() {
        MockElement element = new MockElement();
        ThemeListImpl firstList = new ThemeListImpl(element);
        ThemeListImpl secondList = new ThemeListImpl(element);

        secondList.add("dark");
        firstList.add("compact");

        assertEquals(Set.of("dark", "compact"),
                Set.of(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME)
                        .split(" ")),
                "Modifying one ThemeList instance should not discard themes added through another instance");
        assertTrue(firstList.containsAll(Arrays.asList("dark", "compact")),
                "Every ThemeList instance of the same element should see the same themes");
        assertTrue(secondList.containsAll(Arrays.asList("dark", "compact")),
                "Every ThemeList instance of the same element should see the same themes");
    }

    @Test
    void removalKeepsThemesAddedThroughAnotherInstance() {
        MockElement element = new MockElement("theme1", "theme2");
        ThemeListImpl firstList = new ThemeListImpl(element);
        ThemeListImpl secondList = new ThemeListImpl(element);

        secondList.add("dark");
        firstList.remove("theme1");

        assertEquals(Set.of("dark", "theme2"),
                Set.of(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME)
                        .split(" ")),
                "Removing through one ThemeList instance should only remove the requested theme");
    }

    @Test
    void iteratorRemovalKeepsThemesAddedThroughAnotherInstance() {
        MockElement element = new MockElement("theme1", "theme2");
        ThemeListImpl themeList = new ThemeListImpl(element);
        ThemeListImpl otherList = new ThemeListImpl(element);

        Iterator<String> iterator = themeList.iterator();
        String removedTheme = iterator.next();
        otherList.add("dark");
        iterator.remove();

        Set<String> expected = new HashSet<>(
                Arrays.asList("theme1", "theme2", "dark"));
        expected.remove(removedTheme);
        assertEquals(expected,
                Set.of(element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME)
                        .split(" ")),
                "Iterator removal should only remove the theme it returned, keeping themes added through another instance");
        assertFalse(themeList.contains(removedTheme),
                "Iterator removal should remove the theme it returned");
    }

    @Test
    void themesKeepInsertionOrderInAttribute() {
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        themeList.add("theme1");
        themeList.add("theme2");
        themeList.add("theme3");

        assertEquals("theme1 theme2 theme3",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                "Themes should be written to the theme attribute in the order they were added");
    }

    @Test
    void iteratorSeesThemesAddedDuringIteration() {
        MockElement element = new MockElement("theme1");
        ThemeListImpl themeList = new ThemeListImpl(element);

        Iterator<String> iterator = themeList.iterator();
        assertEquals("theme1", iterator.next());
        new ThemeListImpl(element).add("dark");

        assertTrue(iterator.hasNext(),
                "Iterator should see a theme added through another instance while iterating");
        assertEquals("dark", iterator.next(),
                "Iterator should return the theme added while iterating");
        assertFalse(iterator.hasNext(),
                "Iterator should return every theme exactly once");
    }

    @Test
    void iteratorSkipsThemesRemovedDuringIteration() {
        MockElement element = new MockElement("theme1", "theme2");
        ThemeListImpl themeList = new ThemeListImpl(element);

        Iterator<String> iterator = themeList.iterator();
        assertEquals("theme1", iterator.next());
        new ThemeListImpl(element).remove("theme2");

        assertFalse(iterator.hasNext(),
                "Iterator should skip a theme removed through another instance while iterating");
    }

    @Test
    void iteratorRemoveWithoutNext_throwsIllegalStateException() {
        MockElement element = new MockElement("theme1");
        ThemeListImpl themeList = new ThemeListImpl(element);

        Iterator<String> iterator = themeList.iterator();
        assertThrows(IllegalStateException.class, iterator::remove,
                "Iterator removal without a preceding next() should throw");

        iterator.next();
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove,
                "Repeated iterator removal for the same theme should throw");
    }

    @Test
    void spaceSeparatedValueIsTreatedAsSeveralThemeNames() {
        MockElement element = new MockElement();
        ThemeListImpl themeList = new ThemeListImpl(element);

        assertTrue(themeList.add("primary small"),
                "Adding a space separated value should modify the theme list");

        assertEquals("primary small",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                "A space separated value should be written to the theme attribute as is");
        assertEquals(2, themeList.size(),
                "A space separated value should count as several theme names");
        assertTrue(themeList.contains("primary small"),
                "A space separated value should be found after it was added");
        assertTrue(themeList.contains("small"),
                "Each theme name of a space separated value should be found on its own");
        assertFalse(themeList.add("small"),
                "Adding a theme name that is already present should not modify the theme list");

        assertTrue(themeList.remove("primary small"),
                "Removing a space separated value should modify the theme list");
        assertTrue(themeList.isEmpty(),
                "Removing a space separated value should remove each of its theme names");
    }

    @Test
    void addNullThemeName_throwsNullPointerException() {
        ThemeListImpl themeList = new ThemeListImpl(new MockElement());

        assertThrows(NullPointerException.class, () -> themeList.add(null),
                "A null theme name should be rejected");
    }

    @Test
    void blankAndUnknownValuesAreNotContained() {
        MockElement element = new MockElement("theme1");
        ThemeListImpl themeList = new ThemeListImpl(element);

        assertFalse(themeList.contains(""),
                "An empty value is not a theme name and should not be found");
        assertFalse(themeList.contains(null),
                "A null value should not be found");
        assertFalse(themeList.contains(Integer.valueOf(1)),
                "A non-string value should not be found");
        assertFalse(themeList.remove(Integer.valueOf(1)),
                "Removing a non-string value should not modify the theme list");
        assertTrue(themeList.containsAll(List.of()),
                "Every theme list contains the empty collection");
        assertEquals("theme1",
                element.getAttribute(ThemeListImpl.THEME_ATTRIBUTE_NAME),
                "None of the above should have modified the theme attribute");
    }
}
