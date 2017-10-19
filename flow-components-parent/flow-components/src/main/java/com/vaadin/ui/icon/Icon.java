/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.icon;

import com.vaadin.flow.dom.ElementConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.HtmlImport;

/**
 * Component for displaying an icon from the
 * <a href="https://vaadin.com/icons">Vaadin Icons</a> collection.
 * 
 * @author Vaadin Ltd
 * @see VaadinIcons
 */
@Tag("iron-icon")
@HtmlImport("frontend://bower_components/vaadin-icons/vaadin-icons.html")
public class Icon extends Component implements HasStyle {

    private static final String ICON_ATTRIBUTE_NAME = "icon";
    private static final String ICON_COLLECTION_NAME = "vaadin";

    /**
     * Creates an Icon component that displays a Vaadin logo.
     */
    public Icon() {
        this(VaadinIcons.VAADIN_H);
    }

    /**
     * Creates an Icon component that displays the given icon from
     * {@link VaadinIcons}.
     *
     * @param icon
     *            the icon to display
     */
    public Icon(VaadinIcons icon) {
        this(ICON_COLLECTION_NAME, icon.name().toLowerCase().replace('_', '-'));
    }

    /**
     * Creates an Icon component that displays the given {@code icon} from the
     * given {@code collection}.
     *
     * @param collection
     *            the icon collection
     * @param icon
     *            the icon name
     */
    public Icon(String collection, String icon) {
        // iron-icon's icon-attribute uses the format "collection:name",
        // eg. icon="vaadin:arrow-down"
        getElement().setAttribute(ICON_ATTRIBUTE_NAME, collection + ':' + icon);
    }

    /**
     * Sets the width and the height of the icon.
     * <p>
     * The size should be in a format understood by the browser, e.g. "100px" or
     * "2.5em".
     *
     * @param size
     *            the size to set, may be <code>null</code> to clear the value
     */
    public void setSize(String size) {
        if (size == null) {
            getStyle().remove(ElementConstants.STYLE_WIDTH);
            getStyle().remove(ElementConstants.STYLE_HEIGHT);
        } else {
            getStyle().set(ElementConstants.STYLE_WIDTH, size);
            getStyle().set(ElementConstants.STYLE_HEIGHT, size);
        }
    }

    /**
     * Sets the fill color of the icon.
     * <p>
     * The color should be in a format understood by the browser, e.g. "orange",
     * "#FF9E2C" or "rgb(255, 158, 44)".
     * 
     * @param color
     *            the fill color to set, may be <code>null</code> to clear the
     *            value
     */
    public void setColor(String color) {
        if (color == null) {
            getStyle().remove(ElementConstants.STYLE_COLOR);
        } else {
            getStyle().set(ElementConstants.STYLE_COLOR, color);
        }
    }

    /**
     * Gets the fill color of this icon as a String.
     * 
     * @return the fill color of the icon, or <code>null</code> if the color has
     *         not been set
     */
    public String getColor() {
        return getStyle().get(ElementConstants.STYLE_COLOR);
    }
}
