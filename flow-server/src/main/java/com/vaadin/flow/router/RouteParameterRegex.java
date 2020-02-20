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
 * Predefined regex used with url template parameters.
 */
public class RouteParameterRegex {

    /**
     * Int type regex.
     */
    public static final String INT_REGEX = "-?[0-1]?[0-9]{1,9}";

    /**
     * Long type regex.
     */
    public static final String LONG_REGEX = "-?[0-8]?[0-9]{1,18}";

    /**
     * Bool type regex.
     */
    public static final String BOOL_REGEX = "true|false";

}
