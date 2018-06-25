package com.vaadin.flow.server.startup;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Icon;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaConfiguration;

/**
 * Registry for PWA data.
 *
 * Includes:
 * - {@link PwaConfiguration} - basic info
 * - List of {@link Icon}:s - different sizes of icons for header and manifest
 * - Offline page
 */
public class PWARegistry implements Serializable {

    private PwaConfiguration pwaConfiguration;
    private List<Icon> icons;
    private final String offlineHtml;
    private long offlineHash;


    private PWARegistry(PWA pwa, ServletContext servletContext)
            throws IOException {
        // set basic configuration by given PWA annotation
        // fall back to defaults if unavailable
        this.pwaConfiguration = new PwaConfiguration(pwa, servletContext);
        URL logo = servletContext.getResource(pwaConfiguration.relLogoPath());
        URL offlinePage = servletContext.getResource(pwaConfiguration
                .relOfflinePath());
        icons = new ArrayList<>();
        // Load base logo from servletcontext if available
        // fall back to local image if unavailable
        BufferedImage baseImage = getBaseImage(logo);

        // Pick top-left pixel as fill color if needed for image resizing
        int bgColor = baseImage.getRGB(0,0);

        for (Icon icon : getIcons(pwaConfiguration.getLogoPath())) {
            // New image with wanted size
            BufferedImage bimage = new BufferedImage(icon.getWidth(),
                    icon.getHeight(), BufferedImage.TYPE_INT_ARGB);
            // Draw the image on to the buffered image
            Graphics2D bGr = bimage.createGraphics();

            // fill bg with fill-color
            bGr.setBackground(new Color(bgColor, true));
            bGr.clearRect(0,0,icon.getWidth(),icon.getHeight());

            // calculate ratio (bigger ratio) for resize
            float ratio = (float) baseImage.getWidth() / (float) icon.getWidth() >
                    (float) baseImage.getHeight() / (float)  icon.getHeight()
                    ? (float) baseImage.getWidth() / (float) icon.getWidth()
                    : (float) baseImage.getHeight() / (float) icon.getHeight();

            // Forbid upscaling of image
            ratio = ratio > 1.0f ? ratio : 1.0f;

            // calculate sizes with ratio
            int newWidth = Math.round (baseImage.getHeight() / ratio);
            int newHeight = Math.round (baseImage.getWidth() / ratio);

            // draw rescaled img in the center of created image
            bGr.drawImage(baseImage.getScaledInstance(newWidth, newHeight,
                    Image.SCALE_SMOOTH), (icon.getWidth() - newWidth) / 2,
                    (icon.getHeight() - newHeight) / 2, null);
            bGr.dispose();

            icon.setImage(bimage);
            // Store byte array and hashcode of image (GeneratedImage)
            icons.add(icon);
        }
        // Load offline page as string, from servlet context if
        // available, fall back to default page
        offlineHtml = getOfflinePage(pwaConfiguration, offlinePage);
        offlineHash = offlineHtml.hashCode();
    }

    public static PWARegistry initRegistry(ServletContext servletContext)
            throws IOException {
        assert servletContext != null;

        PWA pwa = null;
        RouteRegistry reg = RouteRegistry.getInstance(servletContext);

        // Search for PWA annotation from layouts
        for (RouteData routeData : reg.getRegisteredRoutes()) {
            Class<?> clazz = routeData.getParentLayout() != null &&
                    routeData.getParentLayout().isAnnotationPresent(PWA.class)
                    ? routeData.getParentLayout()
                    : routeData.getNavigationTarget();
            if (clazz.isAnnotationPresent(PWA.class)) {
                pwa = clazz.getAnnotation(PWA.class);
                break;
            }
        }
        // Initialize PWARegistry with found PWA settings
        // will fall back to defaults, if no PWA annotation available
        return new PWARegistry(pwa, servletContext);
    }

    private String getOfflinePage(PwaConfiguration config, URL resource)
            throws IOException {
        // Used only icons which are cached with service worker
        List<Icon> iconList = getIcons().stream()
                .filter(icon -> icon.cached())
                .collect(Collectors.toList());
        // init header inject of icons
        String iconHead = iconList.stream()
                .map(icon -> icon.toString())
                .collect(Collectors.joining("\n"));
        // init large image
        Icon largest = iconList.stream()
                .sorted((icon1, icon2)-> icon2.getWidth() - icon1.getWidth())
                .findFirst().get();

        URLConnection connection;
        if (resource != null) {
            connection = resource.openConnection();
        } else {
            connection = BootstrapHandler.class
                    .getResource("default-offline-page.html")
                    .openConnection();
        }
        // Get offline page from servlet context
        // Fall back to local default file if unavailable
        String offlinePage = getResourceAsString(connection);
        // Replace template variables with values
        return offlinePage
                .replaceAll("%%%PROJECT_NAME%%%",config.getAppName())
                .replaceAll("%%%BACKGROUND_COLOR%%%",
                        config.getBackgroundColor())
                .replaceAll("%%%LOGO_PATH%%%", largest.href())
                .replaceAll("%%%META_ICONS%%%", iconHead);

    }

    private String getResourceAsString(URLConnection connection) {
        try {
             BufferedReader bf = new BufferedReader(new InputStreamReader(
                     connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            bf.lines().forEach(builder::append);
            connection.getInputStream().close();
            return builder.toString();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    private BufferedImage getBaseImage(URL logo)
            throws IOException {
        URLConnection logoResource = logo != null ?
                logo.openConnection() :
                BootstrapHandler.class
                        .getResource("default-logo.png").openConnection();
        return ImageIO.read(logoResource.getInputStream());
    }

    public String getOfflineHtml() {
        return offlineHtml;
    }

    public String offlinePageCache() {
        return String.format("{ url: '%s', revision: '%s' }",
                pwaConfiguration.getOfflinePath(),
                offlineHash);
    }

    public List<Icon> getHeaderIcons() {
        return getIcons(Icon.Domain.HEADER);
    }

    public List<Icon> getManifestIcons() {
        return getIcons(Icon.Domain.MANIFEST);
    }

    public List<Icon> getIcons() {
        return icons.stream().collect(Collectors.toList());
    }

    private List<Icon> getIcons(Icon.Domain domain) {
        return icons.stream().filter(icon ->
                icon.domain().equals(domain)).collect(Collectors.toList());
    }

    public PwaConfiguration getPwaConfiguration() {
        return pwaConfiguration;
    }

    private static List<Icon> getIcons(String baseName) {
        List<Icon> icons = new ArrayList<>();
        // Basic manifest icons for android support
        icons.add(new Icon().size(144, 144)
                .domain(Icon.Domain.MANIFEST).cached(true));
        icons.add(new Icon().size(192, 192)
                .domain(Icon.Domain.MANIFEST).cached(true));
        icons.add(new Icon().size(512, 512)
                .domain(Icon.Domain.MANIFEST).cached(true));
        // Favicons
        icons.add(new Icon().size(16, 16)
                .href("favicon.png")
                .rel("shortcut icon").cached(true));
        icons.add(new Icon().size(32, 32));
        icons.add(new Icon().size(96, 96));

        // IOS basic icon
        icons.add(new Icon().size(180, 180)
                .rel("apple-touch-icon"));

        // IOS device specific splash screens
        // iPhone X (1125px x 2436px)
        icons.add(new Icon().size(1125, 2436)
                .rel("apple-touch-startup-image")
                .media("(device-width: 375px) and (device-height: 812px) "
                        + "and (-webkit-device-pixel-ratio: 3)"));
        // iPhone 8, 7, 6s, 6 (750px x 1334px)
        icons.add(new Icon().size(750, 1334)
                .rel("apple-touch-startup-image")
                .media("(device-width: 375px) and (device-height: 667px) "
                        + "and (-webkit-device-pixel-ratio: 2)"));

        // iPhone 8 Plus, 7 Plus, 6s Plus, 6 Plus (1242px x 2208px)
        icons.add(new Icon().size(1242, 2208)
                .rel("apple-touch-startup-image")
                .media("(device-width: 414px) and (device-height: 736px) "
                        + "and (-webkit-device-pixel-ratio: 3)"));

        // iPhone 5 (640px x 1136px)
        icons.add(new Icon().size(640, 1136)
                .rel("apple-touch-startup-image")
                .media("(device-width: 320px) and (device-height: 568px) "
                        + "and (-webkit-device-pixel-ratio: 2)"));

        for (Icon icon : icons) {
            icon.baseName(baseName);
        }
        return icons;
    }

}
