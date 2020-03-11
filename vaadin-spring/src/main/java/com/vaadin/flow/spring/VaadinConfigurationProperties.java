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


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for Vaadin Spring Boot.
 *
 * @author Vaadin Ltd
 * @see <a href=
 * "http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html">http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html</a>
 */
@ConfigurationProperties(prefix = "vaadin")
public class VaadinConfigurationProperties {

    /**
     * Base URL mapping of the Vaadin servlet.
     */
    private String urlMapping = "/*";

    /**
     * Whether asynchronous operations are supported.
     */
    private boolean asyncSupported = true;

    /**
     * Whetcher pnpm support is enabled
     **/
    private boolean pnpmEnabled = false;

    /**
     * Custom package blacklist that should be skipped in scanning.
     */
    private List<String> blacklistedPackages = new ArrayList<>();

    /**
     * Custom package whitelist that should be scanned.
     */
    private List<String> whitelistedPackages = new ArrayList<>();

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
     * @param urlMapping the {@code urlMapping} property value
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
     * @param asyncSupported the {@code asyncSupported} property value
     */
    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    /**
     * Returns if pnpm support is enabled.
     *
     * @return if pnpm is enabled
     */
    public boolean isPnpmEnabled() {
        return pnpmEnabled;
    }

    /**
     * Sets {@code pnpmEnabled} property value.
     *
     * @param pnpmEnabled the {@code pnpmEnabled} property value
     */
    public void setPnpmEnabled(boolean pnpmEnabled) {
        this.pnpmEnabled = pnpmEnabled;
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
     * @param blacklistedPackages list of packages to ignore
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
     * @param whitelistedPackages list of packages to be scanned
     */
    public void setWhitelistedPackages(List<String> whitelistedPackages) {
        this.whitelistedPackages = new ArrayList<>(whitelistedPackages);
    }
}
