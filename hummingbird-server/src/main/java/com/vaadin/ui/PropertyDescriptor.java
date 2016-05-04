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
package com.vaadin.ui;

/**
 * Describes a component property that has its value stored in some form in the
 * component's element, typically an element property or attribute. The
 * descriptor encapsulates details like default value handling and how to return
 * an optional value so that those settings wouldn't have to be spread out among
 * individual setter and getter implementations in the component's API.
 * <p>
 * Use the factory methods in {@link PropertyDescriptors} to create property
 * descriptor instances.
 *
 * @author Vaadin Ltd
 * @param <S>
 *            the type used when setting the property value
 * @param <G>
 *            the type used when getting the property value, this is typically
 *            either <code>S</code> or <code>Optional&lt;S&gt;</code>
 */
public interface PropertyDescriptor<S, G> {

    /**
     * Sets the property value for the given component.
     *
     * @param component
     *            the component for which to set the value, not
     *            <code>null</code>
     * @param value
     *            the property value to set
     */
    void set(Component component, S value);

    /**
     * Gets the property value for the given component.
     *
     * @param component
     *            the component for which to get the value, not
     *            <code>null</code>
     * @return the property value
     */
    G get(Component component);

}
