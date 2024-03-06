/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import com.vaadin.flow.shared.util.SharedUtil;

/**
 * Utility methods for {@link Style}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StyleUtil {

    private static String[] vendorPrefixes = new String[] { "webkit", "moz",
            "ms", "o" };

    private StyleUtil() {
        // Only static helpers
    }

    /**
     * Converts the given attribute style (dash-separated) into a property style
     * (camelCase).
     *
     * @param attributeStyle
     *            the attribute style
     * @return the property style
     */
    public static String styleAttributeToProperty(String attributeStyle) {
        String propertyStyle;
        if (attributeStyle.startsWith("-")) {
            // Vendor prefix -webkit-border-after is webkitBorderAfter
            propertyStyle = attributeStyle.substring(1);
        } else {
            propertyStyle = attributeStyle;
        }

        return SharedUtil.dashSeparatedToCamelCase(propertyStyle);
    }

    /**
     * Converts the given property style (camelCase) into a attribute style
     * (dash-separated).
     *
     * @param propertyStyle
     *            the property style
     * @return the attribute style
     */
    public static String stylePropertyToAttribute(String propertyStyle) {
        String attributeStyle = SharedUtil
                .camelCaseToDashSeparated(propertyStyle);
        int dashIndex = attributeStyle.indexOf("-");
        if (dashIndex != -1) {
            // webkit-border-after -> -webkit-border-after
            String possibleVendorPrefix = propertyStyle.substring(0, dashIndex);
            for (String vendorPrefix : vendorPrefixes) {
                if (vendorPrefix.equals(possibleVendorPrefix)) {
                    return "-" + attributeStyle;
                }
            }
        }

        return attributeStyle;
    }

}
