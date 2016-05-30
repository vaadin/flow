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

import com.vaadin.ui.Template;

/**
 * Interface for a {@link Template}'s model. Extending this interface and adding
 * getters and setters makes it possible to easily bind data to a template.
 * <p>
 * It is also possible to import a Bean's properties to the model using
 * {@link #importBean(Object)}.
 * <p>
 * Supported property types:
 * <ul>
 * <li>boolean &amp; Boolean</li>
 * <li>int &amp; Integer</li>
 * <li>double &amp; Double</li>
 * <li>String</li>
 * </ul>
 *
 * @author Vaadin Ltd
 */
public interface TemplateModel extends Serializable {

    /**
     * Import a Bean to this template model.
     * <p>
     * The given Bean is searched for getter methods and the values that the
     * getters return are set to model values with the corresponding key.
     * <p>
     * E.g. the <code>firstName</code> property in the bean (has a
     * <code>getFirstName</code> getter method) will be imported to template
     * model with the <code>firstName</code> key.
     * <p>
     * NOTE: nested beans are not supported.
     *
     * @param bean
     *            the bean to import
     * @see TemplateModel for supported property types
     */
    default void importBean(Object bean) {
        // NOOP since invocation handler takes care of this
    }
}
