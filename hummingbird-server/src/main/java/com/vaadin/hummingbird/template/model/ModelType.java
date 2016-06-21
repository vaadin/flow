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

import com.vaadin.hummingbird.StateNode;

/**
 * A model type that can convert values between a representation suitable for
 * users and a representation suitable for storage in a {@link StateNode}.
 *
 * @author Vaadin Ltd
 */
public abstract class ModelType {
    /**
     * Creates a user-friendly representation of the provided model value.
     *
     * @param modelValue
     *            the model value to convert
     * @return a user-friendly representation of the provided model value
     */
    public abstract Object modelToUser(Serializable modelValue);

    /**
     * Creates a model value representation of the provided user value.
     * <p>
     * For user values that contain properties (i.e. beans), the provided filter
     * is used to determine which properties from the bean should be included in
     * the model representation.
     *
     * @param userValue
     *            the user value to convert
     * @param filter
     *            the filter to use to determine which properties to include
     * @return a model value representation of the provided user value.
     */
    public abstract Serializable userToModel(Object userValue,
            PropertyFilter filter);
}
