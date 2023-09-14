/*
 * Copyright 2000-2023 Vaadin Ltd.
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
     * Pnpm configuration options.
     **/
    private Pnpm pnpm = new Pnpm();

    /**
     * Frontend configuration options.
     **/
    private Frontend frontend = new Frontend();

    /**
     * Custom package blacklist that should be skipped in scanning.
     */
    private List<String> blacklistedPackages = new ArrayList<>();

    /**
     * Custom package whitelist that should be scanned.
     */
    private List<String> whitelistedPackages = new ArrayList<>();

    /**
     * Whether a browser should be launched on startup when in development mode.
     */
    private boolean launchBrowser = false;

    /**
     * URL patterns that should not be handled by the Vaadin servlet when mapped
     * to the context root.
     */
    private List<String> excludeUrls;

    /**
     * Enables class scan caching between reloads when using Spring Boot
     * DevTools.
     */
    private boolean devmodeCaching = true;

    public static class Frontend {
        /**
         * Whether a frontend development server (Vite) is used in development
         * mode or not.
         */
        private boolean hotdeploy = false;

        /*
         * Checks if frontend hotdeploy is enabled.
         *
         * @return true if hotdeploy is enabled
         */
        public boolean isHotdeploy() {
            return hotdeploy;
        }

        /*
         * Enables/disables frontend hotdeploy mode.
         *
         * @param hotdeploy true to enable, false to disable
         */
        public void setHotdeploy(boolean hotdeploy) {
            this.hotdeploy = hotdeploy;
        }
    }

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
     * {@link com.vaadin.flow.spring.security.VaadinWebSecurity} and access to a
     * public view will be denied instead of allowed.
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
     * {@link com.vaadin.flow.spring.security.VaadinWebSecurity} and access to a
     * public view will be denied instead of allowed.
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
     * Returns whether class scan caching between reloads when using Spring Boot
     * DevTools should be enabled.
     * <p>
     *
     * @return if class scan caching should be enabled
     */
    public boolean isDevmodeCaching() {
        return devmodeCaching;
    }

    /**
     * Sets whether class scan caching between reloads when using Spring Boot
     * DevTools should be enabled.
     *
     * @param devmodeCaching
     *            {@code true} to enable class scan caching when in development
     *            mode, {@code false} otherwise
     */
    public void setDevmodeCaching(boolean devmodeCaching) {
        this.devmodeCaching = devmodeCaching;
    }

    /**
     * Get a list of packages that are blacklisted for class scanning.
     *
     * @return package blacklist
     */
    public List<String> getBlacklistedPackages() {
        return Collections.unmodifiableList(blacklistedPackages);
    }

    /**
     * Set list of packages to ignore for class scanning.
     *
     * @param blacklistedPackages
     *            list of packages to ignore
     */
    public void setBlacklistedPackages(List<String> blacklistedPackages) {
        this.blacklistedPackages = new ArrayList<>(blacklistedPackages);
    }

    /**
     * Get a list of packages that are white-listed for class scanning.
     *
     * @return package white-list
     */
    public List<String> getWhitelistedPackages() {
        return Collections.unmodifiableList(whitelistedPackages);
    }

    /**
     * Set list of packages to be scanned. If <code>whitelistedPackages</code>
     * is set then <code>blacklistedPackages</code> is ignored.
     *
     * @param whitelistedPackages
     *            list of packages to be scanned
     */
    public void setWhitelistedPackages(List<String> whitelistedPackages) {
        this.whitelistedPackages = new ArrayList<>(whitelistedPackages);
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

    /**
     * Gets the pnpm specific configuration.
     *
     * @return the pnpm configuration
     */
    public Pnpm getPnpm() {
        return pnpm;
    }

    /**
     * Gets the frontend specific configuration.
     *
     * @return the frontend configuration
     */
    public Frontend getFrontend() {
        return frontend;
    }
}
