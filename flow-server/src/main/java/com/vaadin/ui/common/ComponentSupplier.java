/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.common;

import java.io.Serializable;
import java.util.function.Supplier;

import com.vaadin.ui.Component;

/**
 * Interface for supplying {@link Component}s. Interfaces that contain fluent
 * APIs should extend this supplier, so then can return the proper types for
 * method chains.
 *
 * @param <C>
 *            the type of the component
 */
public interface ComponentSupplier<C extends Component>
        extends Supplier<C>, Serializable {

    /**
     * Gets this instance as a {@link Component}.
     *
     * @return this instance
     */
    @Override
    default C get() {
        return (C) this;
    }

}
