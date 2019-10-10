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
package com.vaadin.flow.server;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.*;
import java.awt.image.BufferedImage;
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
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.server.startup.ApplicationRouteRegistry;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

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
    private static final String APPLE_STARTUP_IMAGE = "apple-touch-startup-image";
    private static final String APPLE_IMAGE_MEDIA = "(device-width: %dpx) and (device-height: %dpx) "
            + "and (-webkit-device-pixel-ratio: %d)";
    public static final String WORKBOX_FOLDER = "VAADIN/static/server/workbox/";
    private static final String WORKBOX_CACHE_FORMAT = "{ url: '%s', revision: '%s' }";

    private String offlineHtml = "";
    private String manifestJson = "";
    private String serviceWorkerJs = "";
    private String installPrompt = "";
    private long offlineHash;
    private List<PwaIcon> icons = new ArrayList<>();
    private final PwaConfiguration pwaConfiguration;

    private PwaRegistry(PWA pwa, ServletContext servletContext)
            throws IOException {
        if (System.getProperty(HEADLESS_PROPERTY) == null) {
            // set headless mode if the property is not explicitly set
            System.setProperty(HEADLESS_PROPERTY, Boolean.TRUE.toString());
        }

        // set basic configuration by given PWA annotation
        // fall back to defaults if unavailable
        pwaConfiguration = new PwaConfiguration(pwa, servletContext);

        // Build pwa elements only if they are enabled
        if (pwaConfiguration.isEnabled()) {
            URL logo = servletContext
                    .getResource(pwaConfiguration.relIconPath());
            URL offlinePage = servletContext
                    .getResource(pwaConfiguration.relOfflinePath());
            // Load base logo from servlet context if available
            // fall back to local image if unavailable
            BufferedImage baseImage = getBaseImage(logo);

            // Pick top-left pixel as fill color if needed for image resizing
            int bgColor = baseImage.getRGB(0, 0);

            // initialize icons
            icons = initializeIcons(baseImage, bgColor);

            // Load offline page as string, from servlet context if
            // available, fall back to default page
            offlineHtml = initializeOfflinePage(pwaConfiguration, offlinePage);
            offlineHash = offlineHtml.hashCode();

            // Initialize manifest.webmanifest
            manifestJson = initializeManifest().toJson();

            // Initialize sw.js
            serviceWorkerJs = initializeServiceWorker(servletContext);

            // Initialize service worker install prompt html/js
            installPrompt = initializeInstallPrompt(pwaConfiguration);
        }
    }

    private List<PwaIcon> initializeIcons(BufferedImage baseImage,
            int bgColor) {
        for (PwaIcon icon : getIconTemplates(pwaConfiguration.getIconPath())) {
            // New image with wanted size
            icon.setImage(drawIconImage(baseImage, bgColor, icon));
            // Store byte array and hashcode of image (GeneratedImage)
            icons.add(icon);
        }
        return icons;
    }

    private BufferedImage drawIconImage(BufferedImage baseImage, int bgColor,
            PwaIcon icon) {
        BufferedImage bimage = new BufferedImage(icon.getWidth(),
                icon.getHeight(), BufferedImage.TYPE_INT_ARGB);
        // Draw the image on to the buffered image
        Graphics2D graphics = bimage.createGraphics();

        // fill bg with fill-color
        graphics.setBackground(new Color(bgColor, true));
        graphics.clearRect(0, 0, icon.getWidth(), icon.getHeight());

        // calculate ratio (bigger ratio) for resize
        float ratio = (float) baseImage.getWidth()
                / (float) icon.getWidth() > (float) baseImage.getHeight()
                        / (float) icon.getHeight()
                                ? (float) baseImage.getWidth()
                                        / (float) icon.getWidth()
                                : (float) baseImage.getHeight()
                                        / (float) icon.getHeight();

        // Forbid upscaling of image
        ratio = ratio > 1.0f ? ratio : 1.0f;

        // calculate sizes with ratio
        int newWidth = Math.round(baseImage.getHeight() / ratio);
        int newHeight = Math.round(baseImage.getWidth() / ratio);

        // draw rescaled img in the center of created image
        graphics.drawImage(
                baseImage.getScaledInstance(newWidth, newHeight,
                        Image.SCALE_SMOOTH),
                (icon.getWidth() - newWidth) / 2,
                (icon.getHeight() - newHeight) / 2, null);
        graphics.dispose();
        return bimage;
    }

    /**
     * Creates manifest.webmanifest json object.
     *
     * @return manifest.webmanifest contents json object
     */
    private JsonObject initializeManifest() {
        JsonObject manifestData = Json.createObject();
        // Add basic properties
        manifestData.put("name", pwaConfiguration.getAppName());
        manifestData.put("short_name", pwaConfiguration.getShortName());
        if (!pwaConfiguration.getDescription().isEmpty()) {
            manifestData.put("description", pwaConfiguration.getDescription());
        }
        manifestData.put("display", pwaConfiguration.getDisplay());
        manifestData.put("background_color",
                pwaConfiguration.getBackgroundColor());
        manifestData.put("theme_color", pwaConfiguration.getThemeColor());
        manifestData.put("start_url", pwaConfiguration.getStartUrl());
        manifestData.put("scope", pwaConfiguration.getRootUrl());

        // Add icons
        JsonArray iconList = Json.createArray();
        int iconIndex = 0;
        for (PwaIcon icon : getManifestIcons()) {
            JsonObject iconData = Json.createObject();
            iconData.put("src", icon.getHref());
            iconData.put("sizes", icon.getSizes());
            iconData.put("type", icon.getType());
            iconList.set(iconIndex++, iconData);
        }
        manifestData.put("icons", iconList);
        return manifestData;
    }

    private String initializeServiceWorker(ServletContext servletContext) {
        StringBuilder stringBuilder = new StringBuilder();

        // List of icons for precache
        List<String> filesToCahe = getIcons().stream()
                .filter(PwaIcon::shouldBeCached).map(PwaIcon::getCacheFormat)
                .collect(Collectors.toList());

        // Add offline page to precache
        filesToCahe.add(offlinePageCache());
        // Add manifest to precache
        filesToCahe.add(manifestCache());

        // Add user defined resources
        for (String resource : pwaConfiguration.getOfflineResources()) {
            filesToCahe.add(String.format(WORKBOX_CACHE_FORMAT,
                    resource.replaceAll("'", ""), servletContext.hashCode()));
        }

        String workBoxAbsolutePath = servletContext.getContextPath() + "/"
                + WORKBOX_FOLDER;
        // Google Workbox import
        stringBuilder.append("importScripts('").append(workBoxAbsolutePath)
                .append("workbox-sw.js").append("');\n\n");

        stringBuilder.append("workbox.setConfig({\n")
                .append("  modulePathPrefix: '").append(workBoxAbsolutePath)
                .append("'\n").append("});\n");

        // Precaching
        stringBuilder.append("workbox.precaching.precacheAndRoute([\n");
        stringBuilder.append(String.join(",\n", filesToCahe));
        stringBuilder.append("\n]);\n");

        // Offline fallback
        stringBuilder
                .append("self.addEventListener('fetch', function(event) {\n")
                .append("  var request = event.request;\n")
                .append("  if (request.mode === 'navigate') {\n")
                .append("    event.respondWith(\n      fetch(request)\n")
                .append("        .catch(function() {\n")
                .append(String.format("          return caches.match('%s');%n",
                        getPwaConfiguration().getOfflinePath()))
                .append("        })\n    );\n  }\n });");

        return stringBuilder.toString();
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
                ApplicationRouteRegistry reg = ApplicationRouteRegistry
                        .getInstance(new VaadinServletContext(servletContext));

                // Initialize PwaRegistry with found PWA settings
                PWA pwa = reg.getPwaConfigurationClass() != null ? reg
                        .getPwaConfigurationClass().getAnnotation(PWA.class)
                        : null;
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
        List<PwaIcon> iconList = getIcons().stream()
                .filter(PwaIcon::shouldBeCached).collect(Collectors.toList());
        // init header inject of icons
        String iconHead = iconList.stream()
                .map(icon -> icon.asElement().toString())
                .collect(Collectors.joining("\n"));
        // init large image
        PwaIcon largest = iconList.stream()
                .min((icon1, icon2) -> icon2.getWidth() - icon1.getWidth())
                .orElse(null);

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
                .replace("%%%LOGO_PATH%%%",
                        largest != null
                                ? pwaConfiguration.getRootUrl()
                                        + largest.getHref()
                                : "")
                .replace("%%%META_ICONS%%%", iconHead);

    }

    private String initializeInstallPrompt(PwaConfiguration pwaConfiguration) {
        PwaIcon largest = getIcons().stream().filter(PwaIcon::shouldBeCached)
                .min((icon1, icon2) -> icon2.getWidth() - icon1.getWidth())
                .orElse(null);
        return BootstrapHandler.readResource("default-pwa-prompt.html")
                .replace("%%%INSTALL%%%", "Install")
                .replace("%%%LOGO_PATH%%%",
                        largest == null ? ""
                                : pwaConfiguration.getRootUrl()
                                        + largest.getHref())
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

    private BufferedImage getBaseImage(URL logo) throws IOException {
        URLConnection logoResource = logo != null ? logo.openConnection()
                : BootstrapHandler.class.getResource("default-logo.png")
                        .openConnection();
        return ImageIO.read(logoResource.getInputStream());
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
     * manifest.webmanifest contents as a String.
     *
     * @return contents of manifest.webmanifest
     */
    public String getManifestJson() {
        return manifestJson;
    }

    /**
     * sw.js (service worker javascript) as String.
     *
     * @return contents of sw.js
     */
    public String getServiceWorkerJs() {
        return serviceWorkerJs;
    }

    /**
     * Google Workbox cache resource String of offline page. example:
     * {@code {url: 'offline.html', revision: '1234567'}}
     *
     * @return Google Workbox cache resource String of offline page
     */
    public String offlinePageCache() {
        return String.format(WORKBOX_CACHE_FORMAT,
                pwaConfiguration.getOfflinePath(), offlineHash);
    }

    private String manifestCache() {
        return String.format(WORKBOX_CACHE_FORMAT,
                pwaConfiguration.getManifestPath(), manifestJson.hashCode());
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
        return getIcons(PwaIcon.Domain.HEADER);
    }

    /**
     * List of {@link PwaIcon}:s that should be added to manifest.webmanifest.
     *
     * @return List of {@link PwaIcon}:s that should be added to
     *         manifest.webmanifest
     */
    public List<PwaIcon> getManifestIcons() {
        return getIcons(PwaIcon.Domain.MANIFEST);
    }

    /**
     * List of all icons managed by {@link PwaRegistry}.
     *
     * @return List of all icons managed by {@link PwaRegistry}
     */
    public List<PwaIcon> getIcons() {
        return new ArrayList<>(icons);
    }

    private List<PwaIcon> getIcons(PwaIcon.Domain domain) {
        return icons.stream().filter(icon -> icon.getDomain().equals(domain))
                .collect(Collectors.toList());
    }

    public PwaConfiguration getPwaConfiguration() {
        return pwaConfiguration;
    }

    private static List<PwaIcon> getIconTemplates(String baseName) {
        List<PwaIcon> icons = new ArrayList<>();
        // Basic manifest icons for android support
        icons.add(
                new PwaIcon(144, 144, baseName, PwaIcon.Domain.MANIFEST, true));
        icons.add(
                new PwaIcon(192, 192, baseName, PwaIcon.Domain.MANIFEST, true));
        icons.add(
                new PwaIcon(512, 512, baseName, PwaIcon.Domain.MANIFEST, true));

        // Basic icons
        icons.add(new PwaIcon(16, 16, baseName, PwaIcon.Domain.HEADER, true,
                "shortcut icon", ""));
        icons.add(new PwaIcon(32, 32, baseName));
        icons.add(new PwaIcon(96, 96, baseName));

        // IOS basic icon
        icons.add(new PwaIcon(180, 180, baseName, PwaIcon.Domain.HEADER, false,
                "apple-touch-icon", ""));

        // IOS device specific splash screens
        // iPhone X (1125px x 2436px)
        icons.add(new PwaIcon(1125, 2436, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 375, 812, 3)));

        // iPhone 8, 7, 6s, 6 (750px x 1334px)
        icons.add(new PwaIcon(750, 1334, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 375, 667, 2)));

        // iPhone 8 Plus, 7 Plus, 6s Plus, 6 Plus (1242px x 2208px)
        icons.add(new PwaIcon(1242, 2208, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 414, 763, 3)));

        // iPhone 5 (640px x 1136px)
        icons.add(new PwaIcon(640, 1136, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 320, 568, 2)));

        return icons;
    }

}
