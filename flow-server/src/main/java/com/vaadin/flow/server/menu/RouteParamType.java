/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.menu;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.vaadin.flow.router.internal.ParameterInfo;

/**
 * @since 24.5
 */
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
