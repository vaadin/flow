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
