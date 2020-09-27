/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.littemplate.LitTemplate;

/**
 * Marker interface for Polymer Template.
 * 
 * @author Vaadin Ltd
 * @since
 * 
 * @deprecated Polymer template support is deprecated - we recommend you to use
 *             {@link LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 *
 */
@Deprecated
public interface DeprecatedPolymerTemplate extends Serializable {

    /**
     * Check if the given Class {@code type} is found in the Model.
     *
     * @param type
     *            Class to check support for
     * @return True if supported by this DeprecatedPolymerTemplate
     */
    boolean isSupportedClass(Class<?> type);

    /**
     * Gets the type of the template model to use with with this template.
     *
     * @return the model type, not <code>null</code>
     */
    Object getModelType(Type type);
}
