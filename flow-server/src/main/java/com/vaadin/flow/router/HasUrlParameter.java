/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.io.Serializable;

/**
 * Defines url parameters for navigation targets for use in routing.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <T>
 *            type parameter type
 */
@FunctionalInterface
public interface HasUrlParameter<T> extends Serializable {

    /**
     * Notifies about navigating to the target that implements this interface.
     *
     * @param event
     *            the navigation event that caused the call to this method
     * @param parameter
     *            the resolved url parameter
     */
    void setParameter(BeforeEvent event, T parameter);
}
