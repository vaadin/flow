/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;

/**
 * Implementation of icons used in PWA resources.
 *
 * Creates the href automatically based on - baseName (the file name with path,
 * as {@literal icons/icon.png"}) - width (width of icon) - height (height of
 * icon) - (possibly) fileHash (the hashcode of image file)
 *
 * The href will be set as: {@code [basename]-[width]x[height].png{?[filehash]}}
 *
 * The trailing {@literal ?[filehash]} will be added if icon cache is not
 * controlled by service worker: cached = false
 *
 * Icon caching is left to the browser if it's not cached with service worker.
 *
 * @since 1.1
 */
public class PwaIcon implements Serializable {
    /**
     * Place where icon belongs to (header or manifest.webmanifest).
     */
    public enum Domain {
        HEADER, MANIFEST
    }

    private static final int DEFAULT_BASE_IMAGE_SIZE = 512;
    private static final Color DEFAULT_INITIALS_BACKGROUND = new Color(
            0x0d72f8);
    private static final Pattern WORD_SEPARATOR = Pattern
            .compile("[^\\p{L}\\p{N}]+");
    private static final Pattern HEX_COLOR = Pattern
            .compile("#(\\p{XDigit}{3}|\\p{XDigit}{6})");

    private final boolean shouldBeCached;
    private final int width;
    private final int height;
    private long fileHash;
    private String baseName;
    private Domain domain;
    private byte[] data;

    private final Map<String, String> attributes = new HashMap<>();
    private String tag = "link";

    private PwaRegistry registry;

    PwaIcon(int width, int height, String baseName) {
        this(width, height, baseName, Domain.HEADER);
    }

    PwaIcon(int width, int height, String baseName, Domain domain) {
        this(width, height, baseName, domain, false);
    }

    PwaIcon(int width, int height, String baseName, Domain domain,
            boolean shouldBeCached) {
        this(width, height, baseName, domain, shouldBeCached, "icon", "");
    }

    PwaIcon(int width, int height, String baseName, Domain domain,
            boolean shouldBeCached, String rel, String media) {
        this.width = width;
        this.height = height;
        this.baseName = baseName;
        this.domain = domain;
        this.shouldBeCached = shouldBeCached;

        attributes.put("type", "image/png");
        attributes.put("rel", rel);
        attributes.put("sizes", width + "x" + height);
        if (media != null && !media.isEmpty()) {
            attributes.put("media", media);
        }

        setRelativeName();
    }

    protected PwaIcon(PwaIcon icon) {
        this.width = icon.width;
        this.height = icon.height;
        this.baseName = icon.baseName;
        this.domain = icon.domain;
        this.shouldBeCached = icon.shouldBeCached;
        this.attributes.putAll(icon.attributes);
    }

    /**
     * Gets an {@link Element} presentation of the icon.
     *
     * @return an {@link Element} presentation of the icon
     */
    public Element asElement() {
        Element element = new Element(tag);
        attributes.forEach(element::attr);
        return element;
    }

    /**
     * Gets width of an icon.
     *
     * @return width of an icon
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets height of an icon.
     *
     * @return height of an icon
     */
    public int getHeight() {
        return height;
    }

    /**
     * Should the icon be cached by the Service Worker.
     *
     * @return should the icon be cached by the Service Worker.
     */
    public boolean shouldBeCached() {
        return shouldBeCached;
    }

    private void setRelativeName() {
        int split = baseName.lastIndexOf('.');
        String link = baseName.substring(0, split) + "-" + getSizes()
                + baseName.substring(split);
        if (!shouldBeCached) {
            link = link + "?" + fileHash;
        }
        attributes.put("href", link);
    }

    /**
     * Gets the value of the {@literal sizes} attribute.
     *
     * @return value of the {@literal sizes} attribute
     */
    public String getSizes() {
        return attributes.get("sizes");
    }

    /**
     * Gets the value of the {@literal href} attribute.
     *
     * @return value of the {@literal href} attribute
     */
    public String getHref() {
        return attributes.get("href");
    }

    /**
     * Return href with '/' -prefix and removed possible ?[fileHash].
     *
     * Used in matching, when serving images.
     *
     * @return href with '/' -prefix and removed possible ?[fileHash]
     */
    public String getRelHref() {
        String[] split = getHref().split("\\?");
        return "/" + split[0];
    }

    /**
     * Gets the cache-string used in Google Workbox caching.
     *
     * @return "{ url: '[href]', revision: '[fileHash' }"
     */
    public String getCacheFormat() {
        return String.format("{ url: '%s', revision: '%s' }", getHref(),
                fileHash);
    }

    /**
     * Gets the value of the {@literal type} attribute.
     *
     * @return value of the {@literal type} attribute
     */
    public String getType() {
        return attributes.get("type");
    }

    /**
     * Gets the value of the {@literal rel} attribute.
     *
     * @return value of the {@literal rel} attribute
     */
    String getRel() {
        return attributes.get("rel");
    }

    /**
     * Gets the icon {@link Domain}.
     *
     * @return the domain of the icon
     */
    public Domain getDomain() {
        return domain;
    }

    public void setRegistry(PwaRegistry registry) {
        this.registry = registry;
    }

    /**
     * Sets the image presenting the icon.
     *
     * @param image
     *            the image in png format
     */
    public void setImage(BufferedImage image) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", stream);
            stream.flush();
            data = stream.toByteArray();
            fileHash = Arrays.hashCode(data);
            setRelativeName();
        } catch (IOException ioe) {
            throw new UncheckedIOException("Failed to write an image ", ioe);
        }
    }

    void setImage(InputStream image) throws IOException {
        if (image != null) {
            data = image.readAllBytes();
            fileHash = Arrays.hashCode(data);
            setRelativeName();
        }
    }

    /**
     * Gets if the icon can be written on a stream or not.
     *
     * @return {@literal true} if the icon can be written, otherwise
     *         {@literal false}.
     * @see #write(OutputStream)
     */
    boolean isAvailable() {
        return data != null || registry.getBaseImage() != null;
    }

    /**
     * Writes the icon image to output stream.
     *
     * @param outputStream
     *            output stream to write the icon image to
     */
    public void write(OutputStream outputStream) {
        if (data == null) {
            // New image with wanted size
            // Store byte array and hashcode of image (GeneratedImage)
            setImage(drawIconImage(getBaseImage(), this.getWidth(),
                    this.getHeight()));
        }
        try {
            outputStream.write(data);
        } catch (IOException ioe) {
            throw new UncheckedIOException(
                    "Failed to store the icon image into the stream provided",
                    ioe);
        }
    }

    // visible for test
    protected BufferedImage getBaseImage() {
        return registry.getBaseImage();
    }

    /**
     * Draws a resized version of the given base image centered on a new image
     * of the specified dimensions. The top-left pixel of the base image is used
     * as the background fill color. The image is scaled down to fit within the
     * target dimensions while preserving its aspect ratio; upscaling is not
     * performed.
     *
     * @param baseImage
     *            the source image to resize
     * @param targetWidth
     *            the width of the resulting image in pixels
     * @param targetHeight
     *            the height of the resulting image in pixels
     * @return a new {@link BufferedImage} with the resized icon drawn centered
     * @since 25.2
     */
    public static BufferedImage drawIconImage(BufferedImage baseImage,
            int targetWidth, int targetHeight) {
        // Pick top-left pixel as fill color if needed for image
        // resizing
        int bgColor = baseImage.getRGB(0, 0);

        BufferedImage bimage = new BufferedImage(targetWidth, targetHeight,
                BufferedImage.TYPE_INT_ARGB);
        // Draw the image on to the buffered image
        Graphics2D graphics = bimage.createGraphics();

        try {
            // fill bg with fill-color
            graphics.setBackground(new Color(bgColor, true));
            graphics.clearRect(0, 0, targetWidth, targetHeight);

            // calculate ratio (bigger ratio) for resize
            float ratio = (float) baseImage.getWidth()
                    / (float) targetWidth > (float) baseImage.getHeight()
                            / (float) targetHeight
                                    ? (float) baseImage.getWidth()
                                            / (float) targetWidth
                                    : (float) baseImage.getHeight()
                                            / (float) targetHeight;

            // Forbid upscaling of image
            ratio = ratio > 1.0f ? ratio : 1.0f;

            // calculate sizes with ratio
            int newWidth = Math.round(baseImage.getHeight() / ratio);
            int newHeight = Math.round(baseImage.getWidth() / ratio);

            // draw rescaled img in the center of created image
            graphics.drawImage(
                    baseImage.getScaledInstance(newWidth, newHeight,
                            Image.SCALE_SMOOTH),
                    (targetWidth - newWidth) / 2,
                    (targetHeight - newHeight) / 2, null);
        } finally {
            graphics.dispose();
        }
        return bimage;
    }

    /**
     * Draws the base image for the PWA icons of an application that has no icon
     * of its own: the initials of the application name on a square filled with
     * the theme color.
     * <p>
     * The initials are the first letters of the first two words of the name.
     * For a single word, they are its first two capital letters when it has at
     * least two (<code>StarPass</code> gives <code>SP</code>), otherwise its
     * first two letters. The square uses a default blue when the theme color is
     * the {@link PwaConfiguration#DEFAULT_THEME_COLOR default} white or not a
     * hex color.
     * <p>
     * Falls back to the bundled Vaadin logo when no initials can be drawn, for
     * example when the name has no letters or digits, or no font that can
     * display them is installed.
     *
     * @param appName
     *            the application name to take the initials from
     * @param themeColor
     *            the theme color of the application, as a CSS hex color
     * @return a new {@link BufferedImage} to generate the icons from
     * @throws IOException
     *             if the bundled logo is needed and can not be read
     * @since 25.4
     */
    public static BufferedImage drawDefaultBaseImage(String appName,
            String themeColor) throws IOException {
        String initials = getInitials(appName);
        if (!initials.isEmpty()) {
            try {
                BufferedImage image = drawInitialsImage(initials,
                        getInitialsBackground(themeColor));
                if (image != null) {
                    return image;
                }
            } catch (RuntimeException | Error e) { // NOSONAR
                // Text rendering fails with an error rather than an exception
                // when the JVM finds no fonts at all, as in minimal containers
                LoggerFactory.getLogger(PwaIcon.class).debug(
                        "Cannot draw the default PWA icon from the application name",
                        e);
            }
        }
        LoggerFactory.getLogger(PwaIcon.class).warn(
                "Cannot draw the initials of '{}' for the default PWA icon, using the Vaadin logo instead. Add an image at the @PWA icon path to use your own icon.",
                appName);
        try (InputStream logo = BootstrapHandler.class
                .getResourceAsStream("default-logo.png")) {
            return ImageIO.read(logo);
        }
    }

    static String getInitials(String appName) {
        List<String> words = Arrays.stream(WORD_SEPARATOR.split(appName))
                .filter(word -> !word.isEmpty()).toList();
        int[] initials;
        if (words.isEmpty()) {
            return "";
        } else if (words.size() > 1) {
            initials = new int[] { words.get(0).codePointAt(0),
                    words.get(1).codePointAt(0) };
        } else {
            String word = words.get(0);
            int[] capitals = word.codePoints().filter(Character::isUpperCase)
                    .toArray();
            initials = capitals.length > 1 ? capitals
                    : word.codePoints().toArray();
            initials = Arrays.copyOf(initials, Math.min(2, initials.length));
        }
        return new String(initials, 0, initials.length)
                .toUpperCase(Locale.ROOT);
    }

    static Color getInitialsBackground(String themeColor) {
        String hex = themeColor.trim();
        if (!HEX_COLOR.matcher(hex).matches()
                || hex.equalsIgnoreCase(PwaConfiguration.DEFAULT_THEME_COLOR)
                || hex.equalsIgnoreCase("#fff")) {
            return DEFAULT_INITIALS_BACKGROUND;
        }
        if (hex.length() == 4) {
            hex = "#" + hex.charAt(1) + hex.charAt(1) + hex.charAt(2)
                    + hex.charAt(2) + hex.charAt(3) + hex.charAt(3);
        }
        return Color.decode(hex);
    }

    private static BufferedImage drawInitialsImage(String initials,
            Color background) {
        int size = DEFAULT_BASE_IMAGE_SIZE;
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, size);
        if (font.canDisplayUpTo(initials) != -1) {
            return null;
        }
        BufferedImage image = new BufferedImage(size, size,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            // A slightly lighter top gives the flat color some depth
            graphics.setPaint(new GradientPaint(0, 0,
                    blend(background, Color.WHITE, 0.12f), 0, (float) size,
                    background));
            graphics.fillRect(0, 0, size, size);

            // Scale the outline of the text to a fixed share of the icon, so
            // the result does not depend on the metrics of the installed font
            Shape text = font.createGlyphVector(graphics.getFontRenderContext(),
                    initials).getOutline();
            Rectangle2D bounds = text.getBounds2D();
            if (bounds.isEmpty()) {
                return null;
            }
            double scale = Math.min(size * 0.32 / bounds.getHeight(),
                    size * 0.62 / bounds.getWidth());
            AffineTransform transform = AffineTransform
                    .getTranslateInstance(size / 2.0, size / 2.0);
            transform.scale(scale, scale);
            transform.translate(-bounds.getCenterX(), -bounds.getCenterY());

            graphics.setColor(
                    getLuminance(background) > 0.6 ? new Color(0x1a1a1a)
                            : Color.WHITE);
            graphics.fill(transform.createTransformedShape(text));
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Color blend(Color color, Color other, float amount) {
        return new Color(
                Math.round(color.getRed()
                        + (other.getRed() - color.getRed()) * amount),
                Math.round(color.getGreen()
                        + (other.getGreen() - color.getGreen()) * amount),
                Math.round(color.getBlue()
                        + (other.getBlue() - color.getBlue()) * amount));
    }

    private static double getLuminance(Color color) {
        return (0.2126 * color.getRed() + 0.7152 * color.getGreen()
                + 0.0722 * color.getBlue()) / 255;
    }

}
