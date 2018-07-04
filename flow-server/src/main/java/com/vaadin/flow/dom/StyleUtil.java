/*
 * Copyright 2000-2018 Vaadin Ltd.
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
