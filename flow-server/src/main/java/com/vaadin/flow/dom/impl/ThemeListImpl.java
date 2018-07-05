/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;

/**
 * Default implementation for the {@link ThemeList} that stores the theme names
 * of the corresponding element. Makes sure that each change to the collection
 * is reflected in the corresponding element attribute name,
 * {@link ThemeListImpl#THEME_ATTRIBUTE_NAME}.
 * 
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ThemeListImpl implements ThemeList, Serializable {
    public static final String THEME_ATTRIBUTE_NAME = "theme";
    private static final String THEME_NAMES_DELIMITER = " ";

    private final class ThemeListIterator implements Iterator<String> {
        private final Iterator<String> wrappedIterator = themes.iterator();

        @Override
        public boolean hasNext() {
            return wrappedIterator.hasNext();
        }

        @Override
        public String next() {
            return wrappedIterator.next();
        }

        @Override
        public void remove() {
            wrappedIterator.remove();
            updateThemeAttribute();
        }
    }

    private final Element element;
    private final Set<String> themes;

    /**
     * Creates new theme list for element specified.
     * 
     * @param element
     *            the element to reflect theme changes onto
     */
    public ThemeListImpl(Element element) {
        this.element = element;
        themes = Optional.ofNullable(element.getAttribute(THEME_ATTRIBUTE_NAME))
                .map(value -> value.split(THEME_NAMES_DELIMITER))
                .map(Stream::of)
                .map(stream -> stream.filter(themeName -> !themeName.isEmpty())
                        .collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }

    @Override
    public Iterator<String> iterator() {
        return new ThemeListIterator();
    }

    @Override
    public boolean add(String themeName) {
        boolean changed = themes.add(themeName);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean addAll(Collection<? extends String> themeNames) {
        boolean changed = themes.addAll(themeNames);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean remove(Object themeName) {
        boolean changed = themes.remove(themeName);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> themeNamesToRetain) {
        boolean changed = themes.retainAll(themeNamesToRetain);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> themeNamesToRemove) {
        boolean changed = themes.removeAll(themeNamesToRemove);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public void clear() {
        themes.clear();
        updateThemeAttribute();
    }

    private void updateThemeAttribute() {
        if (themes.isEmpty()) {
            element.removeAttribute(THEME_ATTRIBUTE_NAME);
        } else {
            element.setAttribute(THEME_ATTRIBUTE_NAME, themes.stream()
                    .collect(Collectors.joining(THEME_NAMES_DELIMITER)));
        }
    }

    @Override
    public int size() {
        return themes.size();
    }

    @Override
    public boolean isEmpty() {
        return themes.isEmpty();
    }

    @Override
    public Object[] toArray() {
        return themes.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return themes.toArray(a);
    }

    @Override
    public boolean contains(Object themeName) {
        return themes.contains(themeName);
    }

    @Override
    public boolean containsAll(Collection<?> themeNames) {
        return themes.containsAll(themeNames);
    }

    @Override
    public String toString() {
        return themes.toString();
    }
}
