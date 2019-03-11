/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server.webcomponent;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object containing information of a WebComponent property field.
 */
public interface PropertyData2<P> extends Serializable {

    /**
     * Getter for the property name.
     *
     * @return property name
     */
    String getName();

    /**
     * Getter for the property value class type.
     *
     * @return value class type
     */
    Class<P> getType();

    /**
     * Getter for the initial value if given.
     *
     * @return initial value or {@code null} if none given
     */
    P getValue();
}
