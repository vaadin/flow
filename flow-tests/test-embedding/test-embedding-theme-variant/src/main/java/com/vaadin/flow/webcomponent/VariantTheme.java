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
package com.vaadin.flow.webcomponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.theme.AbstractTheme;

public class VariantTheme implements AbstractTheme {

    public static final String DARK = "dark";

    @Override
    public String getBaseUrl() {
        return "src/";
    }

    @Override
    public String getThemeUrl() {
        return "theme/lumo/";
    }

    @Override
    public List<String> getHeaderInlineContents() {
        return Collections
                .singletonList("<custom-style>\n" + "</custom-style>");
    }

    @Override
    public Map<String, String> getHtmlAttributes(String variant) {
        if (variant.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> attributes = new HashMap<>(1);
        switch (variant) {
        case DARK:
            attributes.put("theme", DARK);
            break;
        default:
            LoggerFactory.getLogger(VariantTheme.class.getName()).warn(
                    "Theme variant not recognized: '{}'. Using no variant.",
                    variant);
        }
        return attributes;
    }
}
