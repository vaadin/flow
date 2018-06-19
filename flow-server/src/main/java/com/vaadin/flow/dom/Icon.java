package com.vaadin.flow.dom;

import java.util.Objects;

import org.jsoup.nodes.Element;

/**
 * Implementation of icon -element.
 *
 */
public class Icon extends Element {
    public enum Domain {
        HEADER,
        MANIFEST;
    }

    private int size = 0;
    private String baseName = "icons/icon.png";
    private Domain domain = Domain.HEADER;

    public Icon() {
        super("link");
        attr("type", "image/png");
        rel("icon");
    }

    /**
     * Sets the icon size.
     *
     * Icon is considered as square, so width and height are the same.
     *
     * @param size
     * @return
     */
    public Icon size(int size) {
        this.size = size;
        attr("sizes", size + "x" +size);
        setRelativeName();
        return this;
    }

    private void setRelativeName() {
        int split = baseName.lastIndexOf(".");
        attr("href", baseName.substring(0,split) + "-" + sizes() +
                baseName.substring(split));
    }

    public int size() {
        return this.size;
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
     * Overrides current href -attribute.
     *
     * Href is forced as relative, so all [./] -chars are removed from the
     * start of the href.
     *
     * Href is always set when either size or basename is set.
     *
     * @param href
     * @return
     */
    public Icon href(String href) {
        attr("href", href.replaceAll("^[\\./]+", ""));
        return this;
    }

    /**
     *
     * @return
     */
    public String href() {
        return attr("href");
    }

    /**
     * Return href with '/' -prefix.
     *
     * Used in matching, when serving images.
     *
     * @return
     */
    public String relHref() {
        return "/" + href();
    }


    public Icon rel(String rel) {
        attr("rel", rel);
        return this;
    }

    public String rel() {
        return attr("rel");
    }

    public String type() {
        return attr("type");
    }

    public Icon domain(Domain domain) {
        this.domain = domain;
        return this;
    }

    public Domain domain() {
        return this.domain;
    }

    /**
     * Sets basename of icon.
     *
     * The basename is used in naming the icon href, so that if basename is
     * 'foo/bar/my-icon.png' and size would be 96, then href will be set as
     * 'foo/bar/my-icon-96x96.png'
     *
     * @param baseName
     * @return
     */
    public Icon baseName(String baseName) {
        this.baseName = baseName;
        setRelativeName();
        return this;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        Icon icon = (Icon) o;
        return size == icon.size;
    }

    @Override public int hashCode() {

        return Objects.hash(size);
    }


}
