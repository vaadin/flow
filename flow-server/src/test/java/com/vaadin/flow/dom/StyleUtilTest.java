/*
 * Copyright 2000-2025 Vaadin Ltd.
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
