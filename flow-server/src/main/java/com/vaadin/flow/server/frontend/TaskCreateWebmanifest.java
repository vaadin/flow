package com.vaadin.flow.server.frontend;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.BootstrapHandler;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.PwaIcon;
import com.vaadin.flow.server.PwaRegistry;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class TaskCreateWebmanifest implements FallibleCommand {

    private static final String APPLE_STARTUP_IMAGE = "apple-touch-startup-image";
    private static final String APPLE_IMAGE_MEDIA = "(device-width: %dpx) and (device-height: %dpx) "
            + "and (-webkit-device-pixel-ratio: %d)";

    private List<PwaIcon> icons = new ArrayList<>();
    private File[] staticFileLocations;
    private PwaConfiguration pwaConfiguration;
    private File staticFileOutputFolder;

    TaskCreateWebmanifest(FrontendDependenciesScanner frontendDependencies,
            File[] staticFileLocations, File staticFileOutputFolder) {
        this.staticFileLocations = staticFileLocations;
        this.staticFileOutputFolder = staticFileOutputFolder;
        this.pwaConfiguration = frontendDependencies.getPwaConfiguration();
    }

    @Override
    public void execute() {
        try {
            createWebmanifest();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createWebmanifest() throws IOException {
        getLogger().warn("createWebManifest @PWA: " + pwaConfiguration);
        createIconVariants();
        writeManifest(new File(staticFileOutputFolder,
                pwaConfiguration.getManifestPath()));
    }

    private void createIconVariants() throws IOException {
        URL logo = findInStaticFiles(pwaConfiguration.getIconPath());
        BufferedImage baseImage = getBaseImage(logo);

        if (baseImage == null) {
            getLogger().error("Image is not found or can't be loaded: "
                    + pwaConfiguration.getIconPath());
        } else {
            // Pick top-left pixel as fill color if needed for image resizing
            int bgColor = baseImage.getRGB(0, 0);

            // initialize icons
            icons = initializeIcons(baseImage, bgColor);
        }
    }

    private URL findInStaticFiles(String path) throws MalformedURLException {
        if (path == null) {
            return null;
        }
        for (File staticFileLocation : staticFileLocations) {
            File logoFile = new File(staticFileLocation, path);
            if (logoFile.exists()) {
                return logoFile.toURI().toURL();
            }
        }
        return null;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    private BufferedImage getBaseImage(URL logo) throws IOException {
        URLConnection logoResource = logo != null ? logo.openConnection()
                : BootstrapHandler.class.getResource("default-logo.png")
                        .openConnection();
        return ImageIO.read(logoResource.getInputStream());
    }

    private List<PwaIcon> initializeIcons(BufferedImage baseImage,
            int bgColor) {

        for (PwaIcon icon : getIconTemplates(pwaConfiguration.getIconPath())) {
            // New image with wanted size
            BufferedImage image = drawIconImage(baseImage, bgColor, icon);
            writeImage(image, icon.getPath());
            icons.add(icon);
        }
        return icons;
    }

    private void writeImage(BufferedImage image, String relativePath) {
        File target = new File(staticFileOutputFolder, relativePath);
        target.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(target)) {
            ImageIO.write(image, "png", out);
        } catch (IOException ioe) {
            throw new UncheckedIOException(
                    "Failed to write icon to " + target.getAbsolutePath(), ioe);
        }

    }

    private static List<PwaIcon> getIconTemplates(String baseName) {
        List<PwaIcon> icons = new ArrayList<>();
        // Basic manifest icons for android support
        icons.add(new PwaIcon(144, 144, baseName, PwaIcon.Domain.MANIFEST));
        icons.add(new PwaIcon(192, 192, baseName, PwaIcon.Domain.MANIFEST));
        icons.add(new PwaIcon(512, 512, baseName, PwaIcon.Domain.MANIFEST));

        // Basic icons
        icons.add(new PwaIcon(16, 16, baseName, PwaIcon.Domain.HEADER,
                "shortcut icon", ""));
        icons.add(new PwaIcon(32, 32, baseName));
        icons.add(new PwaIcon(96, 96, baseName));

        // IOS basic icon
        icons.add(new PwaIcon(180, 180, baseName, PwaIcon.Domain.HEADER,
                "apple-touch-icon", ""));

        // IOS device specific splash screens
        // iPhone X (1125px x 2436px)
        icons.add(new PwaIcon(1125, 2436, baseName, PwaIcon.Domain.HEADER,
                APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 375, 812, 3)));

        // iPhone 8, 7, 6s, 6 (750px x 1334px)
        icons.add(new PwaIcon(750, 1334, baseName, PwaIcon.Domain.HEADER,
                APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 375, 667, 2)));

        // iPhone 8 Plus, 7 Plus, 6s Plus, 6 Plus (1242px x 2208px)
        icons.add(new PwaIcon(1242, 2208, baseName, PwaIcon.Domain.HEADER,
                APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 414, 763, 3)));

        // iPhone 5 (640px x 1136px)
        icons.add(new PwaIcon(640, 1136, baseName, PwaIcon.Domain.HEADER,
                APPLE_STARTUP_IMAGE,
                String.format(APPLE_IMAGE_MEDIA, 320, 568, 2)));

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

    private void writeManifest(File outputFile) throws IOException {
        JsonObject manifest = getManifest();
        FileUtils.write(outputFile, manifest.toJson(), StandardCharsets.UTF_8);
    }

    /**
     * Creates manifest.webmanifest json object.
     *
     * @return manifest.webmanifest contents json object
     */
    private JsonObject getManifest() {
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
        manifestData.put("scope", "");

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

    /**
     * List of {@link PwaIcon}:s that should be added to manifest.webmanifest.
     *
     * @return List of {@link PwaIcon}:s that should be added to
     *         manifest.webmanifest
     */
    public List<PwaIcon> getManifestIcons() {
        return getIcons(PwaIcon.Domain.MANIFEST);
    }

    private List<PwaIcon> getIcons(PwaIcon.Domain domain) {
        return icons.stream().filter(icon -> icon.getDomain().equals(domain))
                .collect(Collectors.toList());
    }

    /**
     * List of all icons managed by {@link PwaRegistry}.
     *
     * @return List of all icons managed by {@link PwaRegistry}
     */
    public List<PwaIcon> getIcons() {
        return new ArrayList<>(icons);
    }
}