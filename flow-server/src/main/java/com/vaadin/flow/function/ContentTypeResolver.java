/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.function;

import java.io.Serializable;
import java.util.function.BiFunction;

import javax.servlet.ServletContext;

import com.vaadin.flow.server.StreamResource;

/**
 * Content type resolver.
 * <p>
 * Allows to get content type for the given {@link StreamResource} instance
 * using the current {@link ServletContext}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface ContentTypeResolver extends
        BiFunction<StreamResource, ServletContext, String>, Serializable {

}
