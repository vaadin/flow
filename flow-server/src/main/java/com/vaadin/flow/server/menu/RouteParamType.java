/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.server.menu;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.vaadin.flow.router.internal.ParameterInfo;

public enum RouteParamType {
    // @formatter:off
    @JsonProperty("req") REQUIRED,
    @JsonProperty("opt") OPTIONAL,
    @JsonProperty("*") WILDCARD;
    // @formatter:on

    public static RouteParamType getType(ParameterInfo parameterInfo) {
        if (parameterInfo.isVarargs())
            return WILDCARD;
        if (parameterInfo.isOptional())
            return OPTIONAL;
        return REQUIRED;
    }
}
