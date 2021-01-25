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
package com.vaadin.flow.router;

import com.vaadin.flow.internal.Pair;

/**
 * Route parameter containing the name and the value used mainly when
 * constructing a {@link RouteParameters} instance.
 */
public class RouteParam extends Pair<String, String> {

    /**
     * Creates a new route parameter.
     *
     * @param name
     *            the name of the parameter.
     * @param value
     *            the value of the parameter.
     */
    public RouteParam(String name, String value) {
        super(name, value);
    }

    /**
     * Gets the name of the parameter.
     * 
     * @return the name of the parameter.
     */
    public String getName() {
        return getFirst();
    }

    /**
     * Gets the value of the parameter.
     * 
     * @return the value of the parameter.
     */
    public String getValue() {
        return getSecond();
    }

}
