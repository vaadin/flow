/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.webcomponent;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Specifies the configuration annotations for embedded application.
 * <p>
 * For internal use only.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@interface EmbeddedApplicationAnnotations {

    /**
     * Returns configuration annotations for web exporter classes.
     *
     * @return
     */
    Class<? extends Annotation>[] value();
}
