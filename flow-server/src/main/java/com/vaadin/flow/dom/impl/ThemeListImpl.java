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
import java.util.List;
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
        private final Iterator<String> wrappedIterator = themes.iterator();
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

    @Override
    public SignalBinding<Boolean> bind(String name, Signal<Boolean> signal) {
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
        // Re-read themes from the attribute to stay in sync with other
        // ThemeListImpl instances that may have modified the attribute.
        themes.clear();
        themes.addAll(readThemesFromAttribute());

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
        getSignalBindingFeatureIfInitialized().ifPresent(feature -> {
            if (feature.hasAnyBinding(SignalBindingFeature.THEMES)) {
                throw new BindingActiveException();
            }
        });
        themes.clear();
        updateThemeAttribute();
    }

    private void updateThemeAttribute() {
        if (themes.isEmpty()) {
            element.removeAttribute(THEME_ATTRIBUTE_NAME);
        } else {
            String value = String.join(THEME_NAMES_DELIMITER, themes);
            element.setAttribute(THEME_ATTRIBUTE_NAME, value);
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
