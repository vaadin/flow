/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.template.internal;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.vaadin.flow.component.Component;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Service for injecting the polymer event handler when the module is available.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 * @deprecated Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public interface DeprecatedPolymerPublishedEventHandler extends Serializable {

    /**
     * Validate that the given Component instance is a PolymerTemplate and that
     * the value can be converted.
     *
     * @param instance
     *            Component to be validated
     * @param argValue
     *            received value
     * @param convertedType
     *            target type that value should be converted to
     * @return true if valid template model value
     */
    boolean isTemplateModelValue(Component instance, JsonValue argValue,
            Class<?> convertedType);

    /**
     * Get the template model object and type.
     *
     * @param template
     *            polymer template to get model from
     * @param argValue
     *            argument value
     * @param convertedType
     *            value type
     * @return the provided model value
     * @throws IllegalStateException
     *             if the component is not attached to the UI
     */
    Object getTemplateItem(Component template, JsonObject argValue,
            Type convertedType);
}
