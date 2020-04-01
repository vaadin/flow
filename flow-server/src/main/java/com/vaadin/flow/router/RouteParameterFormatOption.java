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

/**
 * Define the route parameters format flags. This is used when retrieving route
 * information, so that the result may contain the parameters definition
 * represented according with the specified flags.
 */
public enum RouteParameterFormatOption {

    /**
     * The name of the parameter.
     */
    NAME,

    /**
     * Original template regex.
     */
    REGEX,

    /**
     * Parameter modifier, i.e. optional or wildcard.
     */
    MODIFIER,

    /**
     * The named template of the parameter, i.e. <code>int</code>,
     * <code>long</code>, <code>bool</code>, <code>string</code>.
     */
    REGEX_NAME,

}
