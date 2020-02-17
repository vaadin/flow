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
public enum RouteParameterFormat {

    /**
     * If specified the original template is provided.
     */
    TEMPLATE,

    /**
     * Whether the format should be the `:`, which is default.
     */
    COLON_FORMAT,

    /**
     * Whether the format should be the `{}`.
     */
    CURLY_BRACKETS_FORMAT,

    /**
     * The name of the parameter.
     */
    NAME,

    /**
     * The simple type of the parameter, i.e. <code>int</code>,
     * <code>long</code>, <code>boolean</code>, <code>string</code> or
     * <code>regex</code>.
     */
    SIMPLE_TYPE,

    /**
     * Whether the primitive types should be capitalized.
     */
    CAPITALIZED_TYPE,

    /**
     * The type of the parameter which is either one of the primitive types or
     * the full regex expression.
     */
    TYPE

}
