/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import org.slf4j.LoggerFactory;

/**
 * Registry for PWA data.
 *
 * Includes:
 * <ul>
 * <li>{@link PwaConfiguration} - basic info
 * <li>List of {@link PwaIcon}:s - different sizes of icons for header and
 * manifest
 * <li>Offline page
 * <li>Manifest json
 * <li>Service worker
 * </ul>
 *
 * @since 1.2
 */
public class PwaRegistry implements Serializable {
    private static final String HEADLESS_PROPERTY = "java.awt.headless";

    private String offlineHtml = "";
    private String installPrompt = "";
    private final PwaConfiguration pwaConfiguration;

    /**
     * Creates a new PwaRegistry instance.
     *
     * @param pwa
     *            the pwa annotation
     * @param servletContext
     *            the context
     * @throws IOException
     *             when icon or offline resources are not found.
     */
    public PwaRegistry(PWA pwa, ServletContext servletContext)
            throws IOException {
        if (System.getProperty(HEADLESS_PROPERTY) == null) {
            // set headless mode if the property is not explicitly set
            System.setProperty(HEADLESS_PROPERTY, Boolean.TRUE.toString());
        }

        // set basic configuration by given PWA annotation
        // fall back to defaults if unavailable
        pwaConfiguration = new PwaConfiguration(pwa);

        // Build pwa elements only if they are enabled
        if (pwaConfiguration.isEnabled()) {
            URL offlinePage = servletContext
                    .getResource(pwaConfiguration.relOfflinePath());

            // Load offline page as string, from servlet context if
            // available, fall back to default page
            offlineHtml = initializeOfflinePage(pwaConfiguration, offlinePage);

            // Initialize service worker install prompt html/js
            installPrompt = initializeInstallPrompt(pwaConfiguration);
        }
    }

    /**
     * Gets the pwa registry for the given servlet context. If the servlet
     * context has no pwa registry, a new instance is created and assigned to
     * the context.
     *
     * @param servletContext
     *            the servlet context for which to get a route registry, not
     *            <code>null</code>
     *
     * @return a registry instance for the given servlet context, not
     *         <code>null</code>
     */
    public static PwaRegistry getInstance(ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext
                    .getAttribute(PwaRegistry.class.getName());

            if (attribute == null) {
                VaadinServletContext context = new VaadinServletContext(
                        servletContext);

                // Try first if there is an AppShell for the project
                Class<?> clazz = AppShellRegistry.getInstance(context)
                        .getShell();

                // Otherwise use the class reported by router
                if (clazz == null) {
                    clazz = ApplicationRouteRegistry.getInstance(context)
                            .getPwaConfigurationClass();
                }

                // Initialize PwaRegistry with found PWA settings
                PWA pwa = clazz != null ? clazz.getAnnotation(PWA.class) : null;

                // will fall back to defaults, if no PWA annotation available
                try {
                    attribute = new PwaRegistry(pwa, servletContext);
                    servletContext.setAttribute(PwaRegistry.class.getName(),
                            attribute);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(
                            "Failed to initialize the PWA registry", ioe);
                }
            }
        }

        if (attribute instanceof PwaRegistry) {
            return (PwaRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    private String initializeOfflinePage(PwaConfiguration config, URL resource)
            throws IOException {
        // Use only icons which are cached with service worker
        // List<PwaIcon> iconList =
        // getIcons().stream().filter(PwaIcon::shouldBeCached)
        // .collect(Collectors.toList());
        // init header inject of icons
        // String iconHead = iconList.stream().map(icon ->
        // icon.asElement().toString())
        // .collect(Collectors.joining("\n"));
        // init large image
        // PwaIcon largest = iconList.stream().min((icon1, icon2) ->
        // icon2.getWidth() -
        // icon1.getWidth())
        // .orElse(null);

        URLConnection connection;
        if (resource != null) {
            connection = resource.openConnection();
        } else {
            connection = BootstrapHandler.class
                    .getResource("default-offline-page.html").openConnection();
        }
        // Get offline page from servlet context
        // Fall back to local default file if unavailable
        String offlinePage = getOfflinePageFromContext(connection);
        // Replace template variables with values
        return offlinePage.replace("%%%PROJECT_NAME%%%", config.getAppName())
                .replace("%%%BACKGROUND_COLOR%%%", config.getBackgroundColor())
        // .replace("%%%LOGO_PATH%%%", largest != null ? largest.getHref() : "")
        // .replace("%%%META_ICONS%%%", iconHead)
        ;

    }

    private String initializeInstallPrompt(PwaConfiguration pwaConfiguration) {
        // PwaIcon largest = getIcons().stream().filter(PwaIcon::shouldBeCached)
        // .min((icon1, icon2) -> icon2.getWidth() -
        // icon1.getWidth()).orElse(null);
        return BootstrapHandler.readResource("default-pwa-prompt.html")
                .replace("%%%INSTALL%%%", "Install")
                // .replace("%%%LOGO_PATH%%%", largest == null ? "" :
                // largest.getHref())
                .replace("%%%PROJECT_NAME%%%", pwaConfiguration.getAppName());
    }

    private String getOfflinePageFromContext(URLConnection connection) {
        try (InputStream stream = connection.getInputStream();
                BufferedReader bf = new BufferedReader(new InputStreamReader(
                        stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            bf.lines().forEach(builder::append);
            return builder.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to retrieve offline page from the servlet context",
                    e);
        }
    }

    /**
     * Static offline page as String.
     *
     * @return contents of offline page
     */
    public String getOfflineHtml() {
        return offlineHtml;
    }

    /**
     * Html and js needed for pwa install prompt as a plain string.
     *
     * @return Html and js needed for pwa install prompt
     */
    public String getInstallPrompt() {
        return installPrompt;
    }

    /**
     * List of {@link PwaIcon}:s that should be added to header.
     *
     * @return List of {@link PwaIcon}:s that should be added to header
     */
    public List<PwaIcon> getHeaderIcons() {
        return Collections.emptyList();
        // return getIcons(PwaIcon.Domain.HEADER);
    }

    public PwaConfiguration getPwaConfiguration() {
        return pwaConfiguration;
    }

}
