/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.communication.PwaHandler;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
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

    private static final String META_INF_RESOURCES = "/META-INF/resources";
    private static final String HEADLESS_PROPERTY = "java.awt.headless";
    private static final String APPLE_STARTUP_IMAGE = "apple-touch-startup-image";
    private static final String APPLE_IMAGE_MEDIA = "screen and (device-width: %dpx) and (device-height: %dpx)"
            + " and (-webkit-device-pixel-ratio: %d) and (orientation: %s)";
    private static final String ORIENTATION_PORTRAIT = "portrait";
    private static final String ORIENTATION_LANDSCAPE = "landscape";
    private static final String WORKBOX_CACHE_FORMAT = "{ url: '%s', revision: '%s' }";

    private String offlineHtml = "";
    private String manifestJson = "";
    private String runtimeServiceWorkerJs = "";
    private long offlineHash;
    private List<PwaIcon> icons = new ArrayList<>();
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

        boolean useV14Bootstrap = false;
        ApplicationConfiguration applicationConfiguration = (ApplicationConfiguration) servletContext
                .getAttribute(ApplicationConfiguration.class.getName());
        if (applicationConfiguration != null) {
            useV14Bootstrap = applicationConfiguration.useV14Bootstrap();
        }

        // set basic configuration by given PWA annotation
        // fall back to defaults if unavailable
        pwaConfiguration = pwa == null ? new PwaConfiguration(useV14Bootstrap)
                : new PwaConfiguration(pwa, useV14Bootstrap);

        // Build pwa elements only if they are enabled
        if (pwaConfiguration.isEnabled()) {
            URL logo = getResourceUrl(servletContext,
                    pwaConfiguration.relIconPath());

            URL offlinePage = pwaConfiguration.isOfflinePathEnabled()
                    ? getResourceUrl(servletContext,
                            pwaConfiguration.relOfflinePath())
                    : null;

            // Load base logo from servlet context if available
            // fall back to local image if unavailable
            BufferedImage baseImage = getBaseImage(logo);

            if (baseImage == null) {
                LoggerFactory.getLogger(PwaRegistry.class).error(
                        "Image is not found or can't be loaded: " + logo);
            } else {
                // Pick top-left pixel as fill color if needed for image
                // resizing
                int bgColor = baseImage.getRGB(0, 0);

                // initialize icons
                icons = initializeIcons(baseImage, bgColor);
            }

            // Load offline page as string, from servlet context if
            // available, fall back to default page
            offlineHtml = initializeOfflinePage(pwaConfiguration, offlinePage);
            offlineHash = offlineHtml.hashCode();

            // Initialize manifest.webmanifest
            manifestJson = initializeManifest().toJson();

            // Initialize sw-runtime.js
            runtimeServiceWorkerJs = initializeRuntimeServiceWorker(
                    servletContext);
        }
    }

    private URL getResourceUrl(ServletContext context, String path)
            throws MalformedURLException {
        URL resourceUrl = context.getResource(path);
        if (resourceUrl == null) {
            // this is a workaround specific for Spring default static resources
            // location: see #8705
            String cpPath = path.startsWith("/") ? META_INF_RESOURCES + path
                    : META_INF_RESOURCES + "/" + path;
            resourceUrl = PwaRegistry.class.getResource(cpPath);
        }
        return resourceUrl;
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

    private String initializeRuntimeServiceWorker(
            ServletContext servletContext) {
        StringBuilder stringBuilder = new StringBuilder();

        // List of files to precache
        Collection<String> filesToCache = getIcons().stream()
                .filter(PwaIcon::shouldBeCached).map(PwaIcon::getCacheFormat)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // When custom offlinePath is in use, it is also an offline resource to
        // precache
        if (pwaConfiguration.isOfflinePathEnabled()) {
            filesToCache
                    .add(offlinePageCache(pwaConfiguration.getOfflinePath()));
        }
        // Offline stub to be shown within an <iframe> in the app shell
        filesToCache
                .add(offlinePageCache(PwaHandler.DEFAULT_OFFLINE_STUB_PATH));

        // Always cache the index.html (#13987):
        filesToCache.add(offlinePageCache("index.html"));

        // Add manifest to precache
        filesToCache.add(manifestCache());

        // Add user defined resources. Do not serve these via Webpack, as the
        // file system location from which a resource is served depends on
        // the (configurable) web app logic (#8996).
        for (String resource : pwaConfiguration.getOfflineResources()) {
            filesToCache.add(String.format(WORKBOX_CACHE_FORMAT,
                    resource.replaceAll("'", ""), servletContext.hashCode()));
        }

        // Precaching
        stringBuilder.append("self.additionalManifestEntries = [\n");
        stringBuilder.append(String.join(",\n", filesToCache));
        stringBuilder.append("\n];\n");

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
                        largest != null ? largest.getHref() : "")
                .replace("%%%META_ICONS%%%", iconHead);

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
     * sw-runtime.js (service worker JavaScript for precaching runtime generated
     * resources) as a String.
     *
     * @return contents of sw-runtime.js
     */
    public String getRuntimeServiceWorkerJs() {
        return runtimeServiceWorkerJs;
    }

    private String offlinePageCache(String offlinePath) {
        return String.format(WORKBOX_CACHE_FORMAT, offlinePath, offlineHash);
    }

    private String manifestCache() {
        return String.format(WORKBOX_CACHE_FORMAT,
                pwaConfiguration.getManifestPath(), manifestJson.hashCode());
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

    static List<PwaIcon> getIconTemplates(String baseName) {
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
        icons.add(new PwaIcon(32, 32, baseName, PwaIcon.Domain.HEADER, true));
        icons.add(new PwaIcon(96, 96, baseName));

        // IOS basic icon
        icons.add(new PwaIcon(180, 180, baseName, PwaIcon.Domain.HEADER, false,
                "apple-touch-icon", ""));

        // IOS device specific splash screens
        // iPad Pro 12.9 Portrait:
        icons.add(new PwaIcon(2048, 2732, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        1024, 1366, 2, ORIENTATION_PORTRAIT)));
        // iPad Pro 12.9 Landscape:
        icons.add(new PwaIcon(2732, 2048, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        1024, 1366, 2, ORIENTATION_LANDSCAPE)));

        // iPad Pro 11, 10.5 Portrait:
        icons.add(new PwaIcon(1668, 2388, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        834, 1194, 2, ORIENTATION_PORTRAIT)));
        // iPad Pro 11, 10.5 Landscape:
        icons.add(new PwaIcon(2388, 1668, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        834, 1194, 2, ORIENTATION_LANDSCAPE)));

        // iPad Air 10.5 Portrait:
        icons.add(new PwaIcon(1668, 2224, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        834, 1112, 2, ORIENTATION_PORTRAIT)));
        // iPad Air 10.5 Landscape:
        icons.add(new PwaIcon(2224, 1668, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        834, 1112, 2, ORIENTATION_LANDSCAPE)));

        // iPad 10.2 Portrait:
        icons.add(new PwaIcon(1620, 2160, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        768, 1024, 2, ORIENTATION_PORTRAIT)));
        // iPad 10.2 Landscape:
        icons.add(new PwaIcon(2160, 1620, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        768, 1024, 2, ORIENTATION_LANDSCAPE)));

        // iPad Pro 9.7, iPad Air 9.7, iPad 9.7, iPad mini 7.9 portrait
        icons.add(new PwaIcon(1536, 2048, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        768, 1024, 2, ORIENTATION_PORTRAIT)));
        // iPad Pro 9.7, iPad Air 9.7, iPad 9.7, iPad mini 7.9 landscape
        icons.add(new PwaIcon(2048, 1536, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        768, 1024, 2, ORIENTATION_LANDSCAPE)));

        // iPhone 13 Pro Max, iPhone 12 Pro Max portrait
        icons.add(new PwaIcon(1284, 2778, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        428, 926, 3, ORIENTATION_PORTRAIT)));
        // iPhone 13 Pro Max, iPhone 12 Pro Max landscape
        icons.add(new PwaIcon(2778, 1284, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        428, 926, 3, ORIENTATION_LANDSCAPE)));

        // iPhone 13 Pro, iPhone 13, iPhone 12 Pro, iPhone 12 portrait
        icons.add(new PwaIcon(1170, 2532, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        390, 844, 3, ORIENTATION_PORTRAIT)));
        // iPhone 13 Pro, iPhone 13, iPhone 12 Pro, iPhone 12 landscape
        icons.add(new PwaIcon(2532, 1170, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        390, 844, 3, ORIENTATION_LANDSCAPE)));

        // iPhone 13 Mini, iPhone 12 Mini, iPhone 11 Pro, iPhone XS, iPhone X
        // portrait
        icons.add(new PwaIcon(1125, 2436, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        375, 812, 3, ORIENTATION_PORTRAIT)));
        // iPhone 13 Mini, iPhone 12 Mini, iPhone 11 Pro, iPhone XS, iPhone X
        // landscape
        icons.add(new PwaIcon(2436, 1125, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        375, 812, 3, ORIENTATION_LANDSCAPE)));

        // iPhone 11 Pro Max, iPhone XS Max portrait
        icons.add(new PwaIcon(1242, 2688, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        414, 896, 3, ORIENTATION_PORTRAIT)));
        // iPhone 11 Pro Max, iPhone XS Max landscape
        icons.add(new PwaIcon(2688, 1242, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        414, 896, 3, ORIENTATION_LANDSCAPE)));

        // iPhone 11, iPhone XR portrait
        icons.add(new PwaIcon(828, 1792, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA, 414, 896,
                        2, ORIENTATION_PORTRAIT)));
        // iPhone 11, iPhone XR landscape
        icons.add(new PwaIcon(1792, 828, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA, 414, 896,
                        2, ORIENTATION_LANDSCAPE)));

        // iPhone 8 Plus, 7 Plus, 6s Plus, 6 Plus portrait
        icons.add(new PwaIcon(1242, 2208, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        414, 736, 3, ORIENTATION_PORTRAIT)));
        // iPhone 8 Plus, 7 Plus, 6s Plus, 6 Plus landscape
        icons.add(new PwaIcon(2208, 1242, baseName, PwaIcon.Domain.HEADER,
                false, APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA,
                        414, 736, 3, ORIENTATION_LANDSCAPE)));

        // iPhone 8, 7, 6s, 6, SE 4.7 portrait
        icons.add(new PwaIcon(750, 1334, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA, 375, 667,
                        2, ORIENTATION_PORTRAIT)));
        // iPhone 8, 7, 6s, 6, SE 4.7 landscape
        icons.add(new PwaIcon(1334, 750, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA, 375, 667,
                        2, ORIENTATION_LANDSCAPE)));

        // iPhone 5, SE 4, iPod touch 5th Gen and later portrait
        icons.add(new PwaIcon(640, 1136, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA, 320, 568,
                        2, ORIENTATION_PORTRAIT)));
        // iPhone 5, SE 4, iPod touch 5th Gen and later landscape
        icons.add(new PwaIcon(1136, 640, baseName, PwaIcon.Domain.HEADER, false,
                APPLE_STARTUP_IMAGE, String.format(APPLE_IMAGE_MEDIA, 320, 568,
                        2, ORIENTATION_LANDSCAPE)));

        return icons;
    }

}
