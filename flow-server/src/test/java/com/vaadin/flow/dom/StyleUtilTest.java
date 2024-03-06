/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class StyleUtilTest {
    private static final Map<String, String> stylepPropertyToAttribute = new java.util.HashMap<>();
    static {
        stylepPropertyToAttribute.put("width", "width");
        stylepPropertyToAttribute.put("borderRadius", "border-radius");
        stylepPropertyToAttribute.put("webkitBorderRadius",
                "-webkit-border-radius");
        stylepPropertyToAttribute.put("mozBorderRadius", "-moz-border-radius");
        stylepPropertyToAttribute.put("msUserSelect", "-ms-user-select");
        stylepPropertyToAttribute.put("oUserSelect", "-o-user-select");
    }

    @Test
    public void attributeToProperty() {
        stylepPropertyToAttribute.entrySet().forEach((entry) -> {
            String property = entry.getKey();
            String attribute = entry.getValue();
            Assert.assertEquals(property,
                    StyleUtil.styleAttributeToProperty(attribute));

        });
    }

    @Test
    public void propertyToAttribute() {
        stylepPropertyToAttribute.entrySet().forEach((entry) -> {
            String property = entry.getKey();
            String attribute = entry.getValue();
            Assert.assertEquals(attribute,
                    StyleUtil.stylePropertyToAttribute(property));

        });
    }

}
