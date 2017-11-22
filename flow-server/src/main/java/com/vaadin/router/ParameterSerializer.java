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
package com.vaadin.router;

import java.util.List;

/**
 * Parameter serializer interface for customizing parameter serialization.
 */
@FunctionalInterface
public interface ParameterSerializer<T> {

    /**
     * Method for serializing the list of url parameters get the url segments
     * This method can be overridden to support more complex objects as an url
     * parameter. By default this method attempts to cast the parameter list to
     * String and collect the parts to a List.
     * 
     * @param urlParameters
     *            parameters to serialize
     * @return list of serialized parameters
     */
    List<String> serializeUrlParameters(List<T> urlParameters);
}