/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Methods annotated with {@link NotSupported} are mapped to the original
 * webcomponent implementation, but not supported at Java level.
 * <p>
 * Calling methods annotated this way results in no-ops.
 * <p>
 * Subclasses can override the not supported methods and add meaningful
 * implementation to them.
 *
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Documented
public @interface NotSupported {

}
