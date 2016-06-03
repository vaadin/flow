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
import java.util.function.Predicate;

import com.vaadin.ui.Template;

/**
 * Interface for a {@link Template}'s model. Extending this interface and adding
 * getters and setters makes it possible to easily bind data to a template.
 * <p>
 * It is also possible to import a Bean's properties to the model using
 * {@link #importBean(Object)}.
 * <p>
 * <b>Supported property types</b>:
 * <ul>
 * <li>boolean &amp; Boolean</li>
 * <li>int &amp; Integer</li>
 * <li>double &amp; Double</li>
 * <li>String</li>
 * <li>Java Beans with only properties of the forementioned types</li>
 * </ul>
 *
 * @author Vaadin Ltd
 */
public interface TemplateModel extends Serializable {

    /**
     * Import a bean to this template model.
     * <p>
     * The given bean is searched for getter methods and the values that the
     * getters return are set (copied) as model values with the corresponding
     * key.
     * <p>
     * E.g. the <code>firstName</code> property in the bean (has a
     * <code>getFirstName()</code> getter method) will be imported to the
     * template model with the <code>firstName</code> key.
     *
     * @param bean
     *            the bean to import
     * @see TemplateModel supported property types
     */
    default void importBean(Object bean) {
        // NOOP invocation handler passes this method call to
        // TemplateModelBeanUtil
    }

    /**
     * Import a bean properties passing the given filter to this template model.
     * <p>
     * The given filter should decide based on the bean property name whether
     * that property should be imported to the model. For nested bean
     * properties, the <em>dot annotation</em> is used.
     * <p>
     * For example, when the given <code>bean</code> is of type Person, and it
     * has a property <code>Address address</code>, the properties inside the
     * <code>Address</code> are passed to the given filter prefixed with
     * <code>address.</code>. e.g. <code>address.postCode</code>.
     *
     *
     * @param bean
     *            the to import
     * @param propertyNameFilter
     *            the filter to apply to the bean's properties
     * @see #importBean(Object)
     * @see TemplateModel supported property types
     */
    default void importBean(Object bean, Predicate<String> propertyNameFilter) {
        // NOOP invocation handler passes this method call to
        // TemplateModelBeanUtil
    }

}
