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

import java.io.Serializable;

/**
 * Immutable data representing one url parameter.
 */
public class RouteParameterData implements Serializable {

    private final String template;

    private final String regex;

    /**
     * Create a parameter data instance.
     * 
     * @param template
     *            the parameter template.
     */
    public RouteParameterData(String template, String regex) {
        this.template = template;
        this.regex = regex;
    }

    /**
     * Gets the parameter template string.
     * 
     * @return the parameter template.
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Gets the regex of the parameter.
     * 
     * @return the regex of the parameter.
     */
    public String getRegex() {
        return regex;
    }
}
