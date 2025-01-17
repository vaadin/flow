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
