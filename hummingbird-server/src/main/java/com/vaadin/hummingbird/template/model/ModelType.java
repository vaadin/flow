/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.model;

import java.io.Serializable;
import java.lang.reflect.Type;

import com.vaadin.hummingbird.StateNode;

import elemental.json.JsonValue;

/**
 * A model type that can convert values between a representation suitable for
 * users and a representation suitable for storage in a {@link StateNode}.
 * <p>
 * Model type instances are shared between all instances of a given
 * {@link TemplateModel} type and should therefore be immutable to prevent race
 * conditions. The root type for a model can be found using
 * {@link ModelDescriptor#get(Class)}.
 *
 * @author Vaadin Ltd
 */
public interface ModelType extends Serializable {
    /**
     * Creates a representation of the provided model value that is intended for
     * use in application code. For mutable values, this is typically a proxy
     * that is directly connected to the underlying model value.
     *
     * @param modelValue
     *            the model value to convert
     * @return a user-friendly representation of the provided model value
     */
    Object modelToApplication(Serializable modelValue);

    /**
     * Creates a representation of the provided model value that is intended for
     * use by Nashorn when evaluating bindings.
     *
     * @param modelValue
     *            the model value to represent
     * @return a representation of the provided model value that is suitable for
     *         Nashorn
     */
    Object modelToNashorn(Serializable modelValue);

    /**
     * Creates a model value representation of the provided application value.
     * <p>
     * For application values that contain properties (i.e. beans), the provided
     * filter is used to determine which properties from the bean should be
     * included in the model representation.
     *
     * @param applicationValue
     *            the user value to convert
     * @param filter
     *            the filter to use to determine which properties to include,
     *            not <code>null</code>
     * @return a model value representation of the provided user value.
     */
    Serializable applicationToModel(Object applicationValue,
            PropertyFilter filter);

    /**
     * Checks whether this type can accept application values of the given type.
     * The method only considers this actual type, not the types of sub
     * properties or list items.
     *
     * @param applicationType
     *            the application type to check, not <code>null</code>
     * @return <code>true</code> if the provided type is acceptable,
     *         <code>false</code> otherwise
     */
    boolean accepts(Type applicationType);

    /**
     * Gets the Java {@link Type} that this model encapsulates.
     *
     * @return the java type
     */
    Type getJavaType();

    /**
     * Creates a JSON representation of this model type.
     *
     * @return a JSON representation of this model type, not <code>null</code>
     */
    public JsonValue toJson();
}
