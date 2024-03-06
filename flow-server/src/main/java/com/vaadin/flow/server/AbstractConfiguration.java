/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;

import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_USE_V14_BOOTSTRAP;

/**
 * Defines a base contract for configuration (e.g. on an application level,
 * servlet level,...).
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface AbstractConfiguration extends Serializable {
    /**
     * Returns whether Vaadin is in production mode.
     *
     * @return true if in production mode, false otherwise.
     */
    boolean isProductionMode();

    /**
     * Get if the dev server should be enabled. True by default
     *
     * @return true if dev server should be used
     */
    default boolean enableDevServer() {
        return getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_ENABLE_DEV_SERVER, true);
    }

    /**
     * Get if the dev server should be reused on each reload. True by default,
     * set it to false in tests so as dev server is not kept as a daemon after
     * the test.
     *
     * @return true if dev server should be reused
     */
    default boolean reuseDevServer() {
        return getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER, true);
    }

    /**
     * Returns whether Vaadin is running in useDeprecatedV14Bootstrapping.
     *
     * @return true if in useDeprecatedV14Bootstrapping, false otherwise.
     */
    default boolean useV14Bootstrap() {
        return getBooleanProperty(SERVLET_PARAMETER_USE_V14_BOOTSTRAP, false);
    }

    /**
     * Gets a configured property as a string.
     *
     * @param name
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     */
    String getStringProperty(String name, String defaultValue);

    /**
     * Gets a configured property as a boolean.
     *
     *
     * @param name
     *            The simple of the property, in some contexts, lookup might be
     *            performed using variations of the provided name.
     * @param defaultValue
     *            the default value that should be used if no value has been
     *            defined
     * @return the property value, or the passed default value if no property
     *         value is found
     *
     */
    boolean getBooleanProperty(String name, boolean defaultValue);

    /**
     * Returns whether pnpm is enabled or not.
     *
     * @return {@code true} if enabled, {@code false} if not
     */
    default boolean isPnpmEnabled() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_ENABLE_PNPM,
                Constants.ENABLE_PNPM_DEFAULT);
    }

    /**
     * Returns whether globally installed pnpm is used or the default one (see
     * {@link com.vaadin.flow.server.frontend.FrontendTools#DEFAULT_PNPM_VERSION}).
     *
     * @return {@code true} if globally installed pnpm is used, {@code false} if
     *         the default one is used.
     */
    default boolean isGlobalPnpm() {
        return getBooleanProperty(InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM,
                Constants.GLOBAL_PNPM_DEFAULT);
    }

    /**
     * Returns whether development time usage statistics collection is enabled
     * or not.
     *
     * Always return false if <code>isProductionMode</code> is {@code true}.
     *
     * @see #isProductionMode()
     * @return {@code true} if enabled, {@code false} if not collected.
     */
    default boolean isUsageStatisticsEnabled() {
        return !isProductionMode() && getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_STATISTICS,
                Constants.DEFAULT_DEVMODE_STATS);
    }

    /**
     * Returns whether cross-site request forgery protection is enabled.
     *
     * @return true if XSRF protection is enabled, false otherwise.
     */
    default boolean isXsrfProtectionEnabled() {
        return !getBooleanProperty(SERVLET_PARAMETER_DISABLE_XSRF_PROTECTION,
                false);
    }

    /**
     * Return the defined build folder for the used build system.
     * <p>
     * Default value is <code>target</code> used by maven and the gradle plugin
     * will set it to <code>build</code>.
     *
     * @return build folder name, default {@code target}
     */
    default String getBuildFolder() {
        return getStringProperty(InitParameters.BUILD_FOLDER, Constants.TARGET);
    }

    /**
     * Gets the folder where resource sources are stored.
     * <p>
     * Only available in development mode.
     *
     * @return the folder where resources are stored, typically
     *         {@code src/main/resources}.
     */
    default File getJavaResourceFolder() {
        return new File(getStringProperty(Constants.JAVA_RESOURCE_FOLDER_TOKEN,
                "src/main/resources"));
    }
}
