/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

/**
 * Define the route parameters format flags. This is used when retrieving route
 * information, so that the result may contain the parameters definition
 * represented according with the specified flags.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
    REGEX_NAME

}
