/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.theme;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation to avoid applying any default theme to the application.
 *
 * <p>
 * Flow uses the following logic to determine which theme to use for the
 * application:
 * <ul>
 * <li>If a {@link Theme} annotation is found on the AppShellConfigurator, the
 * theme defined by it is used.
 * <li>If a {@link NoTheme} annotation is found on the AppShellConfigurator,
 * theming is disabled.
 * <li>If the <code>com.vaadin.flow.theme.lumo.Lumo</code> class is available in
 * the classpath (which comes from the vaadin-lumo-theme project), then it is
 * used as the default theme.
 * </ul>
 * <p>
 * {@link NoTheme} annotation should be added to the AppShellConfigurator
 * implementation.
 *
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface NoTheme {
}
