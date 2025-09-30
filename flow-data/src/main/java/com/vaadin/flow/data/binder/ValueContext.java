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
package com.vaadin.flow.data.binder;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;

/**
 * Value context for {@code Converter}s. Contains relevant information for
 * converting values.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ValueContext implements Serializable {

    private final Binder<?> binder;
    private final Component component;
    private final HasValue<?, ?> hasValue;
    private final Locale locale;

    /**
     * Constructor for {@code ValueContext} without a {@code Locale}.
     */
    public ValueContext() {
        this.binder = null;
        component = null;
        hasValue = null;
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext} without a {@code Locale}.
     *
     * @param binder
     *            the Binder using the value context
     */
    public ValueContext(Binder<?> binder) {
        this.binder = binder;
        component = null;
        hasValue = null;
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext} without a {@code Component}.
     *
     * @param locale
     *            The locale used with conversion. Can be null.
     */
    public ValueContext(Locale locale) {
        this.binder = null;
        component = null;
        this.locale = locale;
        hasValue = null;
    }

    /**
     * Constructor for {@code ValueContext} without a {@code Component}.
     *
     * @param binder
     *            the Binder using the value context
     * @param locale
     *            The locale used with conversion. Can be null.
     */
    public ValueContext(Binder binder, Locale locale) {
        this.binder = binder;
        component = null;
        this.locale = locale;
        hasValue = null;
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param component
     *            The component related to current value. Can be null. If the
     *            component implements {@link HasValue}, it will be returned by
     *            {@link #getHasValue()} as well.
     */
    public ValueContext(Component component) {
        this.binder = null;
        this.component = component;
        if (component instanceof HasValue) {
            hasValue = (HasValue<?, ?>) component;
        } else {
            hasValue = null;
        }
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param binder
     *            the Binder using the value context
     * @param component
     *            The component related to current value. Can be null. If the
     *            component implements {@link HasValue}, it will be returned by
     *            {@link #getHasValue()} as well.
     */
    public ValueContext(Binder binder, Component component) {
        this.binder = binder;
        this.component = component;
        if (component instanceof HasValue) {
            hasValue = (HasValue<?, ?>) component;
        } else {
            hasValue = null;
        }
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param component
     *            The component related to current value. Can be null.
     * @param hasValue
     *            The value source related to current value. Can be null.
     */
    public ValueContext(Component component, HasValue<?, ?> hasValue) {
        this.binder = null;
        this.component = component;
        this.hasValue = hasValue;
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param binder
     *            the Binder using the value context
     * @param component
     *            The component related to current value. Can be null.
     * @param hasValue
     *            The value source related to current value. Can be null.
     */
    public ValueContext(Binder binder, Component component,
            HasValue<?, ?> hasValue) {
        this.binder = binder;
        this.component = component;
        this.hasValue = hasValue;
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param component
     *            The component can be {@code null}.
     * @param locale
     *            The locale used with conversion. Can be {@code null}.
     * @param hasValue
     *            The value source related to current value. Can be
     *            {@code null}.
     */
    public ValueContext(Component component, HasValue<?, ?> hasValue,
            Locale locale) {
        this.binder = null;
        this.component = component;
        this.hasValue = hasValue;
        this.locale = locale;
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param binder
     *            the Binder using the value context
     * @param component
     *            The component can be {@code null}.
     * @param locale
     *            The locale used with conversion. Can be {@code null}.
     * @param hasValue
     *            The value source related to current value. Can be
     *            {@code null}.
     */
    public ValueContext(Binder binder, Component component,
            HasValue<?, ?> hasValue, Locale locale) {
        this.binder = binder;
        this.component = component;
        this.hasValue = hasValue;
        this.locale = locale;
    }

    /**
     * Constructor for {@code ValueContext} without a {@code Locale}.
     *
     * @deprecated Use the version with binder reference instead
     */
    @Deprecated
    public ValueContext() {
        this.binder = null;
        component = null;
        hasValue = null;
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext} without a {@code Component}.
     *
     * @param locale
     *            The locale used with conversion. Can be null.
     * @deprecated Use the version with binder reference instead
     */
    @Deprecated
    public ValueContext(Locale locale) {
        this.binder = null;
        component = null;
        this.locale = locale;
        hasValue = null;
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param component
     *            The component related to current value. Can be null. If the
     *            component implements {@link HasValue}, it will be returned by
     *            {@link #getHasValue()} as well.
     * @deprecated Use the version with binder reference instead
     */
    @Deprecated
    public ValueContext(Component component) {
        this.binder = null;
        this.component = component;
        if (component instanceof HasValue) {
            hasValue = (HasValue<?, ?>) component;
        } else {
            hasValue = null;
        }
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param component
     *            The component related to current value. Can be null.
     * @param hasValue
     *            The value source related to current value. Can be null.
     * @deprecated Use the version with binder reference instead
     */
    @Deprecated
    public ValueContext(Component component, HasValue<?, ?> hasValue) {
        this.binder = null;
        this.component = component;
        this.hasValue = hasValue;
        locale = findLocale(component);
    }

    /**
     * Constructor for {@code ValueContext}.
     *
     * @param component
     *            The component can be {@code null}.
     * @param locale
     *            The locale used with conversion. Can be {@code null}.
     * @param hasValue
     *            The value source related to current value. Can be
     *            {@code null}.
     * @deprecated Use the version with binder reference instead
     */
    @Deprecated
    public ValueContext(Component component, HasValue<?, ?> hasValue,
            Locale locale) {
        this.binder = null;
        this.component = component;
        this.hasValue = hasValue;
        this.locale = locale;
    }

    private Locale findLocale(Component component) {
        UI ui = null;
        if (component != null) {
            ui = component.getUI().orElseGet(UI::getCurrent);
        } else {
            ui = UI.getCurrent();
        }
        if (ui != null) {
            return ui.getLocale();
        } else {
            return Locale.getDefault();
        }
    }

    /**
     * Returns an {@code Optional} for the {@code Component} related to value
     * conversion.
     *
     * @return the optional of component
     */
    public Optional<Component> getComponent() {
        return Optional.ofNullable(component);
    }

    /**
     * Returns an {@code Optional} for the {@code Locale} used in the value
     * conversion.
     *
     * @return the optional of locale
     */
    public Optional<Locale> getLocale() {
        return Optional.ofNullable(locale);
    }

    /**
     * Returns an {@code Optional} for the {@code HasValue} used in the value
     * conversion. In certain complicated cases, ex. cross-field validation,
     * HasValue might be not available.
     *
     * @return the optional of {@code HasValue}
     */
    @SuppressWarnings("unused")
    public Optional<HasValue<?, ?>> getHasValue() {
        return Optional.ofNullable(hasValue);
    }

    /**
     * Returns an {@code Optional} for the {@code Binder} owning this value
     * context.
     *
     * @return the optional of {@code Binder}
     */
    public Optional<Binder<?>> getBinder() {
        return Optional.ofNullable(binder);
    }
}
