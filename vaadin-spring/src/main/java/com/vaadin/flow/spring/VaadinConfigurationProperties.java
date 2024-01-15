/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * Configuration properties for Vaadin Spring Boot.
 *
 * @author Vaadin Ltd
 * @see <a href=
 *      "http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html">http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html</a>
 */
@ConfigurationProperties(prefix = "vaadin")
public class VaadinConfigurationProperties {

    /**
     * Gets the url mapping using the given environment.
     *
     * This is needed only when VaadinConfigurationProperties is not available
     * for injection, e.g. in a condition.
     *
     * @param environment
     *            the application environment
     * @return the url mapping or null if none is defined
     */
    public static String getUrlMapping(Environment environment) {
        return Binder.get(environment)
                .bind("vaadin", VaadinConfigurationProperties.class)
                .map(conf -> conf.getUrlMapping()).orElse(null);
    }

    /**
     * Gets the excluded URLs using the given environment.
     *
     * This is needed only when VaadinConfigurationProperties is not available
     * for injection, e.g. when using Spring without Boot.
     *
     * @param environment
     *            the application environment
     * @return the excluded URLs or null if none is defined
     */
    public static List<String> getExcludedUrls(Environment environment) {
        return Binder.get(environment)
                .bind("vaadin", VaadinConfigurationProperties.class)
                .map(conf -> conf.getExcludeUrls()).orElse(null);
    }

    /**
     * Base URL mapping of the Vaadin servlet.
     */
    private String urlMapping = "/*";

    /**
     * Whether asynchronous operations are supported.
     */
    private boolean asyncSupported = true;

    /**
     * Whether servlet is loaded on startup.
     */
    private boolean loadOnStartup = true;

    /**
     * Whether pnpm support is enabled
     **/
    private Pnpm pnpm = new Pnpm();

    /**
     * List of blocked packages that shouldn't be scanned.
     */
    private List<String> blockedPackages = new ArrayList<>();

    /**
     * List of allowed packages that should be scanned.
     */
    private List<String> allowedPackages = new ArrayList<>();

    /**
     * Whether a browser should be launched on startup when in development mode.
     */
    private boolean launchBrowser = false;

    /**
     * URL patterns that should not be handled by the Vaadin servlet when mapped
     * to the context root.
     */
    private List<String> excludeUrls;

    public static class Pnpm {
        private boolean enable;

        /**
         * Returns if pnpm support is enabled.
         *
         * @return if pnpm is enabled
         */
        public boolean isEnable() {
            return enable;
        }

        /**
         * Enables/disabled pnp support.
         *
         * @param enable
         *            if {@code true} then pnpm support is enabled, otherwise
         *            it's disabled
         *
         */
        public void setEnable(boolean enable) {
            this.enable = enable;
        }

    }

    /**
     * Gets the url mapping for the Vaadin servlet.
     *
     * @return the url mapping
     */
    public String getUrlMapping() {
        return urlMapping;
    }

    /**
     * Sets {@code urlMapping} property value.
     *
     * @param urlMapping
     *            the {@code urlMapping} property value
     */
    public void setUrlMapping(String urlMapping) {
        this.urlMapping = urlMapping;
    }

    /**
     * Returns if asynchronous operations are supported.
     *
     * @return if async is supported
     */
    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    /**
     * Sets {@code asyncSupported} property value.
     *
     * @param asyncSupported
     *            the {@code asyncSupported} property value
     */
    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    /**
     * Returns if servlet is loaded on startup.
     * <p>
     * If the servlet is not loaded on startup then the first request to the
     * server might be incorrectly handled by
     * {@link com.vaadin.flow.spring.security.VaadinWebSecurityConfigurerAdapter}
     * and access to a public view will be denied instead of allowed.
     *
     * @return if servlet is loaded on startup
     */
    public boolean isLoadOnStartup() {
        return loadOnStartup;
    }

    /**
     * Sets whether servlet is loaded on startup.
     * <p>
     * If the servlet is not loaded on startup then the first request to the
     * server might be incorrectly handled by
     * {@link com.vaadin.flow.spring.security.VaadinWebSecurityConfigurerAdapter}
     * and access to a public view will be denied instead of allowed.
     *
     * @param loadOnStartup
     *            {@code true} to load the servlet on startup, {@code false}
     *            otherwise
     */
    public void setLoadOnStartup(boolean loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    /**
     * Returns if a browser should be launched on startup when in development
     * mode.
     * <p>
     *
     * @return if a browser should be launched on startup when in development
     *         mode
     */
    public boolean isLaunchBrowser() {
        return launchBrowser;
    }

    /**
     * Sets whether a browser should be launched on startup when in development
     * mode.
     *
     * @param launchBrowser
     *            {@code true} to launch a browser on startup when in
     *            development mode, {@code false} otherwise
     */
    public void setLaunchBrowser(boolean launchBrowser) {
        this.launchBrowser = launchBrowser;
    }

    /**
     * Returns if pnpm support is enabled.
     *
     * @return if pnpm is enabled
     */
    public boolean isPnpmEnabled() {
        return pnpm.isEnable();
    }

    /**
     * Enables/disabled pnpm support.
     *
     * @param enabled
     *            if {@code true} then pnpm support is enabled, otherwise it's
     *            disabled
     *
     */
    public void setPnpmEnabled(boolean enabled) {
        pnpm.setEnable(enabled);
    }

    /**
     * Get a list of packages that are blocked for class scanning.
     *
     * @return blocked packages
     */
    public List<String> getBlockedPackages() {
        return Collections.unmodifiableList(blockedPackages);
    }

    /**
     * Set list of packages to ignore for class scanning.
     *
     * @param blockedPackages
     *            list of packages to ignore
     */
    public void setBlockedPackages(List<String> blockedPackages) {
        this.blockedPackages = blockedPackages;
    }

    /**
     * Get a list of packages that are blocked for class scanning.
     *
     * @return blocked packages
     * @deprecated use {@link #getBlockedPackages()}
     */
    @Deprecated(forRemoval = true)
    public List<String> getBlacklistedPackages() {
        return Collections.unmodifiableList(blockedPackages);
    }

    /**
     * Set list of packages to ignore for class scanning.
     *
     * @param blockedPackages
     *            list of packages to ignore
     * @deprecated use {@link #setBlockedPackages(List)}
     */
    @Deprecated(forRemoval = true)
    public void setBlacklistedPackages(List<String> blockedPackages) {
        this.blockedPackages = new ArrayList<>(blockedPackages);
    }

    /**
     * Get a list of packages that are allowed for class scanning.
     *
     * @return allowed packages
     */
    public List<String> getAllowedPackages() {
        return allowedPackages;
    }

    /**
     * Set list of packages to be scanned. If <code>allowedPackages</code> is
     * set then <code>blockedPackages</code> is ignored.
     *
     * @param allowedPackages
     *            list of packages to be scanned
     */
    public void setAllowedPackages(List<String> allowedPackages) {
        this.allowedPackages = allowedPackages;
    }

    /**
     * Get a list of packages that are allowed for class scanning.
     *
     * @return allowed packages
     * @deprecated use {@link #getAllowedPackages()}
     */
    @Deprecated(forRemoval = true)
    public List<String> getWhitelistedPackages() {
        return Collections.unmodifiableList(allowedPackages);
    }

    /**
     * Set list of packages to be scanned. If <code>allowedPackages</code> is
     * set then <code>blockedPackages</code> is ignored.
     *
     * @param allowedPackages
     *            list of packages to be scanned
     * @deprecated use {@link #setAllowedPackages(List)}
     */
    @Deprecated(forRemoval = true)
    public void setWhitelistedPackages(List<String> allowedPackages) {
        this.allowedPackages = new ArrayList<>(allowedPackages);
    }

    /**
     * Get a list of URL patterns that are not handled by the Vaadin servlet
     * when it is mapped to the context root.
     *
     * @return a list of url patterns to exclude
     */
    public List<String> getExcludeUrls() {
        return excludeUrls;
    }

    /**
     * Set a list of URL patterns that are not handled by the Vaadin servlet
     * when it is mapped to the context root.
     *
     * @param excludeUrls
     *            a list of url patterns to exclude
     */
    public void setExcludeUrls(List<String> excludeUrls) {
        this.excludeUrls = excludeUrls;
    }
}
