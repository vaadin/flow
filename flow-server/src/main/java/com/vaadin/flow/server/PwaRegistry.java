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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
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

import com.vaadin.flow.server.startup.RouteRegistry;

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
 */
public class PwaRegistry implements Serializable {
    private static final String APPLE_STARTUP_IMAGE = "apple-touch-startup-image";
    private static final String APPLE_IMAGE_MEDIA = "(device-width: %dpx) and (device-height: %dpx) "
            + "and (-webkit-device-pixel-ratio: %d)";

    private final PwaConfiguration pwaConfiguration;
    private final String offlineHtml;
    private final String manifestJson;
    private final String serviceWorkerJs;
    private long offlineHash;
    private List<PwaIcon> icons;

    private PwaRegistry(PWA pwa, ServletContext servletContext)
            throws IOException {
        // set basic configuration by given PWA annotation
        // fall back to defaults if unavailable
        pwaConfiguration = new PwaConfiguration(pwa, servletContext);

        // Build pwa elements only if they are enabled
        if (pwaConfiguration.isEnabled()) {
            URL logo = servletContext
                    .getResource(pwaConfiguration.relLogoPath());
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

            // Initialize manifest.json
            manifestJson = initializeManifest().toJson();

            // Initialize sw.js
            serviceWorkerJs = initializeServiceWorker(servletContext);
        } else {
            offlineHtml = "";
            manifestJson = "";
            serviceWorkerJs = "";
        }
    }

    private List<PwaIcon> initializeIcons(BufferedImage baseImage,
            int bgColor) {
        icons = new ArrayList<>();
        for (PwaIcon icon : getIconTemplates(pwaConfiguration.getLogoPath())) {
            // New image with wanted size
            BufferedImage bimage = new BufferedImage(icon.getWidth(),
                    icon.getHeight(), BufferedImage.TYPE_INT_ARGB);
            // Draw the image on to the buffered image
            Graphics2D bGr = bimage.createGraphics();

            // fill bg with fill-color
            bGr.setBackground(new Color(bgColor, true));
            bGr.clearRect(0, 0, icon.getWidth(), icon.getHeight());

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
            bGr.drawImage(
                    baseImage.getScaledInstance(newWidth, newHeight,
                            Image.SCALE_SMOOTH),
                    (icon.getWidth() - newWidth) / 2,
                    (icon.getHeight() - newHeight) / 2, null);
            bGr.dispose();

            icon.setImage(bimage);
            // Store byte array and hashcode of image (GeneratedImage)
            icons.add(icon);
        }
        return icons;
    }

    /**
     * Creates manifest.json json object.
     *
     * @return manifest.json contents json object
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
        manifestData.put("scope", pwaConfiguration.getStartUrl());

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

        // Add user defined resources
        for (String resource : pwaConfiguration.getOfflineResources()) {
            filesToCahe.add(String.format("{ url: '%s', revision: '%s' }",
                    resource.replaceAll("'", ""), servletContext.hashCode()));
        }

        // Google Workbox import
        stringBuilder.append("importScripts('https://storage.googleapis.com/"
                + "workbox-cdn/releases/3.2.0/workbox-sw.js');\n\n");

        // Precaching
        stringBuilder.append("workbox.precaching.precacheAndRoute([\n");
        stringBuilder.append(
                filesToCahe.stream().collect(Collectors.joining(",\n")));
        stringBuilder.append("\n]);\n");

        // Offline fallback
        stringBuilder
                .append("self.addEventListener('fetch', function(event) {\n"
                        + "  var request = event.request;\n"
                        + "  if (request.mode === 'navigate') {\n"
                        + "    event.respondWith(\n" + "      fetch(request)\n"
                        + "        .catch(function() {\n"
                        + "          return caches.match('"
                        + getPwaConfiguration().getOfflinePath() + "');\n"
                        + "        })\n" + "    );\n" + "  }\n" + "});");

        return stringBuilder.toString();
    }

    protected static PwaRegistry initRegistry(ServletContext servletContext) {
        assert servletContext != null;

        RouteRegistry reg = RouteRegistry.getInstance(servletContext);

        // Initialize PwaRegistry with found PWA settings
        PWA pwa = reg.getPwaConfigurationClass() != null
                ? reg.getPwaConfigurationClass().getAnnotation(PWA.class)
                : null;
        // will fall back to defaults, if no PWA annotation available
        try {
            return new PwaRegistry(pwa, servletContext);
        } catch (IOException ioe) {
            throw new UncheckedIOException(
                    "Failed to initialize the PWA registry", ioe);
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
        return offlinePage.replaceAll("%%%PROJECT_NAME%%%", config.getAppName())
                .replaceAll("%%%BACKGROUND_COLOR%%%",
                        config.getBackgroundColor())
                .replaceAll("%%%LOGO_PATH%%%",
                        largest != null ? largest.getHref() : "")
                .replaceAll("%%%META_ICONS%%%", iconHead);

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
     * manifest.json contents as a String.
     *
     * @return contents of manifest.json
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
        return String.format("{ url: '%s', revision: '%s' }",
                pwaConfiguration.getOfflinePath(), offlineHash);
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
     * List of {@link PwaIcon}:s that should be added to manifest.json.
     *
     * @return List of {@link PwaIcon}:s that should be added to manifest.json
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
