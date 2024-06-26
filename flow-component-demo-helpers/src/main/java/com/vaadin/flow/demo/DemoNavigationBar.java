/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.dom.Element;

/**
 * Component to render anchors for specific parts of a component demo. Those
 * anchors acts like "tabs" in the UI.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag("nav")
public class DemoNavigationBar extends HtmlContainer {

    private final Element list = new Element(Tag.UL);
    private final Map<String, Anchor> anchors = new LinkedHashMap<>();
    private Anchor active;

    /**
     * Default constructor. Creates an empty navigation bar.
     */
    public DemoNavigationBar() {
        getElement().appendChild(list);
    }

    /**
     * Adds a menu item inside the navigation bar.
     *
     * @param text
     *            the text shown for the anchor
     * @param href
     *            the href of the anchor
     */
    public void addLink(String text, String href) {
        Element item = new Element(Tag.LI);
        Anchor anchor = new Anchor(href, text);
        item.appendChild(anchor.getElement());
        list.appendChild(item);
        anchors.put(href, anchor);
    }

    /**
     * Sets an specific anchor as active. The previous active anchor, if
     * existent, is made inactive.
     *
     * @param href
     *            the href of the anchor to be set as active, or
     *            <code>null</code> to inactivate all the anchors.
     */
    public void setActive(String href) {
        if (active != null) {
            active.getElement().removeAttribute("active");
        }
        active = anchors.get(href);
        if (active != null) {
            active.getElement().setAttribute("active", true);
        }
    }

}
