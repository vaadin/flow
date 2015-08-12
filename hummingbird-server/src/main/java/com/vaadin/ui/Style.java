/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.ui;

import com.vaadin.shared.util.SharedUtil;

/**
 *
 * @since
 * @author Vaadin Ltd
 */
public class Style {

    private static final String STYLE = "style";

    /**
     * @since
     * @param element
     * @param string
     * @param width
     */
    public static void add(com.vaadin.hummingbird.kernel.Element element,
            String styleAttribute, String styleValue) {
        String currentStyleAttribute = element.getAttribute(STYLE);
        String[] currentStyles;
        if (currentStyleAttribute == null) {
            currentStyles = new String[0];
        } else {
            currentStyles = currentStyleAttribute.split(";");
        }

        boolean updated = false;
        for (int i = 0; i < currentStyles.length; i++) {
            String currentStyle = currentStyles[i];
            String[] keyValue = currentStyle.split(":", 2);
            String key = keyValue[0].trim();

            if (key.equals(styleAttribute)) {
                updated = true;
                currentStyles[i] = key + ":" + styleValue.trim();
                break;
            }
        }

        String newStyles = SharedUtil.join(currentStyles, ";");
        if (!updated) {
            newStyles += ";" + styleAttribute + ":" + styleValue;
        }
        element.setAttribute("style", newStyles);
    }

    /**
     * @since
     * @param element
     * @param string
     */
    public static void remove(com.vaadin.hummingbird.kernel.Element element,
            String styleProperty) {
        // TODO Auto-generated method stub

    }

    /**
     * @since
     * @param element
     * @param string
     * @param string2
     * @param visible
     */
    public static void set(com.vaadin.hummingbird.kernel.Element element,
            String styleProperty, String styleValue, boolean set) {
        if (set) {
            add(element, styleProperty, styleValue);
        } else {
            remove(element, styleProperty);
        }

    }

}
