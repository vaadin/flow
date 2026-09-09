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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.dom.BindingContext;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.dom.SignalBinding;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;

/**
 * Default implementation for the {@link ThemeList} that provides a live view
 * into the theme names of the corresponding element. Both reads and writes go
 * through the element attribute, {@link ThemeListImpl#THEME_ATTRIBUTE_NAME}, so
 * that all instances obtained for the same element always agree on the current
 * theme names.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ThemeListImpl implements ThemeList, Serializable {
    public static final String THEME_ATTRIBUTE_NAME = "theme";
    private static final String THEME_NAMES_DELIMITER = " ";

    /**
     * Iterator that reads the theme attribute on demand instead of iterating a
     * snapshot taken when the iterator was created. Each theme name currently
     * present in the attribute is returned exactly once, so theme names added
     * while the iteration is ongoing are also returned and theme names removed
     * meanwhile are skipped.
     */
    private final class ThemeListIterator implements Iterator<String> {
        private final Set<String> returned = new LinkedHashSet<>();
        private String next;
        private String current;

        @Override
        public boolean hasNext() {
            return findNext() != null;
        }

        @Override
        public String next() {
            String nextTheme = findNext();
            if (nextTheme == null) {
                throw new NoSuchElementException();
            }
            returned.add(nextTheme);
            current = nextTheme;
            next = null;
            return nextTheme;
        }

        @Override
        public void remove() {
            if (current == null) {
                throw new IllegalStateException(
                        "next() has not been called, or remove() has already been called after the last call to next()");
            }
            throwIfBound(current);
            Set<String> themes = readThemesFromAttribute();
            if (themes.remove(current)) {
                updateThemeAttribute(themes);
            }
            current = null;
        }

        private String findNext() {
            if (next == null) {
                for (String theme : readThemesFromAttribute()) {
                    if (!returned.contains(theme)) {
                        next = theme;
                        break;
                    }
                }
            }
            return next;
        }
    }

    private final Element element;

    /**
     * Creates new theme list for element specified.
     *
     * @param element
     *            the element to reflect theme changes onto
     */
    public ThemeListImpl(Element element) {
        this.element = element;
    }

    private Set<String> readThemesFromAttribute() {
        return Optional.ofNullable(element.getAttribute(THEME_ATTRIBUTE_NAME))
                .map(value -> value.split(THEME_NAMES_DELIMITER))
                .map(Stream::of)
                .map(stream -> stream.filter(themeName -> !themeName.isEmpty())
                        .collect(Collectors
                                .toCollection(LinkedHashSet<String>::new)))
                .orElseGet(LinkedHashSet::new);
    }

    @Override
    public SignalBinding<Boolean> bind(String name, Signal<Boolean> signal) {
        validate(name);
        Objects.requireNonNull(signal, "Signal cannot be null");
        SignalBindingFeature feature = element.getNode()
                .getFeature(SignalBindingFeature.class);

        if (feature.hasBinding(SignalBindingFeature.THEMES + name)) {
            throw new BindingActiveException(
                    "Theme name '" + name + "' is already bound to a signal");
        }

        SignalBinding<Boolean> binding = ElementEffect.bind(
                Element.get(element.getNode()), signal,
                (element, value) -> internalSetPresence(name,
                        Boolean.TRUE.equals(value)));
        feature.setBinding(SignalBindingFeature.THEMES + name, signal);
        return binding;
    }

    @Override
    public SignalBinding<List<String>> bind(Signal<List<String>> names) {
        Objects.requireNonNull(names, "Signal cannot be null");
        SignalBindingFeature feature = element.getNode()
                .getFeature(SignalBindingFeature.class);

        if (feature.hasBinding(SignalBindingFeature.THEME_GROUP)) {
            throw new BindingActiveException(
                    "A group theme name binding is already active");
        }

        SignalBinding<List<String>> binding = new SignalBinding<>();
        Set<String> previousNames = new HashSet<>();
        @SuppressWarnings("unchecked")
        List<String>[] previousValue = new List[] { names.peek() };
        Element ownerElement = Element.get(element.getNode());

        ElementEffect.effect(ownerElement, ctx -> {
            List<String> signalNames = names.get();
            Set<String> newNames = new HashSet<>();
            if (signalNames != null) {
                for (String name : signalNames) {
                    if (name != null && !name.isEmpty()) {
                        newNames.add(name);
                    }
                }
            }

            // Remove names no longer in the list
            for (String old : previousNames) {
                if (!newNames.contains(old)) {
                    internalSetPresence(old, false);
                }
            }
            // Add new names
            for (String name : newNames) {
                if (!previousNames.contains(name)) {
                    internalSetPresence(name, true);
                }
            }

            previousNames.clear();
            previousNames.addAll(newNames);

            if (ctx.isInitialRun() || binding.hasCallbacks()) {
                var bindingContext = new BindingContext<>(ctx.isInitialRun(),
                        ctx.isBackgroundChange(), previousValue[0], signalNames,
                        ownerElement);
                binding.setInitialContext(bindingContext);
                if (binding.hasCallbacks()) {
                    binding.fireOnChange(bindingContext);
                }
            }
            previousValue[0] = signalNames;
        });
        feature.setBinding(SignalBindingFeature.THEME_GROUP, names);
        return binding;
    }

    private void internalSetPresence(String name, boolean set) {
        Set<String> themes = readThemesFromAttribute();

        boolean changed;
        if (set) {
            changed = themes.add(name);
        } else {
            changed = themes.remove(name);
        }
        if (changed) {
            updateThemeAttribute(themes);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new ThemeListIterator();
    }

    @Override
    public boolean add(String themeName) {
        validate(themeName);
        throwIfBound(themeName);
        Set<String> themes = readThemesFromAttribute();
        boolean changed = themes.add(themeName);
        if (changed) {
            updateThemeAttribute(themes);
        }
        return changed;
    }

    @Override
    public boolean addAll(Collection<? extends String> themeNames) {
        themeNames.forEach(this::validate);
        themeNames.forEach(this::throwIfBound);
        Set<String> themes = readThemesFromAttribute();
        boolean changed = themes.addAll(themeNames);
        if (changed) {
            updateThemeAttribute(themes);
        }
        return changed;
    }

    @Override
    public boolean remove(Object themeName) {
        if (themeName instanceof String name) {
            throwIfBound(name);
        }
        Set<String> themes = readThemesFromAttribute();
        boolean changed = themes.remove(themeName);
        if (changed) {
            updateThemeAttribute(themes);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> themeNamesToRetain) {
        Set<String> themes = readThemesFromAttribute();
        themes.stream().filter(name -> !themeNamesToRetain.contains(name))
                .forEach(this::throwIfBound);
        boolean changed = themes.retainAll(themeNamesToRetain);
        if (changed) {
            updateThemeAttribute(themes);
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> themeNamesToRemove) {
        themeNamesToRemove.stream().map(String.class::cast)
                .forEach(this::throwIfBound);
        Set<String> themes = readThemesFromAttribute();
        boolean changed = themes.removeAll(themeNamesToRemove);
        if (changed) {
            updateThemeAttribute(themes);
        }
        return changed;
    }

    @Override
    public void clear() {
        getSignalBindingFeatureIfInitialized().ifPresent(feature -> {
            if (feature.hasAnyBinding(SignalBindingFeature.THEMES)) {
                throw new BindingActiveException();
            }
        });
        element.removeAttribute(THEME_ATTRIBUTE_NAME);
    }

    private void updateThemeAttribute(Set<String> themes) {
        if (themes.isEmpty()) {
            element.removeAttribute(THEME_ATTRIBUTE_NAME);
        } else {
            String value = String.join(THEME_NAMES_DELIMITER, themes);
            element.setAttribute(THEME_ATTRIBUTE_NAME, value);
        }
    }

    @Override
    public int size() {
        return readThemesFromAttribute().size();
    }

    @Override
    public boolean isEmpty() {
        return readThemesFromAttribute().isEmpty();
    }

    @Override
    public Object[] toArray() {
        return readThemesFromAttribute().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return readThemesFromAttribute().toArray(a);
    }

    @Override
    public boolean contains(Object themeName) {
        return readThemesFromAttribute().contains(themeName);
    }

    @Override
    public boolean containsAll(Collection<?> themeNames) {
        return readThemesFromAttribute().containsAll(themeNames);
    }

    @Override
    public String toString() {
        return readThemesFromAttribute().toString();
    }

    /**
     * Checks that the given theme name can be stored as a single entry of the
     * space separated {@code theme} attribute.
     *
     * @param themeName
     *            the theme name to validate
     */
    private void validate(String themeName) {
        if (themeName == null) {
            throw new IllegalArgumentException("Theme name cannot be null");
        }
        if (themeName.isEmpty()) {
            throw new IllegalArgumentException("Theme name cannot be empty");
        }
        if (themeName.indexOf(' ') != -1) {
            throw new IllegalArgumentException(
                    "Theme name cannot contain spaces. Use separate theme names or Element.setAttribute(\"theme\", ...) to set a space separated value");
        }
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
