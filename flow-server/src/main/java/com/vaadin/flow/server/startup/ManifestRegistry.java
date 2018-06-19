package com.vaadin.flow.server.startup;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.vaadin.flow.dom.Icon;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Manifest;
import com.vaadin.flow.server.PwaConfiguration;

public class ManifestRegistry implements Serializable {

    private PwaConfiguration pwaConfiguration;
    private ConcurrentMap<Icon, BufferedImage> icons;
    private final String offlineHtml;


    private ManifestRegistry(Manifest manifest, ServletContext servletContext)
            throws IOException {
        this.pwaConfiguration = new PwaConfiguration(manifest, servletContext);
        URL logo = servletContext.getResource(pwaConfiguration.relLogoPath());
        URL offlinePage = servletContext.getResource(pwaConfiguration
                .relOfflinePath());
        icons = new ConcurrentHashMap<>();
        BufferedImage baseImage = getBaseImage(logo);
        offlineHtml = getOfflinePage(pwaConfiguration, offlinePage);

        for (Icon icon : getIcons(manifest.logoPath())) {
            BufferedImage bimage = new BufferedImage(icon.size(), icon.size(),
                    BufferedImage.TYPE_INT_ARGB);
            // Draw the image on to the buffered image
            Graphics2D bGr = bimage.createGraphics();
            bGr.drawImage(baseImage.getScaledInstance(icon.size(), icon.size(),
                    Image.SCALE_SMOOTH), 0, 0, null);
            bGr.dispose();
            icons.putIfAbsent(icon, bimage);
        }
    }

    public static ManifestRegistry getInstance(ServletContext servletContext) {
        assert servletContext != null;

        Object attribute;
        synchronized (servletContext) {
            attribute = servletContext
                    .getAttribute(ManifestRegistry.class.getName());

            if (attribute == null) {
                try {
                    initRegistry(servletContext, null);
                    attribute = servletContext
                            .getAttribute(ManifestRegistry.class.getName());
                } catch (IOException e) {
                    throw new ExceptionInInitializerError(
                            "Error initializing manifest registry: " + e);
                }
            }
        }

        if (attribute instanceof ManifestRegistry) {
            return (ManifestRegistry) attribute;
        } else {
            throw new IllegalStateException(
                    "Unknown servlet context attribute value: " + attribute);
        }
    }

    protected static void initRegistry(ServletContext servletContext,
            Manifest manifest) throws IOException {
        assert servletContext != null;
        synchronized (servletContext) {
            ManifestRegistry registry = new ManifestRegistry(manifest,
                    servletContext);
            servletContext.setAttribute(ManifestRegistry.class.getName(), registry);
        }
    }

    private static String getOfflinePage(PwaConfiguration config, URL resource)
            throws IOException {
        if (resource != null) {
            return getResourceAsString(resource);
        } else {
            List<Icon> iconList = getIcons(config.getLogoPath());
            String iconHead = iconList.stream().filter(icon ->
                    icon.domain().equals(Icon.Domain.HEADER))
                    .map(icon -> icon.html())
                    .collect(Collectors.joining("\n"));
            Icon largest = iconList.stream()
                    .sorted((icon1, icon2)-> icon2.size() - icon1.size())
                    .findFirst().get();

            String defaultPage = getResourceAsString(BootstrapHandler.class
                    .getResource("default-offline-page.html"));
            return defaultPage
                    .replaceAll("%%%PROJECT_NAME%%%",config.getAppName())
                    .replaceAll("%%%BACKGROUND_COLOR%%%",
                            config.getBackgroundColor())
                    .replaceAll("%%%LOGO_PATH%%%", largest.href())
                    .replaceAll("%%%META_ICONS%%%", iconHead);
        }
    }

    private static String getResourceAsString(URL resource) {
        try {
            resource.openStream();
             BufferedReader bf = new BufferedReader(new InputStreamReader(
                     resource.openStream(), StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            bf.lines().forEach(builder::append);
            return builder.toString();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    private static BufferedImage getBaseImage(URL logo)
            throws IOException {
        // This is only the fallback image, it should
        // first scan the the user-image, which is located as in
        // manifest
        InputStream stream = logo != null ?
                logo.openStream() :
                BootstrapHandler.class
                        .getResourceAsStream("default-logo.png");
        return ImageIO.read(stream);
    }

    public String getOfflineHtml() {
        return offlineHtml;
    }

    /**
     * Yeah, no. Ain't threadsafe
     *
     * @param icon
     * @return
     */
    public BufferedImage getImage(Icon icon) {
        return icons.get(icon);
    }

    public boolean hasImage(Icon icon) {
        return icons.containsKey(icon);
    }

    public List<Icon> getHeaderIcons() {
        return getIcons(Icon.Domain.HEADER);
    }

    public List<Icon> getManifestIcons() {
        return getIcons(Icon.Domain.MANIFEST);
    }

    public List<Icon> getIcons() {
        return icons.keySet().stream().collect(Collectors.toList());
    }

    private List<Icon> getIcons(Icon.Domain domain) {
        return icons.keySet().stream().filter(icon ->
                icon.domain().equals(domain)).collect(Collectors.toList());
    }

    public PwaConfiguration getPwaConfiguration() {
        return pwaConfiguration;
    }

    private static List<Icon> getIcons(String baseName) {
        List<Icon> icons = new ArrayList<>();
        icons.add(new Icon().size(144).baseName(baseName)
                .domain(Icon.Domain.MANIFEST));
        icons.add(new Icon().size(192).baseName(baseName)
                .domain(Icon.Domain.MANIFEST));
        icons.add(new Icon().size(512).baseName(baseName)
                .domain(Icon.Domain.MANIFEST));
        icons.add(new Icon().size(16).baseName(baseName));
        icons.add(new Icon().size(32).baseName(baseName));
        icons.add(new Icon().size(96).baseName(baseName));
        icons.add(new Icon().size(180).href("/apple-touch-icon.png")
                .rel("apple-touch-icon"));
        return icons;
    }

}
