/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomDeploymentConfiguration {

    @Target(value = ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Conf {
        String name();

        String value();
    }

    CustomDeploymentConfiguration.Conf[] value();
}