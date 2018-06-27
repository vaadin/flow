package com.vaadin.flow.dom;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Element;

/**
 * Implementation of icon -element.
 *
 * Creates the href automatically based on
 * - baseName (the file name with path, as "icons/icon.png"
 * - width (width of icon)
 * - height (height of icon)
 * - (possibly) fileHash (the hashcode of image file)
 *
 * The href will be set as:
 * [basename]-[width]x[height].png{?[filehash]}
 *
 * The trailing ?[filehash] will be added if icon cache is not controlled
 * by service worker: cached = false
 *
 * So caching of a icon is left left for browser if it's not cached with
 * service worker.
 *
 */
public class Icon implements Serializable {
    /**
     * Where icon belongs to.
     *
     * In header or manifest.json.
     *
     */
    public enum Domain {
        HEADER,
        MANIFEST;
    }

    private boolean cached = false;
    private int width = 0;
    private int height = 0;
    private long fileHash = 0;
    private String baseName = "icons/icon.png";
    private Domain domain = Domain.HEADER;
    private boolean hrefOverride = false;
    private byte[] data;

    private Map<String, String> attributes = new HashMap<>();
    private String tag = "link";

    public Icon() {
        attr("type", "image/png");
        rel("icon");
    }

    /**
     * Sets size of icon
     *
     * @param width width of icon
     * @param height height of icon
     * @return
     */
    public Icon size(int width, int height) {
        this.width = width;
        this.height = height;

        attr("sizes", width + "x" + height);
        setRelativeName();
        return this;
    }

    /**
     * Gets an {@link Element} presentation of the icon.
     *
     * @return an {@link Element} presentation of the icon
     */
    public Element asElement() {
        Element element = new Element(tag);
        attributes.entrySet().forEach(entry -> {
            element.attr(entry.getKey(), entry.getValue());
        });
        return element;
    }

    private Icon attr(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    private String attr(String key) {
        return attributes.get(key);
    }

    /**
     * Width of icon.
     *
     * @return Width of icon
     */
    public int getWidth() {
        return width;
    }

    /**
     * Height of icon.
     *
     * @return Height of icon
     */
    public int getHeight() {
        return height;
    }

    /**
     * Should the icon be cached viá Service Worker.
     *
     * @return Should the icon be cached viá Service Worker.
     */
    public boolean cached() {
        return cached;
    }

    /**
     * Chained setter for chained.
     *
     * @param cached Should the icon cached viá Service Worker
     * @return
     */
    public Icon cached(boolean cached) {
        this.cached = cached;
        return this;
    }

    /**
     * Sets the href based on icon values.
     *
     */
    private void setRelativeName() {
        if (!hrefOverride) {
            int split = baseName.lastIndexOf(".");
            String link = baseName.substring(0,split) + "-" + sizes() +
                    baseName.substring(split);
            if (!cached) {
                link = link + "?" + fileHash;
            }
            attr("href", link);
        }
    }

    /**
     * Gets string for sizes -attribute.
     *
     *
     * @return a String as [size]x[size]
     */
    public String sizes() {
        return attr("sizes");
    }

    /**
     * Chaining setter of href -attribute.
     *
     * Href is forced as relative, so all [./] -chars are removed from the
     * start of the href.
     *
     * Href is always set when either size or basename is set.
     *
     * @param href href
     * @return self
     */
    public Icon href(String href) {
        hrefOverride = true;
        attr("href", href.replaceAll("^[\\./]+", ""));
        return this;
    }

    /**
     * href attribute
     *
     * @return href attribute
     */
    public String href() {
        return attr("href");
    }

    /**
     * Return href with '/' -prefix and removed possible ?[fileHash]
     *
     * Used in matching, when serving images.
     *
     * @return
     */
    public String relHref() {
        String[] splitted = href().split("\\?");
        return "/" + splitted[0];
    }

    /**'
     * Gets the cache-string used in Google Workbox caching.
     *
     * @return "{ url: '[href]', revision: '[fileHash' }"
     */
    public String cache() {
        return String.format("{ url: '%s', revision: '%s' }", href(),
                fileHash);
    }

    /**
     * Getter for rel attribute.
     *
     * @return rel attribute
     */
    public String rel() {
        return attr("rel");
    }

    /**
     * Chaining setter for rel-attribute.
     *
     * @param rel rel value
     * @return self
     */
    public Icon rel(String rel) {
        attr("rel", rel);
        return this;
    }

    /**
     * rel -attribute
     *
     * @return rel -attribute
     */
    public String type() {
        return attr("type");
    }

    /**
     * Chained setter for domain.
     *
     * @param domain Domain
     * @return self
     */
    public Icon domain(Domain domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Domain of icon.
     *
     * @return Domain of icon
     */
    public Domain domain() {
        return this.domain;
    }

    /**
     * Media attribute.
     *
     * @return Media attribute
     */
    public String media() {
        return attr("media");
    }

    /**
     * Chained setter for media attribute.
     *
     * @param media media
     * @return self
     */
    public Icon media(String media) {
        attr("media", media);
        return this;
    }

    /**
     * Chaining setter of basename.
     *
     * @param baseName image full name with path, like "icon/icon.png"
     * @return self
     */
    public Icon baseName(String baseName) {
        this.baseName = baseName;
        setRelativeName();
        return this;
    }

    /**
     * Sets the image presenting the icon.
     *
     * @param image image in png -format
     * @throws IOException
     */
    public void setImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write( image, "png", baos );
        baos.flush();
        data = baos.toByteArray();
        fileHash = Arrays.hashCode(data);
        setRelativeName();
        baos.close();
    }

    /**
     * Writes the image to output stream.
     *
     * @param outputStream  output stream
     * @throws IOException
     */
    public synchronized void write(OutputStream outputStream)
            throws IOException {
        outputStream.write(data);
    }

}
