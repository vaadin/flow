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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;

/**
 * Default implementation for the {@link ThemeList} that stores the theme names
 * of the corresponding element. Makes sure that each change to the collection
 * is reflected in the corresponding element attribute name,
 * {@link ThemeListImpl#THEME_ATTRIBUTE_NAME}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ThemeListImpl implements ThemeList, Serializable {
    public static final String THEME_ATTRIBUTE_NAME = "theme";
    private static final String THEME_NAMES_DELIMITER = " ";

    private final class ThemeListIterator implements Iterator<String> {
        private final Iterator<String> wrappedIterator = allNames().iterator();
        private String current;

        @Override
        public boolean hasNext() {
            return wrappedIterator.hasNext();
        }

        @Override
        public String next() {
            current = wrappedIterator.next();
            return current;
        }

        @Override
        public void remove() {
            wrappedIterator.remove();
            themes.remove(current);
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
        themes = readThemesFromAttribute();
    }

    private Set<String> readThemesFromAttribute() {
        return Optional.ofNullable(element.getAttribute(THEME_ATTRIBUTE_NAME))
                .map(value -> value.split(THEME_NAMES_DELIMITER))
                .map(Stream::of)
                .map(stream -> stream.filter(themeName -> !themeName.isEmpty())
                        .collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }

    private List<String> readThemesFromAttributeAsList() {
        String attr = element.getAttribute(THEME_ATTRIBUTE_NAME);
        if (attr == null || attr.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String name : attr.split(THEME_NAMES_DELIMITER)) {
            if (!name.isEmpty()) {
                result.add(name);
            }
        }
        return result;
    }

    private List<String> getGroupBoundNames() {
        return getSignalBindingFeatureIfInitialized()
                .map(SignalBindingFeature::getThemeGroupBoundNames)
                .orElse(Collections.emptyList());
    }

    @Override
    public void bind(String name, Signal<Boolean> signal) {
        Objects.requireNonNull(signal, "Signal cannot be null");
        SignalBindingFeature feature = element.getNode()
                .getFeature(SignalBindingFeature.class);

        if (feature.hasBinding(SignalBindingFeature.THEMES + name)) {
            throw new BindingActiveException(
                    "Theme name '" + name + "' is already bound to a signal");
        }

        Registration registration = ElementEffect.bind(
                Element.get(element.getNode()), signal,
                (element, value) -> internalSetPresence(name,
                        Boolean.TRUE.equals(value)));
        feature.setBinding(SignalBindingFeature.THEMES + name, registration,
                signal);
    }

    @Override
    public void bind(Signal<List<String>> names) {
        Objects.requireNonNull(names, "Signal cannot be null");
        SignalBindingFeature feature = element.getNode()
                .getFeature(SignalBindingFeature.class);

        if (feature.hasBinding(SignalBindingFeature.THEME_GROUP)) {
            throw new BindingActiveException(
                    "A group theme name binding is already active");
        }

        ArrayList<String> groupBoundNames = new ArrayList<>();

        // Store binding data before creating the effect so that
        // getGroupBoundNames() can find it during the first execution
        feature.setBinding(SignalBindingFeature.THEME_GROUP, () -> {
        }, names, null, groupBoundNames);

        Registration registration = ElementEffect
                .effect(Element.get(element.getNode()), () -> {
                    List<String> current = names.get();

                    // Save old group names before replacing
                    List<String> oldGroupNames = new ArrayList<>(
                            groupBoundNames);

                    // Compute new group names
                    groupBoundNames.clear();
                    if (current != null) {
                        for (String name : current) {
                            if (name != null && !name.isEmpty()) {
                                groupBoundNames.add(name);
                            }
                        }
                    }

                    // Re-read all names from the attribute as a list
                    // (preserving duplicates), then remove one occurrence of
                    // each old group name to recover static/toggle themes
                    List<String> attrNames = readThemesFromAttributeAsList();
                    for (String old : oldGroupNames) {
                        attrNames.remove(old);
                    }
                    themes.clear();
                    themes.addAll(attrNames);
                    updateThemeAttribute();
                });
        // Replace with real registration
        feature.setBinding(SignalBindingFeature.THEME_GROUP, registration,
                names, null, groupBoundNames);
    }

    private void internalSetPresence(String name, boolean set) {
        // Re-read themes from the attribute to stay in sync with other
        // ThemeListImpl instances that may have modified the attribute.
        themes.clear();
        Set<String> fromAttribute = readThemesFromAttribute();
        // Exclude group-bound names to avoid pulling them into the themes set
        List<String> groupNames = getGroupBoundNames();
        fromAttribute.removeAll(groupNames);
        themes.addAll(fromAttribute);

        boolean changed;
        if (set) {
            changed = themes.add(name);
        } else {
            changed = themes.remove(name);
        }
        if (changed) {
            updateThemeAttribute();
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new ThemeListIterator();
    }

    @Override
    public boolean add(String themeName) {
        throwIfBound(themeName);
        boolean changed = themes.add(themeName);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean addAll(Collection<? extends String> themeNames) {
        themeNames.forEach(this::throwIfBound);
        boolean changed = themes.addAll(themeNames);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean remove(Object themeName) {
        if (themeName instanceof String name) {
            throwIfBound(name);
        }
        boolean changed = themes.remove(themeName);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> themeNamesToRetain) {
        themes.stream().filter(name -> !themeNamesToRetain.contains(name))
                .forEach(this::throwIfBound);
        boolean changed = themes.retainAll(themeNamesToRetain);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> themeNamesToRemove) {
        themeNamesToRemove.stream().map(String.class::cast)
                .forEach(this::throwIfBound);
        boolean changed = themes.removeAll(themeNamesToRemove);
        if (changed) {
            updateThemeAttribute();
        }
        return changed;
    }

    @Override
    public void clear() {
        clearBindings();
        themes.clear();
        updateThemeAttribute();
    }

    private void updateThemeAttribute() {
        List<String> groupNames = getGroupBoundNames();
        if (themes.isEmpty() && groupNames.isEmpty()) {
            element.removeAttribute(THEME_ATTRIBUTE_NAME);
        } else {
            String value = Stream.concat(themes.stream(), groupNames.stream())
                    .collect(Collectors.joining(THEME_NAMES_DELIMITER));
            element.setAttribute(THEME_ATTRIBUTE_NAME, value);
        }
    }

    private Set<String> allNames() {
        Set<String> all = new HashSet<>(themes);
        all.addAll(getGroupBoundNames());
        return all;
    }

    @Override
    public int size() {
        return allNames().size();
    }

    @Override
    public boolean isEmpty() {
        return themes.isEmpty() && getGroupBoundNames().isEmpty();
    }

    @Override
    public Object[] toArray() {
        return allNames().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return allNames().toArray(a);
    }

    @Override
    public boolean contains(Object themeName) {
        return themes.contains(themeName)
                || getGroupBoundNames().contains(themeName);
    }

    @Override
    public boolean containsAll(Collection<?> themeNames) {
        return allNames().containsAll(themeNames);
    }

    @Override
    public String toString() {
        return allNames().toString();
    }

    /**
     * Clears all signal bindings.
     */
    public void clearBindings() {
        getSignalBindingFeatureIfInitialized().ifPresent(
                feature -> feature.clearBindings(SignalBindingFeature.THEMES));
    }

    private void throwIfBound(String className) {
        getSignalBindingFeatureIfInitialized().ifPresent(feature -> {
            if (feature.hasBinding(SignalBindingFeature.THEMES + className)) {
                throw new BindingActiveException("Theme name '" + className
                        + "' is bound and cannot be modified manually");
            }
        });
    }

    private Optional<SignalBindingFeature> getSignalBindingFeatureIfInitialized() {
        try {
            return element.getNode()
                    .getFeatureIfInitialized(SignalBindingFeature.class);
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }
}
